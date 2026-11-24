# Technical Plan: Controlled Overbooking Allowance (Java track)

## Technical Context

`reservation-service` is the existing Java 25 / Spring Boot app to extend
(`ReservationController` → `ReservationService` → `InventoryClient`).
`inventory-service` is a stable Node/Express service and **must not be
modified**; its `POST /api/properties/{propertyId}/inventory/{roomType}/book`
endpoint increments the booked count without checking any limit — that
enforcement is this feature's job, and it lives in `reservation-service` only.

A booking decision requires two separate HTTP calls to `inventory-service` — a
read (`GET .../inventory/{roomType}`) then, if accepted, an increment (`POST
.../book`) — with no transaction spanning the two. That gap is the source of
this plan's concurrency requirement.

## API Contract

- Keep `POST /api/reservations` (201 on success, unchanged request/response
  shape) and `GET /api/reservations/{reservationNumber}`.
- On over-capacity, return **409** with `{"error": "<message>"}` (see spec
  FR-3); on unknown property/room type, **404**; both via the existing
  `ApiExceptionHandler` pattern (new exception type, new `@ExceptionHandler`).
- Add `GET /api/properties/{propertyId}/rooms/{roomType}/capacity` →
  `{"publishedRooms", "bookedRooms", "percentage", "capacity", "available"}`.
- Add `GET|PUT /api/admin/properties/{propertyId}/overbooking-percentage` →
  `GET` returns `{"percentage": number}`; `PUT` takes `{"percentage": number}`
  and applies it immediately to the next request for that property. Reject a
  negative or absurd (for example, >100 without an explicit business sign-off)
  percentage with 400.

## NFRs, made measurable

| NFR | Testable constraint |
|---|---|
| Performance | The capacity check adds one in-memory policy lookup and no extra network calls beyond the existing inventory read. |
| Concurrency & consistency | N concurrent requests for the same `(propertyId, roomType)` never leave `bookedRooms` above `capacity`, even though the counter lives in another process reached over HTTP. |
| Observability | Every accepted or rejected decision is logged with `publishedRooms`, `bookedRooms`, `percentage`, `capacity` and `requestedRooms`. |
| Configurability | The admin `PUT` takes effect on the very next request for that property — no restart, no redeploy. |

## The interleaving that oversells (why a guard is needed at all)

```
Capacity 12, booked 11. Two requests for 1 room each:
  Request A: GET count -> 11  (11 < 12, OK)
  Request B: GET count -> 11  (11 < 12, OK)
  Request A: POST book -> 12
  Request B: POST book -> 13   <-- oversold
```

The check and the act are separate HTTP calls, and `inventory-service` accepts
any increment it's asked to make — it has no concept of a limit.

## Concurrency guard and its assumption

A per-`(propertyId, roomType)` `ReentrantLock` inside `ReservationService`
wraps the **whole** read-check-increment sequence for a booking, so two
requests for the same key can never both pass the check before either
increments.

**Deployment assumption this relies on:** exactly one `reservation-service`
instance, and nothing else increments `inventory-service`'s counter for that
key. This is a single point of failure for the guard, by design, for the core
lab — Extended activity E1 tests this assumption directly by running a second
instance and shows it break.

## Data model

- `OverbookingPolicyService`: an in-memory `Map<propertyId, Integer>` of
  runtime percentage overrides, defaulting to 20 when a property has no
  override. Validates input (0–100, or a documented wider range if the
  business explicitly allows it) before accepting a `PUT`.
- `BookingAuditLogger`: one structured log line per decision (accepted or
  rejected), carrying the fields in the Observability row above.

## Core logic (shape to implement against)

```java
lock(propertyId + "|" + roomType)
  inventory  = inventoryClient.getInventory(propertyId, roomType)
  percentage = policy.getPercentage(propertyId)
  capacity   = floor(inventory.publishedRooms() * (1 + percentage / 100.0))
  available  = capacity - inventory.bookedRooms()
  if (rooms > available) {
      auditLogger.rejected(propertyId, roomType, inventory, percentage, capacity, rooms)
      throw new BookingRejectedException(...)       // -> 409, no increment
  }
  inventoryClient.incrementBooked(propertyId, roomType, rooms)
  auditLogger.accepted(propertyId, roomType, inventory, percentage, capacity, rooms, reservationNumber)
  return response                                     // -> 201
unlock
```

## Assumptions and limits

- The lock is a JVM-local `ReentrantLock` map — it protects nothing across
  multiple instances or restarts (see the deployment assumption above).
- Runtime percentage overrides are in-memory only; they reset to the default
  (20%) on restart. That's acceptable for this lab and called out explicitly
  rather than silently assumed.
- No idempotency handling yet — a retried request is treated as a brand-new
  booking. Extended activity E2 adds this.
