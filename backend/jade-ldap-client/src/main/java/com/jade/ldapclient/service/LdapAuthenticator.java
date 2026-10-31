package com.jade.ldapclient.service;

import com.jade.ldapclient.config.LdapConfig;
import com.jade.security.entity.SysUser;
import com.jade.security.repository.SysUserRepository;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import at.favre.lib.crypto.bcrypt.BCrypt;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.text.MessageFormat;
import java.util.Optional;

/**
 * LDAP 认证源 (客户端): 查 DN → bind 校验 → 可选自动建本地用户
 *
 * 登录挂钩 (AuthService): 本地校验失败且此处启用时调用。
 * 产生的本地用户 auth_source=ldap, 密码存随机串 (本地密码不可用)。
 */
@ApplicationScoped
public class LdapAuthenticator {

    /** sys_user.auth_source 取值: 本地账号 */
    public static final String SOURCE_LOCAL = "local";
    /** sys_user.auth_source 取值: LDAP 账号 */
    public static final String SOURCE_LDAP = "ldap";

    @Inject
    SysUserRepository userRepository;

    @Inject
    EntityManager em;

    // 自注入代理: 保证 @Transactional 在跨方法调用时生效
    @Inject
    Instance<LdapAuthenticator> self;

    public boolean isEnabled() {
        LdapConfig cfg = LdapConfig.singleton();
        return Boolean.TRUE.equals(cfg.enabled)
                && cfg.ldapUrl != null && !cfg.ldapUrl.isBlank()
                && cfg.baseDn != null && !cfg.baseDn.isBlank();
    }

