package com.breezewood.reservation.service;

import com.breezewood.reservation.client.InventoryClient;
import com.breezewood.reservation.exception.BookingRejectedException;
import com.breezewood.reservation.exception.PropertyNotFoundException;
import com.breezewood.reservation.exception.ReservationNotFoundException;
import com.breezewood.reservation.model.InventoryRecord;
import com.breezewood.reservation.model.ReservationRequest;
import com.breezewood.reservation.model.ReservationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReservationService {

    private final InventoryClient inventoryClient;
    private final Map<String, ReservationResponse> reservations = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(100000);

    public ReservationService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    public ReservationResponse book(ReservationRequest request) {
        InventoryRecord inventory = lookupInventory(request.propertyId(), request.roomType());

        // NOTE: no overbooking allowance yet -- a request is only accepted if it
        // fits within the published room count exactly (capacity == publishedRooms).
        // This strict cap is the rule the lab's overbooking feature (section 4)
        // relaxes to 120% of publishedRooms, with the NFRs from section 3 layered in.
        int capacity = inventory.publishedRooms();
        int available = capacity - inventory.bookedRooms();
        if (request.rooms() > available) {
            throw new BookingRejectedException(
                    "Not enough available %s rooms at %s: %d requested, %d available"
                            .formatted(request.roomType(), request.propertyId(), request.rooms(), available));
        }

        inventoryClient.incrementBooked(request.propertyId(), request.roomType(), request.rooms());

        String reservationNumber = "BWR-" + sequence.getAndIncrement();
        ReservationResponse response = new ReservationResponse(
                reservationNumber,
                request.propertyId(),
                request.roomType(),
                request.rooms(),
                request.guestName(),
                request.checkIn(),
                request.checkOut());

        reservations.put(reservationNumber, response);
        return response;
    }

    public ReservationResponse find(String reservationNumber) {
        ReservationResponse response = reservations.get(reservationNumber);
        if (response == null) {
            throw new ReservationNotFoundException("No reservation found with number " + reservationNumber);
        }
        return response;
    }

    private InventoryRecord lookupInventory(String propertyId, String roomType) {
        try {
            return inventoryClient.getInventory(propertyId, roomType);
        } catch (HttpClientErrorException.NotFound e) {
            throw new PropertyNotFoundException(
                    "Unknown property/room type: %s/%s".formatted(propertyId, roomType));
        }
    }
}
