-- V16: LDAP 组→系统角色映射 (MySQL)
ALTER TABLE sys_ldap_config ADD COLUMN role_mappings TEXT;
