package com.breezewood.reservation.model;

public record InventoryRecord(
        String propertyId,
        String roomType,
        int publishedRooms,
        int bookedRooms
) {}
