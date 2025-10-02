-- Greetings read-side table
CREATE TABLE IF NOT EXISTS greetings_count (
  name VARCHAR(255) PRIMARY KEY,
  greeting_count INTEGER NOT NULL,
  last_greeted_at TIMESTAMP NOT NULL
);