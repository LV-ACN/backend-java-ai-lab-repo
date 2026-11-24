package com.breezewood.reservation.exception;

public class BookingRejectedException extends RuntimeException {

    public BookingRejectedException(String message) {
        super(message);
    }
}
