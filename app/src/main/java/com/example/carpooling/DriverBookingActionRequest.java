package com.example.carpooling;

public class DriverBookingActionRequest {
    private int bookingId;

    public DriverBookingActionRequest(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
}