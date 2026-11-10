CREATE TABLE IF NOT EXISTS user (
    id          INTEGER PRIMARY KEY,
    username    VARCHAR(255) NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL
);

-- Fruits are never deleted (user_settings_fruit references them, and a deleted row would leave
-- a gap in users' ordered lists). Retire one with active = 0; never reuse an id.
CREATE TABLE IF NOT EXISTS fruit (
    id      INTEGER PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE,
    active  INTEGER     NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

-- One settings row per user, created with the user. Every column is NOT NULL.
CREATE TABLE IF NOT EXISTS user_settings (
    id            INTEGER PRIMARY KEY,
    user_id       INTEGER NOT NULL UNIQUE REFERENCES user (id) ON DELETE CASCADE,
    results_size  INTEGER NOT NULL DEFAULT 20,
    font_size     INTEGER NOT NULL DEFAULT 12
);

-- The user's ordered fruit list (SQLite has no array type, so this is a join table).
-- position is the 0-based index in the list; Hibernate maintains it via @OrderColumn.
CREATE TABLE IF NOT EXISTS user_settings_fruit (
    user_settings_id  INTEGER NOT NULL REFERENCES user_settings (id) ON DELETE CASCADE,
    position          INTEGER NOT NULL CHECK (position >= 0),
    fruit_id          INTEGER NOT NULL REFERENCES fruit (id),
    PRIMARY KEY (user_settings_id, position)
);
