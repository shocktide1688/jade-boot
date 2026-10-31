-- =====================================================
-- V16: LDAP 组→系统角色映射 (Jade Platform, jade-ldap-client)
-- sys_ldap_config.role_mappings = JSON 数组 [{"group":"cn","role":"roleCode"}]
-- 为空时行为与旧版完全一致 (不映射)
-- =====================================================

ALTER TABLE sys_ldap_config ADD COLUMN IF NOT EXISTS role_mappings TEXT;
