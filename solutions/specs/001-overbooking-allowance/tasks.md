# Tasks: Controlled Overbooking Allowance (Java track)

Each task is independently reviewable and should be its own small, scoped diff.

1. **Default allowance config.** Add `overbooking.default-percentage: 20` to
   `application.yml`.
2. **`OverbookingPolicyService`** (`service/OverbookingPolicyService.java`): a
   per-property runtime override map (`Map<String, Integer>`), falling back to
   the configured default; validates a percentage before accepting it
   (reject negative, reject implausibly large values with a clear message).
3. **Admin endpoints** (`controller/OverbookingAdminController.java`):
   `GET|PUT /api/admin/properties/{propertyId}/overbooking-percentage`,
   delegating to `OverbookingPolicyService`.
4. **Capacity endpoint** (new method on `ReservationController` or a small new
   `CapacityController`): `GET
   /api/properties/{propertyId}/rooms/{roomType}/capacity`, computing
   `publishedRooms`, `bookedRooms`, `percentage`, `capacity`, `available` from
   `InventoryClient` + `OverbookingPolicyService` — read-only, no booking side
   effects.
5. **`BookingAuditLogger`** (`service/BookingAuditLogger.java`): `accepted(...)`
   and `rejected(...)` methods, each logging the fields FR-6 / the plan's
   Observability row require.
6. **`ReservationService.book()`**: apply `capacity = floor(publishedRooms *
   (1 + percentage/100))` inside a per-`(propertyId, roomType)`
   `ReentrantLock`, replacing the strict `capacity == publishedRooms` check.
   Reject over-capacity requests with `BookingRejectedException` (409);
   accepted requests still call `InventoryClient.incrementBooked` exactly as
   before.
7. **Wire the audit logger into both branches** of `book()` — accepted and
   rejected — so every decision is recorded, not just failures.
8. **Update the starter tests.** `ReservationServiceApplicationTests` and any
   starter test asserting the old strict-capacity behavior now need updating
   to assert the new allowance-aware behavior instead (see spec FR-2/FR-3 for
   the exact numbers).

## Review gates (answer from the diff before committing)

| Gate | Question |
|---|---|
| Scope | Is `inventory-service/` untouched? (`git status` should show nothing there.) |
| Rule fidelity | Does the capacity calculation round exactly as `spec.md` FR-2 says — floor, never up? |
| Concurrency | Does the guard cover the **whole** read-then-increment sequence, or just one call? |
| Rejection path | On rejection, is the increment skipped and the audit entry still written? |
| Contract | Do paths, fields and status codes match `plan.md`'s API Contract exactly? |
| Security hygiene | Are admin inputs validated (no negative or absurd percentages)? No secrets or personal data in logs? |
| Dependencies | Were any new libraries added? Are they necessary and well maintained? |
