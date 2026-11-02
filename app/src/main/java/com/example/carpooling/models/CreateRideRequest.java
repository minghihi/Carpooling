package com.example.carpooling.models;

public class CreateRideRequest {
    private int driverId;
    private String origin;
    private String destination;
    private String departureTime;
    private int availableSeats;
    private double price;

    public CreateRideRequest(int driverId, String origin, String destination, String departureTime, int availableSeats, double price) {
        this.driverId = driverId;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.availableSeats = availableSeats;
        this.price = price;
    }

}