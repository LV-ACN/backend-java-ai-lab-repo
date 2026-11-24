// REFERENCE IMPLEMENTATION ONLY -- Extended activity E4.
//
// Drop this file into src/main/java/com/breezewood/reservation/agent/
// alongside ReservationMcpTools.java. No new dependency needed -- this is a
// plain @RestController, the same shape as ReservationController.
//
// A task that fails for a business reason (capacity exceeded, missing
// confirmation) is still a successfully *handled* task: it comes back as
// HTTP 200 with {"status":"failed", "error": "..."}, not a raw 409. That's
// the A2A task-result convention, distinct from the REST controller's HTTP
// status mapping in ApiExceptionHandler -- the same business rule, reported
// two different ways depending on which interface asked.
package com.breezewood.reservation.agent;

import com.breezewood.reservation.client.InventoryClient;
import com.breezewood.reservation.exception.BookingRejectedException;
import com.breezewood.reservation.model.InventoryRecord;
import com.breezewood.reservation.model.ReservationRequest;
import com.breezewood.reservation.model.ReservationResponse;
import com.breezewood.reservation.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ReservationA2AController {

    private final ReservationService reservationService;
    private final InventoryClient inventoryClient;

    public ReservationA2AController(ReservationService reservationService, InventoryClient inventoryClient) {
        this.reservationService = reservationService;
        this.inventoryClient = inventoryClient;
    }

    @GetMapping("/.well-known/agent-card.json")
    public AgentCard agentCard() {
        return new AgentCard(
                "reservation-agent",
                "Reservation Domain Agent for Breezewood Hotels -- read capacity, check availability, create a reservation, all within the published overbooking policy.",
                List.of("get_capacity", "check_availability", "create_reservation"));
    }

    @PostMapping("/a2a/tasks")
    public ResponseEntity<TaskResult> handleTask(@RequestBody TaskRequest task) {
        try {
            Object output = switch (task.skill()) {
                case "get_capacity" -> capacity(task.input());
                case "check_availability" -> availability(task.input());
                case "create_reservation" -> createReservation(task.input());
                default -> throw new IllegalArgumentException("Unknown skill: " + task.skill());
            };
            return ResponseEntity.ok(new TaskResult("completed", output, null));
        } catch (BookingRejectedException e) {
            return ResponseEntity.ok(new TaskResult("failed", null, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new TaskResult("failed", null, e.getMessage()));
        }
    }

    private Map<String, Object> capacity(Map<String, Object> input) {
        InventoryRecord inventory = inventoryClient.getInventory(
                (String) input.get("propertyId"), (String) input.get("roomType"));
        int available = inventory.publishedRooms() - inventory.bookedRooms();
        return Map.of(
                "publishedRooms", inventory.publishedRooms(),
                "bookedRooms", inventory.bookedRooms(),
                "available", available);
    }

    private Map<String, Object> availability(Map<String, Object> input) {
        InventoryRecord inventory = inventoryClient.getInventory(
                (String) input.get("propertyId"), (String) input.get("roomType"));
        int rooms = ((Number) input.get("rooms")).intValue();
        int available = inventory.publishedRooms() - inventory.bookedRooms();
        return Map.of("canBook", rooms <= available, "available", available);
    }

    private ReservationResponse createReservation(Map<String, Object> input) {
        if (!Boolean.TRUE.equals(input.get("confirmed"))) {
            throw new IllegalArgumentException("create_reservation requires \"confirmed\": true in the task input");
        }
        ReservationRequest request = new ReservationRequest(
                (String) input.get("propertyId"),
                (String) input.get("roomType"),
                ((Number) input.get("rooms")).intValue(),
                (String) input.get("guestName"),
                (String) input.get("guestEmail"),
                (String) input.get("checkIn"),
                (String) input.get("checkOut"));
        return reservationService.book(request);
    }

    public record AgentCard(String name, String description, List<String> skills) {}

    public record TaskRequest(String skill, Map<String, Object> input) {}

    public record TaskResult(String status, Object output, String error) {}
}
