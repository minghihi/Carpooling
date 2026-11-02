package com.example.carpooling.utils;

public class FareCalculator {
    public static double calculateRideFare(double distanceInKm) {
        double baseFare = 12000.0;
        double baseKm = 2.0;
        double pricePerKm = 4500.0;

        double rawPrice;
        if (distanceInKm <= baseKm) {
            rawPrice = baseFare;
        } else {
            rawPrice = baseFare + ((distanceInKm - baseKm) * pricePerKm);
        }

        return roundToNearestThousand(rawPrice);
    }

    private static double roundToNearestThousand(double amount) {
        return Math.round(amount / 1000.0) * 1000.0;
    }
}