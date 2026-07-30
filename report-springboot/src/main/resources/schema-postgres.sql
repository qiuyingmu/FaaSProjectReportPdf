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
