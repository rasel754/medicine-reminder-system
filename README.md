# PillSync - Medicine Reminder System

A premium Java desktop application for tracking medications, scheduling reminders, monitoring inventory levels, and maintaining compliance histories.

## Features

- **Auth System**: Secure registration and login featuring local password hashing.
- **Dynamic Checklist**: Real-time checklist of today's pending medicines with "Take" and "Skip" buttons.
- **Inventory Stock Control**: Interactive stock tracking with Low Stock status warning badges.
- **Background Reminders**: Active background daemon thread that scans schedule timings, generates dual-tone audio notifications, and spawns interactive popup dialog modals.
- **Compliance Calendar**: Visual calendar charting daily medication adherence logs (Green for taken, Yellow/Orange for skipped doses) with detailed HTML hover tooltip metrics.
- **Modular Stylesheet**: Central styling properties loaded dynamically from `style.css`.

---

## Directory Layout

```
medicine-reminder-system/
├── lib/
│   └── sqlite-jdbc.jar      # Portable SQLite Database Driver
├── database/
│   ├── schema.sql           # Database Initialization SQL script
│   └── medicine_reminder.db # Active Local SQLite Database file
├── resources/
│   ├── alert.wav            # Optional custom alarm sound (synthesized fallback available)
│   └── style.css            # Dark themed CSS stylesheet configuration
└── src/
    ├── model/               # User, Medicine, Stock, UsageLog models
    ├── dao/                 # SQLite database query handlers
    ├── service/             # Auth, inventory, and alarm loop logic
    ├── util/                # DB Connection helper and Sound generator
    ├── ui/                  # Dashboard, Calendar, and Login views
    └── Main.java            # Bootstrap application launcher
```

---

## How to Compile & Run

Open a terminal in the root directory: `d:\google-antigravity\java\p-srver\medicine-reminder-system`.

### 1. Compile the Sources
```bash
javac -d bin -cp "lib/*" src/Main.java src/model/*.java src/dao/*.java src/service/*.java src/util/*.java src/ui/*.java
```

### 2. Run the Application
```bash
java -cp "bin;lib/*" Main
```
