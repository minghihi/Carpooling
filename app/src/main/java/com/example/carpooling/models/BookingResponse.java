package com.example.carpooling.models;

public class BookingResponse {
    private int id;
    private int ride_id;
    private int passenger_id;
    private int seats_booked;
    private String status;

    public int getId() { return id; }
    public int getRideId() { return ride_id; }
    public int getPassengerId() { return passenger_id; }
    public int getSeatsBooked() { return seats_booked; }
    public String getStatus() { return status; }
}