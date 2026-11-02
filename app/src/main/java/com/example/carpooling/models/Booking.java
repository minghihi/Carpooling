package com.example.carpooling.models;

public class Booking {
    private String bookingId;
    private String rideId;
    private String passengerUid;
    private String passengerEmail;
    private String status;
    private float rating;
    private String review;

    private String origin;
    private String destination;
    private String departureTime;
    private int availableSeats;
    private double price;

    private int bookedSeats;
    private double pickupLat;
    private double pickupLon;

    public Booking() {}

    public Booking(String bookingId, String rideId, String passengerUid, String passengerEmail, String status) {
        this.bookingId = bookingId;
        this.rideId = rideId;
        this.passengerUid = passengerUid;
        this.passengerEmail = passengerEmail;
        this.status = status;
        this.bookedSeats = 1;
        this.rating = 0;
        this.review = "";
    }
    public Booking(String bookingId, String rideId, String passengerUid, String passengerEmail, String status, int bookedSeats) {
        this.bookingId = bookingId;
        this.rideId = rideId;
        this.passengerUid = passengerUid;
        this.passengerEmail = passengerEmail;
        this.status = status;
        this.bookedSeats = bookedSeats;
        this.rating = 0;
        this.review = "";
    }

    public String getBookingId() { return bookingId; }
    public String getRideId() { return rideId; }
    public String getPassengerUid() { return passengerUid; }
    public String getPassengerEmail() { return passengerEmail; }
    public String getStatus() { return status; }
    public float getRating() { return rating; }
    public String getReview() { return review; }

    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getDepartureTime() { return departureTime; }
    public int getAvailableSeats() { return availableSeats; }
    public double getPrice() { return price; }

    public int getBookedSeats() { return bookedSeats; }

    public double getPickupLat() { return pickupLat; }
    public double getPickupLon() { return pickupLon; }

    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public void setPassengerUid(String passengerUid) { this.passengerUid = passengerUid; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }
    public void setStatus(String status) { this.status = status; }
    public void setRating(float rating) { this.rating = rating; }
    public void setReview(String review) { this.review = review; }

    public void setOrigin(String origin) { this.origin = origin; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public void setPrice(double price) { this.price = price; }

    public void setBookedSeats(int bookedSeats) { this.bookedSeats = bookedSeats; }

    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }
    public void setPickupLon(double pickupLon) { this.pickupLon = pickupLon; }
}