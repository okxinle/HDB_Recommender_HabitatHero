CREATE TABLE IF NOT EXISTS user_accounts (
  user_id INTEGER PRIMARY KEY AUTOINCREMENT,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  is_active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS users (
  user_id INTEGER PRIMARY KEY AUTOINCREMENT,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL
);

INSERT OR IGNORE INTO user_accounts (user_id, email, password_hash, is_active)
SELECT user_id, email, password_hash, 1
FROM users;

CREATE TABLE IF NOT EXISTS user_saved_results (
  user_id INTEGER PRIMARY KEY,
  results_json TEXT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES user_accounts(user_id)
);