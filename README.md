# SkillBridge — Student Skill Exchange & Project Collaboration Platform

College project implementing the SRS (Group 16): a platform where students can
build profiles, discover peers by skill, create or join projects, manage tasks,
chat, exchange mentorship, maintain a portfolio and give feedback — with an
admin console for moderation.

**Stack**

| Layer     | Technology                                                        |
|-----------|-------------------------------------------------------------------|
| Frontend  | React 18 + Vite + Tailwind CSS v4                                  |
| Backend   | Spring Boot 3 (Java 21), Spring Security, Spring Data JPA, MySQL  |
| Database  | MySQL Community Server (local)                                    |

## 1. Database setup

The app uses the local MySQL server on **port 3306** — on this machine that is
Oracle MySQL 8.4, installed at `/usr/local/mysql` (data dir
`/usr/local/mysql/data`), with the `root` account password `YOUR_MYSQL_PASSWORD`.

Connection settings live in
`backend/src/main/resources/application.properties` and can be overridden with
the `DB_USERNAME` / `DB_PASSWORD` environment variables.

To reload the schema and demo data from scratch:

```bash
mysql -h127.0.0.1 -P3306 -uroot -p'YOUR_MYSQL_PASSWORD' -e "DROP DATABASE IF EXISTS skillbridge;"
mysql -h127.0.0.1 -P3306 -uroot -p'YOUR_MYSQL_PASSWORD' < database/schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p'YOUR_MYSQL_PASSWORD' < database/seed.sql
```

`schema.sql` creates the `skillbridge` database and its 26 tables (the app uses
`ddl-auto=none`, so the schema must exist); `seed.sql` inserts the demo data.

### Viewing the data in MySQL Workbench

Create a connection with these settings:

| Setting          | Value               |
|------------------|---------------------|
| Connection name  | `SkillBridge (local)` |
| Hostname         | `127.0.0.1`         |
| Port             | `3306`              |
| Username         | `root`              |
| Password         | `YOUR_MYSQL_PASSWORD`      |
| Default schema   | `skillbridge`       |

Then run `SELECT * FROM users;`, `SELECT * FROM projects;`, etc. Rows added
through the app (register, create project, send message…) are written straight
to this database and appear immediately — the backend has no in-memory store,
so the data also survives restarts.

> **Second MySQL install:** this Mac also has an older Homebrew MySQL (data kept
> in `/opt/homebrew/var/mysql`, configured for port **3307** and **stopped**).
> It is not used by the app. A backup dump of its data is at
> `/tmp/skillbridge-backup-3307.sql`, and the previous, incompatible 13-table
> schema was preserved as the database `skillbridge_legacy`.

Seed data includes demo users, projects, tasks, milestones, mentorship offers,
notifications and an admin account. **Password for every demo account:
`skillbridge1`** — notably:

- `karthik@skillbridge.edu` — student
- `admin@skillbridge.edu` — admin

## 2. Backend

```bash
cd backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  mvn spring-boot:run
```

- Runs on **http://localhost:8080** (CORS open for dev; the frontend proxies `/api`).
- Config in `backend/src/main/resources/application.properties`
  (MySQL URL/port/credentials, upload dir `./uploads`).
- Note: build with **Temurin (or any) JDK 21** — newer JDKs can break Lombok
  annotation processing inside Maven.

> If you just want to compile first: `JAVA_HOME=… mvn -q compile`.

## 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- Runs on **http://localhost:5173** (if that port is busy Vite picks 5174 and
  prints the actual URL); it proxies `/api/*` → `http://localhost:8080`.
- Production bundle: `npm run build` → `dist/`.

## 4. Demo walkthrough (SRS use cases)

1. **U1 Auth** — register at `/register`, log in, reset/change password.
2. **U2 Discovery** — `Profile` → add skills; `Teammates` → search students;
   `Recommendations` → skill-matched projects.
3. **U3 Projects** — `CreateProject` (draft/publish, edit as owner);
   join requests & invitations on `Projects` / `Requests`.
4. **U4 Collaboration** — `ProjectDetails` tabs: team, group chat, file upload
   (≤10 MB, pdf/img/zip/…).
5. **U5 Tasks** — `Tasks` board: assign, move status, review flow, comments,
   progress bars = project detail progress tab.
6. **U6 Messages** — `Collaboration` DMs (threaded).
7. **U7 Notifications** — bell in the navbar + `Notifications` page
   (mark read / mark all read).
8. **U8 Mentorship & portfolio** — `Mentorship` offers/requests, complete a
   session, `Feedback` rating, `Portfolio` entries (public/private).
9. **U9 Admin** — log in as `admin@skillbridge.edu` → `Admin` tabs: accounts
   suspend/reactivate, skill catalogue CRUD, report review, audit trail, stats.

## Project layout

```
project/
├── backend/          Spring Boot app (entities, repos, services, controllers)
├── database/         schema.sql + seed.sql (authoritative DB)
└── frontend/         React + Vite + Tailwind v4 app
```

## API auth model

`POST /api/auth/login` → `{ token, user }`; send `Authorization: Bearer <token>`
on all other `/api/**` calls. Passwords are BCrypt-hashed; tokens are opaque,
SHA-256-hashed at rest, and valid 30 days.

## Troubleshooting

- **Backend won't start / `Communications link failure`** — MySQL isn't running.
  Open **System Settings → MySQL → Start MySQL Server** (needs admin password).
- **`Access denied for user 'root'@'localhost'`** — the password in
  `application.properties` must match the MySQL root password
  (`YOUR_MYSQL_PASSWORD`). To use a different one without editing code, start the
  backend with `DB_PASSWORD='your-password' mvn spring-boot:run`.
- **Workbench shows no `skillbridge` tables** — check the connection uses port
  **3306** (not 3307) and the default schema is `skillbridge`.
- **Port 3306 already in use** — another MySQL install is running. Only the
  3306 server is used by this project; stop any other one before starting.
