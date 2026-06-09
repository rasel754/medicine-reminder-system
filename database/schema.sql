CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    email TEXT
);

CREATE TABLE IF NOT EXISTS medicines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    dosage TEXT,
    frequency TEXT, -- e.g., "Daily", "Weekly", "Monthly"
    time_of_day TEXT, -- e.g., "08:00,14:00,20:00" (comma-separated 24h format HH:MM)
    start_date TEXT, -- YYYY-MM-DD
    end_date TEXT,   -- YYYY-MM-DD
    instructions TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS stocks (
    medicine_id INTEGER PRIMARY KEY,
    current_stock INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER NOT NULL DEFAULT 5,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS usage_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    medicine_id INTEGER NOT NULL,
    taken_date_time TEXT NOT NULL, -- YYYY-MM-DD HH:MM:SS
    status TEXT NOT NULL,          -- TAKEN, SKIPPED, MISSED
    FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);
