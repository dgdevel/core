CREATE TABLE IF NOT EXISTS roles (
    id IDENTITY PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (code),
    FOREIGN KEY (parent_id) REFERENCES roles(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_roles_code ON roles(code);
CREATE INDEX IF NOT EXISTS idx_roles_parent_id ON roles(parent_id);

CREATE TABLE IF NOT EXISTS authorizations (
    id IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_authorizations_user_id ON authorizations(user_id);
CREATE INDEX IF NOT EXISTS idx_authorizations_role_id ON authorizations(role_id);
CREATE INDEX IF NOT EXISTS idx_authorizations_valid_from ON authorizations(valid_from);
CREATE INDEX IF NOT EXISTS idx_authorizations_valid_until ON authorizations(valid_until);

CREATE TABLE IF NOT EXISTS role_functions (
    id IDENTITY PRIMARY KEY,
    role_id BIGINT NOT NULL,
    function_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (function_id) REFERENCES functions(id) ON DELETE CASCADE,
    UNIQUE (role_id, function_id)
);

CREATE INDEX IF NOT EXISTS idx_role_functions_role_id ON role_functions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_functions_function_id ON role_functions(function_id);