    /**
     * LDAP 认证并按配置同步本地用户; 认证失败返回 empty (不区分"连接失败/密码错误")
     */
    public Optional<SysUser> authenticateAndSync(String username, String password) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        LdapConfig cfg = LdapConfig.singleton();
        String host = hostOf(cfg.ldapUrl);
        try (LDAPConnection conn = new LDAPConnection(host, portOf(cfg.ldapUrl))) {
            if (cfg.bindDn != null && !cfg.bindDn.isBlank()) {
                conn.bind(cfg.bindDn, cfg.bindPassword == null ? "" : cfg.bindPassword);
            }
            String filter = MessageFormat.format(
                    cfg.userSearchFilter == null || cfg.userSearchFilter.isBlank()
                            ? "(uid={0})" : cfg.userSearchFilter.replace("'", "'"),
                    username);
            SearchRequest req = new SearchRequest(cfg.baseDn, SearchScope.SUB, filter, "uid", "cn", "mail");
            SearchResult result = conn.search(req);
            SearchResultEntry entry = result.getSearchEntries().stream().findFirst().orElse(null);
            if (entry == null) {
                return Optional.empty();
            }
            // 用户 DN + 用户密码 再 bind 一次 = 密码校验
            try (LDAPConnection userConn = new LDAPConnection(host, portOf(cfg.ldapUrl), entry.getDN(), password)) {
                // bind 成功即密码正确
            }
            try (InstanceHandle<LdapAuthenticator> handle = Arc.container().instance(LdapAuthenticator.class)) {
                return handle.get().syncLocalUser(username, entry);
            }
        } catch (LDAPException e) {
            // 连接失败/凭据错误/用户不存在统一视为认证未通过, 不向登录侧泄露 LDAP 内部信息
            return Optional.empty();
        }
    }

    /** 连接测试: 返回错误信息, null = 成功 */
    public String testConnection() {
        LdapConfig cfg = LdapConfig.singleton();
        if (cfg.ldapUrl == null || cfg.ldapUrl.isBlank()) {
            return "LDAP URL 未配置";
        }
        String host = hostOf(cfg.ldapUrl);
        try (LDAPConnection conn = new LDAPConnection(host, portOf(cfg.ldapUrl))) {
            if (cfg.bindDn != null && !cfg.bindDn.isBlank()) {
                conn.bind(cfg.bindDn, cfg.bindPassword == null ? "" : cfg.bindPassword);
            }
            if (cfg.baseDn != null && !cfg.baseDn.isBlank()) {
                conn.search(cfg.baseDn, SearchScope.BASE, "(objectClass=*)");
            }
            return null;
        } catch (LDAPException e) {
            return e.getMessage();
        }
    }

    /** bind 成功后同步本地用户 (auto_create_user=true 时) */
    @Transactional
    public Optional<SysUser> syncLocalUser(String username, SearchResultEntry entry) {
        LdapConfig cfg = LdapConfig.singleton();
        SysUser existing = userRepository.findByUsername(username).orElse(null);
        if (existing != null) {
            // 已有本地账号也同步组→角色映射 (只增不减)
            applyGroupRoleMapping(existing.id, entry, cfg);
            return Optional.of(existing);
        }
        if (!Boolean.TRUE.equals(cfg.autoCreateUser)) {
            return Optional.empty();
        }
        SysUser user = new SysUser();
        user.username = username;
        user.password = BCrypt.withDefaults().hashToString(10, randomPassword().toCharArray());
        user.nickname = entry.getAttributeValue("cn") != null
                ? entry.getAttributeValue("cn") : username;
        user.email = entry.getAttributeValue("mail");
        user.status = 1;
        user.authSource = SOURCE_LDAP;
        user.tenantId = null;
        user.persist();
        applyGroupRoleMapping(user.id, entry, cfg);
        return Optional.of(user);
    }

    /**
     * LDAP 组→系统角色映射 (V16): 按用户 DN 查其所属组 (roleOccupant / member),
     * 依 role_mappings JSON 追加系统角色。只增不减, 不影响手工授权;
     * role_mappings 为空时零行为 (与旧版兼容)。
     */
    void applyGroupRoleMapping(Long userId, SearchResultEntry entry, com.jade.ldapclient.config.LdapConfig cfg) {
        String mappingsJson = cfg.roleMappings;
        if (mappingsJson == null || mappingsJson.isBlank()) {
            return;
        }
        try {
            java.util.Map<String, String> groupToRole = new java.util.LinkedHashMap<>();
            com.fasterxml.jackson.databind.JsonNode arr =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(mappingsJson);
            if (arr.isArray()) {
                arr.forEach(n -> groupToRole.put(n.path("group").asText(""), n.path("role").asText("")));
            }
            if (groupToRole.isEmpty()) {
                return;
            }

            // 用户所属组 (兼容 roleOccupant 与标准 member 属性)
            java.util.Set<String> groupCns = new java.util.HashSet<>();
            try (var conn = new com.unboundid.ldap.sdk.LDAPConnection(
                    hostOf(cfg.ldapUrl), portOf(cfg.ldapUrl))) {
                if (cfg.bindDn != null && !cfg.bindDn.isBlank()) {
                    conn.bind(cfg.bindDn, cfg.bindPassword == null ? "" : cfg.bindPassword);
                }
                String filter = com.unboundid.ldap.sdk.Filter.createORFilter(
                        com.unboundid.ldap.sdk.Filter.createEqualityFilter("roleOccupant", entry.getDN()),
                        com.unboundid.ldap.sdk.Filter.createEqualityFilter("member", entry.getDN())
                ).toString();
                SearchResult groups = conn.search(cfg.baseDn, com.unboundid.ldap.sdk.SearchScope.SUB,
                        filter, "cn");
                for (SearchResultEntry g : groups.getSearchEntries()) {
                    String cn = g.getAttributeValue("cn");
                    if (cn != null) {
                        groupCns.add(cn);
                    }
                }
            }

            for (java.util.Map.Entry<String, String> m : groupToRole.entrySet()) {
                if (!groupCns.contains(m.getKey()) || m.getValue().isBlank()) {
                    continue;
                }
                // 原生 SQL: 角色实体在 jade-admin 模块, 此处不引依赖
                int inserted = em.createNativeQuery(
                        "INSERT INTO sys_user_role (user_id, role_id) "
                                + "SELECT :uid, r.id FROM sys_role r WHERE r.role_code = :code "
                                + "AND NOT EXISTS (SELECT 1 FROM sys_user_role ur "
                                + "WHERE ur.user_id = :uid AND ur.role_id = r.id)")
                        .setParameter("uid", userId)
                        .setParameter("code", m.getValue())
                        .executeUpdate();
                if (inserted > 0) {
                    io.quarkus.logging.Log.infof(
                            "ldap role mapped: user=%s group=%s -> role=%s", entry.getDN(), m.getKey(), m.getValue());
                }
            }
        } catch (Exception e) {
            // 映射失败不阻断登录
            io.quarkus.logging.Log.errorf(e, "ldap role mapping failed: user=%s", entry.getDN());
        }
    }

    private String hostOf(String url) {
        String after = url == null ? "" : url.replace("ldap://", "").replace("ldaps://", "");
        return after.contains(":") ? after.substring(0, after.indexOf(':')) : after;
    }

    private int portOf(String url) {
        try {
            String after = url.substring(url.indexOf("://") + 3);
            if (after.contains(":")) {
                return Integer.parseInt(after.substring(after.indexOf(':') + 1));
            }
            return url.startsWith("ldaps") ? 636 : 389;
        } catch (Exception e) {
            return url.startsWith("ldaps") ? 636 : 389;
        }
    }

    private String randomPassword() {
        return java.util.UUID.randomUUID().toString() + "-" + System.nanoTime();
    }
}
