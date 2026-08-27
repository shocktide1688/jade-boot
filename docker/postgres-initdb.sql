-- =====================================================
-- PostgreSQL 初始化脚本 (CI 启动时自动跑)
-- =====================================================
-- 创建 jade-admin 跟 jade-demo 各自独立的 DB
-- 跟本地 docker-compose.yml 起的 jade-postgres 容器 init 一致
-- =====================================================

CREATE DATABASE jade_demo;
CREATE DATABASE jade_admin;
