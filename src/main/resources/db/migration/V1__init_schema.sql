CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_single_admin
    ON users (role)
    WHERE role = 'ADMIN';

CREATE TABLE IF NOT EXISTS otp_config (
    id INT PRIMARY KEY CHECK (id = 1),
    ttl_seconds BIGINT NOT NULL,
    code_length INT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO otp_config (id, ttl_seconds, code_length)
VALUES (1, 300, 6)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS otp_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(150) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    delivery_channel VARCHAR(20) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_otp_codes_user_operation_status
    ON otp_codes (user_id, operation_id, status);

CREATE INDEX IF NOT EXISTS idx_otp_codes_expires_at
    ON otp_codes (expires_at);
