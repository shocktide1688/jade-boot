-- =====================================================
-- V6: 给 sys_project 加 deleted 字段
-- =====================================================
ALTER TABLE sys_project ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
