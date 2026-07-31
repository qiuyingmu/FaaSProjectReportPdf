-- 新表在首次部署时由 Hibernate ddl-auto=update 自动创建，
-- 改为 validate 后，此处作为补充初始化脚本确保表存在。
-- 此脚本在 Hibernate 之前执行，因此先建表再校验。

-- ===== 旧表（首次由 Hibernate update 创建，这里补齐保证全新环境 validate 可启动）=====

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'ADMIN',
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL,
    last_login  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operation_logs (
    id          BIGSERIAL       PRIMARY KEY,
    operator    VARCHAR(50),
    ip_address  VARCHAR(64),
    action      VARCHAR(50)     NOT NULL,
    detail      TEXT,
    result      VARCHAR(20),
    duration_ms BIGINT,
    created_at  TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS schedule_tasks (
    id              BIGSERIAL       PRIMARY KEY,
    task_type       VARCHAR(20)     NOT NULL UNIQUE,
    cron            VARCHAR(100)    NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    display_name    VARCHAR(50),
    description     VARCHAR(255),
    time_range_code VARCHAR(30),
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_keys (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    key_prefix  VARCHAR(100)    NOT NULL,
    key_hash    VARCHAR(255)    NOT NULL,
    created_at  TIMESTAMP       NOT NULL,
    last_used_at TIMESTAMP,
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    total_requests BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS api_key_usage_log (
    id          BIGSERIAL       PRIMARY KEY,
    api_key_id  BIGINT          NOT NULL,
    usage_date  DATE            NOT NULL,
    count       BIGINT          NOT NULL DEFAULT 0,
    UNIQUE (api_key_id, usage_date)
);

-- ===== 可配置化版本：表单数据源配置 =====
CREATE TABLE IF NOT EXISTS form_configs (
    id          BIGSERIAL       PRIMARY KEY,
    config_key  VARCHAR(50)     NOT NULL UNIQUE,
    label       VARCHAR(50)     NOT NULL,
    color       VARCHAR(20),
    form_uuid   VARCHAR(100)    NOT NULL,
    person_field VARCHAR(100)   NOT NULL,
    date_field  VARCHAR(100)    NOT NULL,
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order  INTEGER         NOT NULL DEFAULT 0
);

-- ===== 可配置化版本：宜搭应用配置 =====
CREATE TABLE IF NOT EXISTS app_configs (
    id          BIGSERIAL       PRIMARY KEY,
    config_key  VARCHAR(50)     NOT NULL UNIQUE,
    app_type    VARCHAR(100)    NOT NULL,
    system_token VARCHAR(255)   NOT NULL,
    user_id     VARCHAR(100)    NOT NULL,
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ===== API 调用明细日志 =====
CREATE TABLE IF NOT EXISTS api_access_logs (
    id          BIGSERIAL       PRIMARY KEY,
    key_prefix  VARCHAR(50),
    source      VARCHAR(20),
    ip          VARCHAR(64),
    method      VARCHAR(10),
    path        VARCHAR(255),
    status      INTEGER,
    duration_ms BIGINT,
    created_at  TIMESTAMP       NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_aal_created ON api_access_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_aal_key_created ON api_access_logs (key_prefix, created_at DESC);

-- ===== 操作日志：补充来源 IP 列（旧表升级）=====
ALTER TABLE operation_logs ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_ol_ip_created ON operation_logs (ip_address, created_at DESC);
