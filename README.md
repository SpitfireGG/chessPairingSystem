# Chess Pairing System

A small web app for running Swiss-system chess tournaments. Uses the JDK built-in
`HttpServer`, JDBC, and MySQL. No Spring, no ORM, no JavaScript framework.

I built this because I wanted to run local tournaments without dealing with a
heavy platform. It does one thing — Swiss pairings — and stays out of your way.

## What you can do with it

- Register players with an initial rank (used as a tiebreaker)
- Create a tournament (snapshots your current player list)
- Generate Swiss pairings round by round
- Enter results: 1-0, 0.5-0.5, 0-1, or BYE
- View a live leaderboard
- All of the above works from a browser on any device

There's a public view (`/live`) so players can check pairings and standings
without logging in. The organizer side lives behind a session cookie.

## Quick start

You need Java 21+ and MySQL.

Create a database:

```sql
CREATE DATABASE chess_pairing;
```

The app creates its own tables on startup. You don't need to run a schema file
manually.

Set these environment variables (or use the defaults):

```bash
export DB_URL='jdbc:mysql://localhost:3306/chess_pairing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USER='root'
export DB_PASSWORD='root'
```

Run:

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew run
```

Open http://localhost:8080/ and you're in.

Default login is `admin` / `admin123`. You can change those with environment
variables — see the config section below.

## How it works

The app follows a straightforward layered layout:

```
HttpServer  ->  ApiController  ->  Services  ->  Repositories  ->  MySQL
```

There is no dependency injection framework. Everything is wired by hand in
`ChessPairingApplication.main()`. It takes about 60 lines to set up.

The Swiss pairing algorithm lives in `SwissPairingService`:

- Sort players by score (highest first)
- Then by initial rank, then by name
- Walk down the list and pair the first unpaired player with the next
  unpaired player they haven't already played
- If there's an odd number, the leftover gets a BYE

It's not fancy. It doesn't handle accelerated pairings, and it won't win any
awards for algorithmic elegance. But it works for real tournaments.

The frontend is vanilla HTML, CSS, and JavaScript served as static files.
No build step, no bundler, no node_modules. The JS uses `fetch()` against the
API and builds table rows with string concatenation. It's not pretty code but
it loads fast and has no dependencies.

## Configuration

All config comes from environment variables:

| Variable                     | Default                                         | What it does                    |
| ---------------------------- | ----------------------------------------------- | ------------------------------- |
| `PORT`                       | `8080`                                          | HTTP port                       |
| `DB_URL`                     | `jdbc:mysql://localhost:3306/chess_pairing?...` | JDBC connection string          |
| `DB_USER`                    | `root`                                          | MySQL user                      |
| `DB_PASSWORD`                | `root`                                          | MySQL password                  |
| `APP_DEFAULT_ADMIN_USER`     | `admin`                                         | Auto-created organizer username |
| `APP_DEFAULT_ADMIN_PASSWORD` | `admin123`                                      | Auto-created organizer password |

## API

Most endpoints require the session cookie (`CPS_SESSION`) that you get from
`POST /api/auth/login`.

**Auth**

```
POST /api/auth/login    — form: username, password  → sets session cookie
POST /api/auth/logout
GET  /api/admin/session — returns { adminId, username }
```

**Players**

```
GET    /api/admin/players       — list your players
POST   /api/admin/players       — form: name, initialRank
DELETE /api/admin/players/{id}
```

**Tournaments**

```
GET  /api/admin/tournaments              — list your tournaments
POST /api/admin/tournaments              — form: name
GET  /api/admin/tournaments/{id}/matches         ?round=N or "current"
GET  /api/admin/tournaments/{id}/leaderboard
POST /api/admin/tournaments/{id}/pairings/generate
```

**Results**

```
POST /api/admin/matches/{id}/result  — form: result (P1_WIN, DRAW, P2_WIN, BYE)
```

**Public (no auth)**

```
GET /api/public/tournaments
GET /api/public/tournaments/{id}/matches/current
GET /api/public/tournaments/{id}/leaderboard
```

## Notable design choices (and their tradeoffs)

- **No JSON library.** The app builds JSON by hand with string concatenation
  and a `JsonUtil.escape()` method. It works fine for the data shapes here.
  I wouldn't do this for a larger project, but pulling in Jackson for five
  response shapes felt like overkill.

- **Passwords are SHA-256 hashed.** No bcrypt, no Argon2. SHA-256 is fast,
  which isn't ideal for password storage, but this runs on local networks
  and the threat model is minimal. If you deploy this on the open internet,
  swap in a proper KDF.

- **Sessions live in memory.** Restart the server and everyone gets logged
  out. There's no session persistence. For a club tournament that runs for
  a few hours, this hasn't been a problem.

- **No automated tests.** The `src/test/` directory does not exist. I know.
  This started as a prototype and kept working, so I kept adding to it.
  The database is real — there are no in-memory test doubles.

- **The pairing algorithm is simple.** It works well for 8-40 player
  tournaments. It does not support accelerated pairings, and the rematch
  avoidance is best-effort (it won't pair someone against a previous
  opponent if another option exists, but it doesn't do a full search).

## Building

```bash
./gradlew build
```

The build produces a distribution in `build/distributions/` via the
Gradle application plugin. You can also run directly:

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew run
```

## Pages

| Route    | Page                            |
| -------- | ------------------------------- |
| `/`      | Landing page                    |
| `/login` | Organizer login                 |
| `/app`   | Organizer dashboard             |
| `/live`  | Public pairings and leaderboard |

## What this project is not

- It is not a general-purpose tournament management system.
- It does not handle round-robin or knockout formats.
- It does not have user registration — only the admin can log in.
- It does not have an API client or SDK.
- It is not containerized (no Dockerfile).

It is a focused tool that handles one job. If your needs are more complex,
you should probably look at something like ChessManager, Tornelo, or
Swiss-Manager.

## License

The project doesn't declare a license. Assume all rights reserved.
