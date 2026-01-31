CREATE TABLE IF NOT EXISTS audit_log_type (
    id IDENTITY PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_audit_log_type_code ON audit_log_type(code);

CREATE TABLE IF NOT EXISTS audit_log (
    id IDENTITY PRIMARY KEY,
    instant_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    log_type_id BIGINT NOT NULL,
    payload VARCHAR(1000) NOT NULL,
    FOREIGN KEY (log_type_id) REFERENCES audit_log_type(id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_instant_at ON audit_log(instant_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_log_type_id ON audit_log(log_type_id);
