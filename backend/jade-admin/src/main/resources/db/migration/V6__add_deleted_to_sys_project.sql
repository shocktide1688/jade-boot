-- =====================================================
-- V6: 给 sys_project 加 deleted 字段
-- =====================================================
-- 现在 admin 自己有 sys_project (V4 建的, 含 deleted 列)
-- 所以这个 migration 啥也不用做, 但保留作版本号
-- 如果未来 sys_project 在别处建, 可以用:
--   ALTER TABLE sys_project ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
-- =====================================================

-- placeholder: 不做任何变更
SELECT 1;
