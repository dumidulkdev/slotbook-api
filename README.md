# SlotBook

SlotBook is a small appointment booking REST API for independent service providers (tutors, barbers, consultants) who still take bookings over chat. Chat-based booking leads to double bookings, unclear schedules, and missed appointments. SlotBook lets a provider publish weekly availability and lets clients book concrete time slots safely.

## Features

- User registration and JWT login with roles `PROVIDER` and `CLIENT`
- Providers set weekly working hours and slot length (POST/PUT `/api/providers/me/availability`)
- Public listing of providers and free slots for a given date
- Clients book available slots with double-booking protection
- Paged "my bookings" view for clients and providers
- Cancellation with a configurable cut-off window (default 2 hours)
- Provider outcomes: complete or no-show after start time
- Consistent JSON success/error responses and OpenAPI documentation (Swagger UI)

## Tech stack

- Java 21, Spring Boot 3.4, Maven
- Spring Web, Spring Data JPA, Spring Security (stateless JWT, BCrypt)
- Bean Validation, springdoc-openapi (Swagger UI)
- H2 (local profile), PostgreSQL (prod profile)
- JUnit 5, Mockito, Spring Boot Test, MockMvc
- Docker and Docker Compose

## Running the application

### Maven (local / H2)

```bash
mvn spring-boot:run
```

The `local` profile is active by default. Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Docker Compose (PostgreSQL)

```bash
docker compose up --build
```

### Configuration

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` (H2) or `prod` (PostgreSQL) |
| `JWT_SECRET` | HMAC secret for signing JWTs (required in prod) |
| `JWT_EXPIRATION_MINUTES` | Token lifetime (default 60 in prod, 480 in local) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection (prod) |
| `APP_TIMEZONE` | Configured via `app.timezone` in `application.yml` (default `UTC`) |

Local profile includes a development-only JWT secret. Always set `JWT_SECRET` in production.

### Swagger authorization

1. Call `POST /api/auth/login` with demo credentials (below) or register a new user.
2. Copy the `token` from `data.token` in the response.
3. Open Swagger UI, click **Authorize**, and enter `Bearer <token>`.

## Demo credentials (local profile)

| Role | Email | Password |
| --- | --- | --- |
| Provider | `dana.provider@slotbook.dev` | `Provider123!` |
| Client | `chris.client@slotbook.dev` | `Client123!` |

The demo provider has Mon–Fri 09:00–17:00 availability with 30-minute slots.

## API endpoints

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Register as PROVIDER or CLIENT |
| POST | `/api/auth/login` | Public | Login and receive JWT |
| GET | `/api/providers` | Public | List providers (paged) |
| GET | `/api/providers/{id}/slots?date=YYYY-MM-DD` | Public | Free slots for a date |
| POST | `/api/providers/me/availability` | PROVIDER | Create availability |
| PUT | `/api/providers/me/availability` | PROVIDER | Update availability |
| GET | `/api/providers/me/availability` | PROVIDER | Read own availability |
| POST | `/api/bookings` | CLIENT | Book a slot |
| GET | `/api/bookings/me` | Authenticated | Own bookings (paged, by start time) |
| PATCH | `/api/bookings/{id}/cancel` | Client or provider on the booking | Cancel |
| PATCH | `/api/bookings/{id}/complete` | Owning PROVIDER | Mark completed |
| PATCH | `/api/bookings/{id}/no-show` | Owning PROVIDER | Mark no-show |

## Example requests

```bash
# Login as demo client
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"chris.client@slotbook.dev","password":"Client123!"}'
```

```bash
# List free slots (replace PROVIDER_ID and DATE)
curl -s "http://localhost:8080/api/providers/1/slots?date=2026-11-20"
```

```bash
# Book a slot
curl -s -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"providerId":1,"slotStart":"2026-11-20T09:00:00"}'
```

```bash
# Second booking for the same slot returns HTTP 409
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"providerId":1,"slotStart":"2026-11-20T09:00:00"}'
```

```bash
# Cancel a booking (replace BOOKING_ID)
curl -s -X PATCH http://localhost:8080/api/bookings/1/cancel \
  -H "Authorization: Bearer $TOKEN"
```

## Booking rules

- Only `CLIENT` users can create bookings.
- The requested slot must match provider availability, align to slot length, and start in the future.
- Active bookings are unique per provider and start time (database unique constraint on `provider_id` + `active_start_time`, optimistic locking via `@Version`).
- Cancellation is allowed for the client or provider on the booking only if status is `BOOKED` and the start is at least **2 hours** away (configurable).
- `COMPLETE` and `NO_SHOW` are provider-only, allowed only after the start time, and only from `BOOKED`.
- Cancelled slots release the time and appear again in free-slot listings.

## Project structure

```
src/main/java/com/slotbook/
  controller/    REST endpoints
  service/       Business logic
  repository/    Spring Data JPA
  domain/        JPA entities
  dto/           Java record DTOs
  mapper/        Entity/DTO mapping
  security/      JWT filter and configuration
  config/        App properties, OpenAPI, demo seed
  exception/     Global exception handling
src/test/        Unit and integration tests
```

## What I learned

- Wiring **stateless JWT** authentication with Spring Security role checks at the controller/service layer.
- Combining a **unique constraint** on active slot starts with **optimistic locking** so concurrent booking attempts yield exactly one success.
- Generating bookable slots from weekly windows using **java.time** plus an injectable **Clock** for deterministic tests.
- Testing secured flows with **MockMvc**, including 401/403 behavior and multi-threaded booking contention.

## Tests

```bash
mvn verify
```

## License

[MIT License](LICENSE). Developed by Dumidu Lakshan.