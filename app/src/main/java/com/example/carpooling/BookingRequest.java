package com.example.carpooling;

public class BookingRequest {
    private int passengerId;
    private int rideId;

    public BookingRequest(int passengerId, int rideId) {
        this.passengerId = passengerId;
        this.rideId = rideId;
    }
}