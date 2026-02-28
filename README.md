# Chess Pairing System (Pure Java + Gradle + MySQL)

This project is a barebone Java web app for Swiss chess pairing.
It uses:
- JDK `HttpServer` for web/API
- JDBC for persistence
- MySQL as database

## Implemented Modules

- Landing page describing the system and login option
- Organizer login/authorization (session cookie)
- Register & manage players
- Create & manage Swiss tournaments
- Generate Swiss round pairings
- Enter match results (`P1_WIN`, `DRAW`, `P2_WIN`, `BYE`)
- Live leaderboard
- Public player live view (current pairings + leaderboard)

## Database Setup (MySQL)

1. Create database:

```sql
CREATE DATABASE chess_pairing;
```

2. Configure environment variables (optional if using defaults):

```bash
export DB_URL='jdbc:mysql://localhost:3306/chess_pairing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USER='root'
export DB_PASSWORD='root'
export APP_DEFAULT_ADMIN_USER='admin'
export APP_DEFAULT_ADMIN_PASSWORD='admin123'
```

The app auto-creates schema on startup from `src/main/resources/db/schema.sql`.

## Run

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew run
```

Open:
- `http://localhost:8080/` landing page
- `http://localhost:8080/login` organizer login
- `http://localhost:8080/live` public player live view

## Notes

- Tournament participants are snapshotted from current players when tournament is created.
- Generate next round only after current round results are complete.
- Default organizer is auto-created if missing.
