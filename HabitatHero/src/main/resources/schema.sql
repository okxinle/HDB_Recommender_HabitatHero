/* IGNORE THIS FILE - THIS IS A TEST FILE FOR THE SQL SCHEMA. \
  This SQL script creates the 'users' table for the application.\
  The table includes fields for user ID, email, password hash, role, and creation timestamp.
*/

CREATE TABLE IF NOT EXISTS users (
  user_id INTEGER PRIMARY KEY AUTOINCREMENT,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'USER',
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);