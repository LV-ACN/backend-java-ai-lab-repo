package com.breezewood.reservation.model;

public record ReservationResponse(
        String reservationNumber,
        String propertyId,
        String roomType,
        int rooms,
        String guestName,
        String checkIn,
        String checkOut
) {}
