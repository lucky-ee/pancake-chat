# 🥞 pancake-chat — REST + WebSocket Chat Webapp

Spring Boot chat app: REST API for auth/rooms/history, STOMP-over-WebSocket for
real-time messaging (including typing indicators and read receipts), PostgreSQL
for persistence, and a pancake-themed vanilla JS frontend bundled in.

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
DB credentials are **not** committed. `application.properties` reads them from
the environment, defaulting to `postgres` with an empty password:
```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/chatapp}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:}
```
Point it at your own database either way:

```bash
# option A — environment variables
export DB_USER=myuser DB_PASSWORD=secret
```
```properties
# option B — application-local.properties in the project root (git-ignored,
# imported automatically, overrides the committed defaults)
spring.datasource.username=myuser
spring.datasource.password=secret
```
Tables are auto-created on startup (`spring.jpa.hibernate.ddl-auto=update`).

## 3. Run
```bash
mvn spring-boot:run
```
App starts on **http://localhost:8081** (`server.port` in
`application.properties`) — open it in a browser for the bundled client,
register two users in two tabs, create a room, and chat.

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
  of this project had. It applies to every socket destination: chat messages,
  join events, typing state, and read receipts.
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
| GET    | `/api/rooms/{id}/messages?page=0&size=50` | Bearer | —                     | Paginated history (page 0 = most recent, oldest→newest within a page) |
| GET    | `/api/rooms/{id}/receipts`    | Bearer | —                                 | Current read position per user in the room |

## WebSocket (STOMP over SockJS)

Connect: `ws(s)://<host>/ws?token=<bearer-token>` (SockJS handshake; rejected
with 401 if the token is missing/invalid/expired).

| Direction | Destination | Payload | Notes |
|-----------|-------------|---------|-------|
| Subscribe | `/topic/room/{roomId}`          | `ChatMessageDto`  | Live chat + join events |
| Subscribe | `/topic/room/{roomId}/typing`   | `{ username, typing }` | Ephemeral typing state |
| Subscribe | `/topic/room/{roomId}/receipts` | `{ username, lastReadMessageId, updatedAt }` | Read-position updates |
| Send      | `/app/chat.sendMessage/{roomId}` | `{ "content" }` | Sender comes from your session, not the payload |
| Send      | `/app/chat.join/{roomId}`        | `{}` | Join/presence event |
| Send      | `/app/chat.typing/{roomId}`      | `{ "typing": true\|false }` | Relayed only — never persisted |
| Send      | `/app/chat.read/{roomId}`        | `{ "lastReadMessageId": <id> }` | Persisted, then broadcast |

Chat and join messages are persisted to Postgres and broadcast to all current
subscribers of that room's topic, so REST history and the live feed stay
consistent.

### Typing indicators
Typing state is deliberately kept off the database and on its own topic, so it
can't be confused with real messages. The client sends `typing: true` on first
keystroke, then `typing: false` after a 2.5s pause or on send. Receivers also
apply a 4s safety-expiry per user, so a dropped "stopped typing" event (lost
connection, closed tab) can't leave the indicator stuck on.

### Read receipts
One row per `(room, user)` — see `model/ReadReceipt.java` and its unique
constraint — updated whenever a client views the newest message in a room.
Because it's persisted rather than session state, "seen" survives reconnects.
A client fetches `GET /api/rooms/{id}/receipts` once when opening a room so
existing seen-state renders immediately, then keeps it live via the
`/receipts` topic. The frontend shows a "Seen by …" marker under the last
message *you* sent.

## Bundled frontend

`src/main/resources/static` holds a three-screen client (auth → room list →
chat) with no build step — plain HTML/CSS/JS, SockJS and STOMP.js pulled from
a CDN. It's a real client, not just a test harness: pancake-themed styling,
per-user deterministic bubble/avatar colors, live typing indicator, seen
markers, a connection-status dot, and a sprinkle burst on send.

## CORS / allowed origins

Set via `app.allowed-origins` in `application.properties` (comma-separated),
applied to both `/api/**` CORS and the `/ws` STOMP endpoint. Currently set to
`http://localhost:8081,http://127.0.0.1:8081,http://192.168.1.93:8081` — the
last one is a LAN IP for testing from another device on the same network, and
should be replaced with your real frontend origin(s) before deploying. Never
widen this to `*` now that auth is in play (an open origin + cookie/token-based
auth is how cross-site WebSocket hijacking happens).

## Project layout
```
src/main/java/com/example/chatapp/
├── ChatApplication.java        entry point
├── config/                     WebSocket (STOMP) + CORS/interceptor config
├── security/                   token store, bearer-token interceptor,
│                               WebSocket handshake auth + Principal binding
├── model/                      JPA entities: User, ChatRoom, ChatMessage, ReadReceipt
├── repository/                 Spring Data repositories
├── service/                    business logic (auth, rooms, messages, receipts)
├── controller/                 REST controllers + WebSocket @MessageMapping controller
├── dto/                        request/response payloads (auth, chat, rooms, presence)
└── exception/                  custom exceptions + @RestControllerAdvice handler

src/main/resources/
├── application.properties
└── static/                     bundled frontend (index.html, css/, js/)
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
- Rooms are global: any authenticated user can read and post in any room.
  There's no membership model, so read receipts cover "whoever has read here,"
  not a defined member list.
- The simple in-memory STOMP broker doesn't fan out across instances — you'd
  need a real broker (RabbitMQ/ActiveMQ) to run more than one node.
- No rate limiting / message size limits — add before exposing publicly.
- No password reset / account recovery flow.
- No automated tests yet (`spring-boot-starter-test` is on the classpath, but
  there's no `src/test`).
