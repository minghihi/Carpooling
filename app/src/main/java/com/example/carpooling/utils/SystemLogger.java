package com.example.carpooling.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SystemLogger {

    public static void logAction(String action, String targetId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        String adminId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "UNKNOWN_ADMIN";

        Map<String, Object> logData = new HashMap<>();
        logData.put("adminId", adminId);
        logData.put("action", action);
        logData.put("target", targetId);
        logData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("system_logs").add(logData);
    }
}