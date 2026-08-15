# Java Chat — REST + WebSocket Chat Webapp

Spring Boot chat app: REST API for auth/rooms/history, STOMP-over-WebSocket for
real-time messaging, PostgreSQL for persistence, and a small vanilla JS test
frontend bundled in.

## Stack
- Java 17, Spring Boot 3.3 (Web, WebSocket, Data JPA, Validation)
- PostgreSQL
- Maven
- Plain HTML/CSS/JS frontend (SockJS + STOMP.js via CDN) served from `src/main/resources/static`

## Prerequisites
- JDK 17+
- Maven 3.9+
- PostgreSQL running locally (or update `application.properties` to point elsewhere)

## 1. Create the database
```bash
createdb chatapp
# or in psql:
# CREATE DATABASE chatapp;
```

## 2. Configure credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/chatapp
spring.datasource.username=postgres
spring.datasource.password=postgres
```
Tables are auto-created on startup (`spring.jpa.hibernate.ddl-auto=update`).

## 3. Run
```bash
mvn spring-boot:run
```
App starts on **http://localhost:8080** — open it in a browser for the bundled
test client, register two users in two tabs, create a room, and chat.

## Authentication

`register` and `login` return a bearer **token** (opaque, server-issued,
24h TTL, held in an in-memory store — see `security/TokenStore.java`).

- **REST**: every `/api/rooms/**` call must include `Authorization: Bearer <token>`.
  `/api/auth/**` stays open (that's how you get a token in the first place).
- **WebSocket**: the token is passed as a query param on the handshake URL —
  `/ws?token=<token>` — and validated *before* the socket opens
  (`AuthHandshakeInterceptor`). Once connected, the session is bound to a real
  `Principal` (`CustomHandshakeHandler`), and `ChatWebSocketController` reads
  the sender's identity from that `Principal` — never from the client's
  message payload. This closes the sender-spoofing hole an earlier version
  of this project had.
- `POST /api/auth/logout` (with the bearer header) revokes the token.

## REST API

| Method | Path                          | Auth | Body                              | Description                    |
|--------|-------------------------------|------|-------------------------------------|---------------------------------|
| POST   | `/api/auth/register`          | —    | `{ "username", "password" }`       | Create a user, returns a token |
| POST   | `/api/auth/login`             | —    | `{ "username", "password" }`       | Verify credentials, returns a token |
| POST   | `/api/auth/logout`            | Bearer | —                                 | Revoke the current token       |
| GET    | `/api/rooms`                  | Bearer | —                                 | List all rooms                 |
| POST   | `/api/rooms`                  | Bearer | `{ "name" }`                     | Create a room                  |
| GET    | `/api/rooms/{id}`             | Bearer | —                                 | Get one room                   |
| GET    | `/api/rooms/{id}/messages?page=0&size=50` | Bearer | —                     | Paginated history (oldest→newest per page) |

## WebSocket (STOMP over SockJS)

- Connect: `ws(s)://<host>/ws?token=<bearer-token>` (SockJS handshake; rejected with 401 if the token is missing/invalid/expired)
- Subscribe for live messages: `/topic/room/{roomId}`
- Send a chat message: `/app/chat.sendMessage/{roomId}` with body `{ "content" }` — sender is taken from your authenticated session, not the payload
- Send a join/presence event: `/app/chat.join/{roomId}` with body `{}`

Every message sent over the socket is persisted to Postgres and broadcast to
all current subscribers of that room's topic, so REST history and the live
feed stay consistent.

## CORS / allowed origins

Set via `app.allowed-origins` in `application.properties` (comma-separated).
Defaults to `http://localhost:8080,http://127.0.0.1:8080` — add your real
frontend origin(s) before deploying, and never widen this to `*` now that
auth is in play (an open origin + cookie/token-based auth is how
cross-site WebSocket hijacking happens).

## Project layout
```
src/main/java/com/example/chatapp/
├── ChatApplication.java        entry point
├── config/                     WebSocket (STOMP) + CORS config
├── model/                      JPA entities: User, ChatRoom, ChatMessage
├── repository/                 Spring Data repositories
├── service/                    business logic (auth, rooms, messages)
├── controller/                 REST controllers + WebSocket @MessageMapping controller
├── dto/                        request/response payloads
└── exception/                  custom exceptions + @RestControllerAdvice handler

src/main/resources/
├── application.properties
└── static/                     bundled test frontend (index.html, css/, js/)
```

## Notes / next steps for a production version
- The token store is in-memory and single-instance — fine for dev/demo, but
  won't survive a restart or work across multiple app instances. Swap for
  signed JWTs or a shared store (Redis) for real deployments.
- Tokens travel over plain `ws://`/`http://` locally. Put this behind TLS
  (`wss://`/`https://`) before it touches the public internet — otherwise the
  bearer token is readable to anyone on the network path.
- `ddl-auto=update` is dev-only; switch to a migration tool (Flyway/Liquibase)
  before production.
- No rate limiting / message size limits — add before exposing publicly.
- No password reset / account recovery flow.
# pancake-chat
