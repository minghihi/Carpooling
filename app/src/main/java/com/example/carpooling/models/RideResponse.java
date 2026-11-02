package com.example.carpooling.models;

import com.google.gson.annotations.SerializedName;

public class RideResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("origin")
    private String origin;

    @SerializedName("destination")
    private String destination;

    @SerializedName("departure_time")
    private String departureTime;

    @SerializedName("available_seats")
    private int availableSeats;

    @SerializedName("price")
    private double price;

    @SerializedName("status")
    private String status;

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getDriverId() { return driverId != null ? driverId : ""; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getOrigin() {
        return (origin != null && !origin.isEmpty()) ? origin : "Điểm đi chưa xác định";
    }

    public String getDestination() {
        return (destination != null && !destination.isEmpty()) ? destination : "Điểm đến chưa xác định";
    }

    public String getStartLocation() { return getOrigin(); }
    public String getEndLocation() { return getDestination(); }

    public String getDepartureTime() {
        return (departureTime != null && !departureTime.isEmpty()) ? departureTime : "Chưa xác định";
    }

    public int getAvailableSeats() { return availableSeats; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }

    public double getLatitude() {
        try {
            int numericId = Integer.parseInt(id);
            return 21.0362 + (numericId * 0.0015);
        } catch (Exception e) {
            return 21.0362;
        }
    }

    public double getLongitude() {
        try {
            int numericId = Integer.parseInt(id);
            return 105.7828 + (numericId * 0.0012);
        } catch (Exception e) {
            return 105.7828;
        }
    }
}