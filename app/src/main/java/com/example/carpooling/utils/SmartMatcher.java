package com.example.carpooling.utils;

import com.example.carpooling.models.Ride;
import com.example.carpooling.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class SmartMatcher {

    public interface OnSmartMatchListener {
        void onMatchedSuccess(List<Booking> matchedBookings);
        void onMatchedFailed(String error);
    }

    public static void findMatchingPassengersForRide(Ride driverRide, OnSmartMatchListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("bookings")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> matchedBookings = new ArrayList<>();

                    listener.onMatchedSuccess(matchedBookings);
                })
                .addOnFailureListener(e -> listener.onMatchedFailed(e.getMessage()));
    }
}