package com.example.carpooling.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpooling.R;
import com.example.carpooling.utils.SharedPrefManager;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvTotalRides;
    private Button btnManageUsers, btnModerateRides, btnManageSupport, btnViewLogs, btnLogout;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();

        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalRides = findViewById(R.id.tvTotalRides);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnModerateRides = findViewById(R.id.btnModerateRides);
        btnManageSupport = findViewById(R.id.btnManageSupport);
        btnViewLogs = findViewById(R.id.btnViewLogs);
        btnLogout = findViewById(R.id.btnLogout);

        loadSystemStatistics();
        setupClickListeners();
    }

    private void loadSystemStatistics() {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            int totalUsers = queryDocumentSnapshots.size();
            tvTotalUsers.setText(String.valueOf(totalUsers));
        });

        db.collection("rides").get().addOnSuccessListener(queryDocumentSnapshots -> {
            int totalRides = queryDocumentSnapshots.size();
            tvTotalRides.setText(String.valueOf(totalRides));
        });
    }

    private void setupClickListeners() {
        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminUserManagementActivity.class);
            startActivity(intent);
        });

        btnModerateRides.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminRideModerationActivity.class);
            startActivity(intent);
        });
        btnManageSupport.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminSupportActivity.class);
            startActivity(intent);
        });
        btnViewLogs.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, SystemLogActivity.class);
            startActivity(intent);
        });

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPrefManager.logoutUser(AdminDashboardActivity.this);
                finish();
            });
        }
    }
}