package com.breezewood.reservation.model;

public record ReservationRequest(
        String propertyId,
        String roomType,
        int rooms,
        String guestName,
        String guestEmail,
        String checkIn,
        String checkOut
) {}
