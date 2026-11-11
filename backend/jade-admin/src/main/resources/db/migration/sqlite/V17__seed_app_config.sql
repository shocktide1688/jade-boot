-- =====================================================
-- V17: 应用配置种子 + 系统配置菜单 (SQLite 方言)
-- 应用标题/登录欢迎语存 sys_config (is_builtin=1, 走
-- /api/v1/configs/public 公开读取 + /api/v1/configs/app 保存)
-- =====================================================

-- 种子: 应用标题 (管理端左上角 / 登录页品牌区)
INSERT INTO sys_config (config_name, config_key, config_value, config_type, is_builtin, remark, created_at, updated_at)
SELECT '应用标题', 'app.title', 'Jade Platform', 'system', 1, '管理端左上角标题与登录页品牌区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'app.title' AND deleted = FALSE);

-- 种子: 登录欢迎语 (登录框上方文案)
INSERT INTO sys_config (config_name, config_key, config_value, config_type, is_builtin, remark, created_at, updated_at)
SELECT '登录欢迎语', 'login.welcome', '欢迎登录', 'system', 1, '登录框上方的欢迎文案', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'login.welcome' AND deleted = FALSE);

-- 系统管理 -> 系统配置 (菜单 id 61, 底座 1~60 已占用)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
SELECT 61, 1, '系统配置', 'C', 'appconfig', 'system/appconfig/index', 'setting', 'system:appconfig:config', 7, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 61);

-- admin 角色 (role_id=1) 授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 61
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 61);
