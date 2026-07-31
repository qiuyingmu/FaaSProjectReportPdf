-- 新表在首次部署时由 Hibernate ddl-auto=update 自动创建，
-- 改为 validate 后，此处作为补充初始化脚本确保表存在。
-- 此脚本在 Hibernate 之前执行，因此先建表再校验。

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
