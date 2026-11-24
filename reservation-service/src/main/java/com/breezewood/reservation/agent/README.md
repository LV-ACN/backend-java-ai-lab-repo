# agent/

Empty on purpose. You build the Reservation Domain Agent's MCP tools here in
lab Section 7 (`get_property_capacity`, `check_overbooking_availability`,
`create_reservation`, via Spring AI's MCP Server support), and its A2A
interface here in Extended activity E4 (`GET /.well-known/agent-card.json`,
`POST /a2a/tasks`) — both calling `ReservationService` directly, not a new
REST client pointed at this same app.

Short on time? `solutions/agent/` has a complete reference implementation you
can compare against or copy in as a fallback.
