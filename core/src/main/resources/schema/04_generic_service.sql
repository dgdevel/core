CREATE TABLE IF NOT EXISTS functions (
    id IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_functions_name ON functions(name);
CREATE INDEX IF NOT EXISTS idx_functions_url ON functions(url);

CREATE TABLE IF NOT EXISTS menu (
    id IDENTITY PRIMARY KEY,
    function_id BIGINT,
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (function_id) REFERENCES functions(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES menu(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_menu_function_id ON menu(function_id);
CREATE INDEX IF NOT EXISTS idx_menu_parent_id ON menu(parent_id);
