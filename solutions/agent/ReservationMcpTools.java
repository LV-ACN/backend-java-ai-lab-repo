// REFERENCE IMPLEMENTATION ONLY -- Section 7.
//
// Drop this file (and ReservationA2AController.java, for Extended activity
// E4) into src/main/java/com/breezewood/reservation/agent/ to use as a
// fallback if you're short on time.
//
// Verified against Spring AI BOM 1.0.0 with a real MCP client (listTools +
// callTool round trip): this starter's MCP server transport is classic SSE,
// not Streamable HTTP -- Cursor (or any MCP client) connects to GET /sse,
// which hands back a session-scoped POST endpoint at /mcp/message?sessionId=...
// for the rest of the exchange. That differs from the Node track, which uses
// Streamable HTTP directly at POST /mcp -- both are valid MCP transports,
// just configured differently per stack. Re-confirm if you're on a newer
// Spring AI release, since this may change.
//
// pom.xml, in addition to the existing dependencies (plus the Spring AI BOM
// 1.0.0 in <dependencyManagement>, matching your Spring Boot / Spring AI
// compatibility matrix):
//
//   <dependency>
//     <groupId>org.springframework.ai</groupId>
//     <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
//   </dependency>
//
// Registration bean, in this package or a @Configuration class (verified --
// Spring AI logs "Registered tools: 3" at startup once this is wired in):
//
//   @Bean
//   public ToolCallbackProvider reservationTools(ReservationMcpTools tools) {
//     return MethodToolCallbackProvider.builder().toolObjects(tools).build();
//   }
//
// This is a thin translation layer only -- every method below calls
// ReservationService or InventoryClient directly. No capacity math here.
package com.breezewood.reservation.agent;

import com.breezewood.reservation.client.InventoryClient;
import com.breezewood.reservation.model.InventoryRecord;
import com.breezewood.reservation.model.ReservationRequest;
import com.breezewood.reservation.model.ReservationResponse;
import com.breezewood.reservation.service.ReservationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ReservationMcpTools {

    private final ReservationService reservationService;
    private final InventoryClient inventoryClient;

    public ReservationMcpTools(ReservationService reservationService, InventoryClient inventoryClient) {
        this.reservationService = reservationService;
        this.inventoryClient = inventoryClient;
    }

    // name= pins the tool name Spring AI exposes over MCP -- without it, the
    // default is the Java method name verbatim (getPropertyCapacity), which
    // doesn't match the snake_case tool names used throughout the lab guide
    // and the Node track (verified: Spring AI 1.0.0's default naming is
    // camelCase-from-method-name, confirmed with a real MCP client).
    @Tool(name = "get_property_capacity", description = "Read the published rooms, booked rooms and available rooms for a property and room type. Read-only.")
    public CapacityResult getPropertyCapacity(String propertyId, String roomType) {
        InventoryRecord inventory = inventoryClient.getInventory(propertyId, roomType);
        int available = inventory.publishedRooms() - inventory.bookedRooms();
        return new CapacityResult(propertyId, roomType, inventory.publishedRooms(), inventory.bookedRooms(), available);
    }

    @Tool(name = "check_overbooking_availability", description = "Check whether a given number of rooms can currently be booked for a property and room type, without booking them. Read-only.")
    public AvailabilityResult checkOverbookingAvailability(String propertyId, String roomType, int rooms) {
        InventoryRecord inventory = inventoryClient.getInventory(propertyId, roomType);
        int available = inventory.publishedRooms() - inventory.bookedRooms();
        return new AvailabilityResult(rooms <= available, available);
    }

    @Tool(name = "create_reservation", description = "Create a reservation for a property, room type, number of rooms and guest. Books exactly the requested number of rooms, or returns the service's rejection reason unchanged. State-changing -- the caller must confirm with the user before invoking this.")
    public ReservationResponse createReservation(String propertyId, String roomType, int rooms,
                                                  String guestName, String guestEmail,
                                                  String checkIn, String checkOut) {
        ReservationRequest request = new ReservationRequest(
                propertyId, roomType, rooms, guestName, guestEmail, checkIn, checkOut);
        // BookingRejectedException propagates as-is; the tool layer never
        // soft-catches or rewords the service's rejection reason.
        return reservationService.book(request);
    }

    public record CapacityResult(String propertyId, String roomType, int publishedRooms, int bookedRooms, int available) {}

    public record AvailabilityResult(boolean canBook, int available) {}
}
