CREATE TABLE IF NOT EXISTS config (
    id IDENTITY PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (namespace, config_key)
);

CREATE INDEX IF NOT EXISTS idx_config_namespace ON config(namespace);

CREATE TABLE IF NOT EXISTS translations (
    id IDENTITY PRIMARY KEY,
    translation_key VARCHAR(255) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    translation VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (translation_key, language_code)
);

CREATE INDEX IF NOT EXISTS idx_translations_key ON translations(translation_key);
CREATE INDEX IF NOT EXISTS idx_translations_language ON translations(language_code);
