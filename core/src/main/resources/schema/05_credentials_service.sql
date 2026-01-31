CREATE TABLE IF NOT EXISTS credential_type (
    id IDENTITY PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    only_one_per_user_id BOOLEAN DEFAULT FALSE,
    only_one_security_principal BOOLEAN DEFAULT FALSE,
    UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_credential_type_code ON credential_type(code);

CREATE TABLE IF NOT EXISTS credentials (
    id IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    credential_type_id BIGINT NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    security_principal VARCHAR(255) NOT NULL,
    security_credentials VARCHAR(1000),
    FOREIGN KEY (credential_type_id) REFERENCES credential_type(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_credentials_user_id ON credentials(user_id);
CREATE INDEX IF NOT EXISTS idx_credentials_credential_type_id ON credentials(credential_type_id);
CREATE INDEX IF NOT EXISTS idx_credentials_security_principal ON credentials(security_principal);
CREATE INDEX IF NOT EXISTS idx_credentials_validity ON credentials(valid_from, valid_until);
