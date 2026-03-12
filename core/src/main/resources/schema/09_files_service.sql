CREATE TABLE IF NOT EXISTS files (
    id IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    payload CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_files_name ON files(name);
CREATE INDEX IF NOT EXISTS idx_files_created_at ON files(created_at);
