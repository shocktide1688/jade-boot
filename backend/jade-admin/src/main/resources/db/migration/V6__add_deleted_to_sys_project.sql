-- =====================================================
-- V6: 给 sys_project 加 deleted 字段
-- =====================================================
-- sys_project 是在 jade-demo 模块的 V1~V3 创建的
-- jade-admin 单跑 CI 时没有 V1~V3, sys_project 不存在
-- 用 DO 块条件判断, 表不存在就跳过 (幂等)
-- =====================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'sys_project'
    ) THEN
        ALTER TABLE sys_project
            ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
    ELSE
        RAISE NOTICE 'V6: sys_project 表不存在, 跳过 (jade-admin 单独跑场景)';
    END IF;
END $$;
