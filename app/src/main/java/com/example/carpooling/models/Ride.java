package com.example.carpooling.models;

public class Ride {
    private String rideId;
    private String driverId;
    private String origin;
    private String destination;
    private String departureTime;
    private double price;
    private int availableSeats;
    private double latitude;
    private double longitude;
    private String status;

    public Ride() {}

    public Ride(String rideId, String driverId, String origin, String destination, String departureTime, double price, int availableSeats, double latitude, double longitude, String status) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.price = price;
        this.availableSeats = availableSeats;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public String getRideId() { return rideId != null ? rideId : ""; }
    public String getDriverId() { return driverId != null ? driverId : ""; }
    public String getOrigin() { return origin != null ? origin : "Chưa xác định"; }
    public String getDestination() { return destination != null ? destination : "Chưa xác định"; }
    public String getDepartureTime() { return departureTime != null ? departureTime : "Chưa xác định"; }
    public double getPrice() { return price; }
    public int getAvailableSeats() { return availableSeats; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getStatus() { return status != null ? status : "AVAILABLE"; }

    public void setRideId(String rideId) { this.rideId = rideId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public void setPrice(double price) { this.price = price; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Ride{rideId='" + rideId + "', origin='" + origin + "', destination='" + destination + "'}";
    }
}