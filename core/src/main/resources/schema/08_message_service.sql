CREATE TABLE IF NOT EXISTS messages (
    id IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    payload VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_messages_topic ON messages(topic);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);
