# AmenityHub API — curl Collection

Base URL: `http://localhost:9099`

All request bodies are JSON. On **Windows PowerShell**, inline JSON with `-d '{...}'`
often gets mangled by the shell, so the reliable pattern is to write the body to a
file and send it with `--data-binary "@file.json"`. On macOS/Linux/Git Bash the
inline `-d '{...}'` form works fine.

Seeded accounts (created automatically on startup):

| Email                      | Password      | Role         |
|----------------------------|---------------|--------------|
| `admin@amenityhub.local`   | `admin1234`   | ROLE_ADMIN   |
| `manager@amenityhub.local` | `manager1234` | ROLE_MANAGER |

---

## 1. Auth

### Register (public) — returns an access token
```bash
curl -X POST http://localhost:9099/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"resident@example.com","password":"password123","fullName":"Resident One","phoneNumber":"9990001111"}'
```

### Login (public) — returns an access token
```bash
curl -X POST http://localhost:9099/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@amenityhub.local","password":"manager1234"}'
```

Copy the `accessToken` from the response and use it as `Bearer <TOKEN>` below.

---

## 2. Resources

### List all active resources (public)
```bash
curl http://localhost:9099/api/v1/resources
```

### Get a single resource (public)
```bash
curl http://localhost:9099/api/v1/resources/1
```

### Get availability for a resource on a date (public)
```bash
curl "http://localhost:9099/api/v1/resources/1/availability?date=2026-09-05"
```

### Create a resource (MANAGER / ADMIN only)
```bash
curl -X POST http://localhost:9099/api/v1/resources \
  -H "Authorization: Bearer <MANAGER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Community Gym","description":"Shared gym","resourceType":"GYM","capacity":1,"cancellationWindowHours":2}'
```
`resourceType` is one of: `GYM`, `PARTY_HALL`, `EV_CHARGER`, `PARKING`,
`CO_WORKING`, `SWIMMING_POOL`, `TENNIS_COURT`, `OTHER`.

### Create an availability slot for a resource (MANAGER / ADMIN only)
```bash
curl -X POST http://localhost:9099/api/v1/resources/1/slots \
  -H "Authorization: Bearer <MANAGER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"startTime":"2026-09-05T10:00:00Z","endTime":"2026-09-05T11:00:00Z","totalCapacity":1}'
```
`startTime` / `endTime` must be in the future (ISO-8601 UTC).

---

## 3. Bookings (all require a Bearer token)

### Book a slot
```bash
curl -X POST http://localhost:9099/api/v1/bookings \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"slotId":1,"joinWaitlistIfFull":true}'
```
- Returns `201` with a CONFIRMED booking if capacity is free.
- Returns `202` with a waitlist entry if the slot is full and `joinWaitlistIfFull` is true.

### List my bookings
```bash
curl http://localhost:9099/api/v1/bookings \
  -H "Authorization: Bearer <TOKEN>"
```

### Get one of my bookings by reference
```bash
curl http://localhost:9099/api/v1/bookings/AMN-XXXXXXXX \
  -H "Authorization: Bearer <TOKEN>"
```

### Cancel a booking (frees the seat, auto-promotes next waitlisted user)
```bash
curl -X DELETE http://localhost:9099/api/v1/bookings/1 \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Response codes you may see

| Code | Meaning                                                        |
|------|---------------------------------------------------------------|
| 200  | OK                                                            |
| 201  | Created (registration, resource/slot created, booking confirmed) |
| 202  | Accepted (added to waitlist)                                  |
| 400  | Validation failed (bad request body)                          |
| 401  | Unauthorized (missing/invalid token, or bad login)            |
| 403  | Forbidden (resident trying a manager/admin action)            |
| 404  | Not found (resource/slot/booking doesn't exist or not yours)  |
| 409  | Conflict (duplicate email, already booked, already waitlisted)|
| 422  | Business rule violation (past slot, cancellation window, full)|
