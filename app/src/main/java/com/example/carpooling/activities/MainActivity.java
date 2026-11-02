package com.example.carpooling.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.adapters.RideAdapter;
import com.example.carpooling.models.Ride;
import com.example.carpooling.utils.BottomNavHelper;
import com.example.carpooling.utils.FareCalculator;
import com.example.carpooling.utils.MapUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView rvAvailableRides;
    private RideAdapter rideAdapter;
    private List<Ride> rideList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Button btnOpenMap, btnActiveRide;
    private String userRole = "PASSENGER";
    private String activeRideId = null;

    private double currentLat = 21.0362;
    private double currentLon = 105.7828;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Đã cấp quyền thông báo", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Từ chối quyền thông báo", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent() != null && getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupBottomNavigation();

        askNotificationPermission();
        initFcmToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAvailableRidesFromFirestore();
        fetchPassengerActiveRide();
        fetchPassengerBookings();
    }

    private void initViews() {
        rvAvailableRides = findViewById(R.id.rvAvailableRides);
        rvAvailableRides.setLayoutManager(new LinearLayoutManager(this));

        btnOpenMap = findViewById(R.id.btnOpenMap);
        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            });
        }

        btnActiveRide = findViewById(R.id.btnActiveRide);
        if (btnActiveRide != null) {
            btnActiveRide.setOnClickListener(v -> {
                if (activeRideId != null && !activeRideId.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, CurrentRideActivity.class);
                    intent.putExtra("RIDE_ID", activeRideId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Bạn chưa có chuyến đi nào đang hoạt động!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        rideAdapter = new RideAdapter(this, rideList, ride -> {
            Toast.makeText(MainActivity.this, "Đang mở vị trí chuyến đi #" + ride.getRideId() + " trên bản đồ...", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("SELECTED_RIDE_ID", ride.getRideId());
            intent.putExtra("USER_ROLE", userRole);
            startActivity(intent);
        });

        rvAvailableRides.setAdapter(rideAdapter);
    }

    private void setupBottomNavigation() {
        BottomNavHelper.setupNav(this, userRole, 1);
    }

    private void fetchAvailableRidesFromFirestore() {
        db.collection("rides")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Pair<Ride, Double>> rideDistanceList = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Ride ride = doc.toObject(Ride.class);
                        if (ride != null) {
                            if (ride.getRideId() == null || ride.getRideId().isEmpty()) {
                                ride.setRideId(doc.getId());
                            }

                            String status = ride.getStatus() != null ? ride.getStatus().trim().toUpperCase() : "";
                            boolean isValidRide = !status.equals("COMPLETED") && !status.equals("CANCELLED");

                            if (!isValidRide) {
                                continue;
                            }

                            double distance = 0.0;
                            if (ride.getLatitude() != 0 && ride.getLongitude() != 0) {
                                distance = MapUtils.calculateDistance(
                                        currentLat, currentLon,
                                        ride.getLatitude(), ride.getLongitude()
                                );
                            }

                            if (distance > 200.0) {
                                continue;
                            }

                            double calculatedPrice = FareCalculator.calculateRideFare(distance);
                            ride.setPrice(calculatedPrice);

                            rideDistanceList.add(new Pair<>(ride, distance));
                        }
                    }

                    Collections.sort(rideDistanceList, (p1, p2) -> Double.compare(p1.second, p2.second));

                    rideList.clear();
                    for (Pair<Ride, Double> pair : rideDistanceList) {
                        rideList.add(pair.first);
                    }

                    rideAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lấy danh sách chuyến xe: " + e.getMessage(), e));
    }

    private void fetchPassengerBookings() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (currentUserId.isEmpty()) return;

        db.collection("bookings")
                .whereEqualTo("passengerUid", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> bookedRideIds = new ArrayList<>();

                    if (queryDocumentSnapshots.isEmpty()) {
                        if (rideAdapter != null) {
                            rideAdapter.setBookedRideIds(bookedRideIds);
                            rideAdapter.setHasActiveRide(false);
                        }
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String rId = doc.getString("rideId");
                        String bookingStatus = doc.getString("status");

                        if (rId != null && !"CANCELLED".equalsIgnoreCase(bookingStatus) && !"REJECTED".equalsIgnoreCase(bookingStatus)) {
                            db.collection("rides").document(rId).get().addOnSuccessListener(rideDoc -> {
                                if (rideDoc.exists()) {
                                    String rideStatus = rideDoc.getString("status");
                                    if (rideStatus != null) {
                                        String upperStatus = rideStatus.trim().toUpperCase();
                                        if (!upperStatus.equals("COMPLETED") && !upperStatus.equals("CANCELLED")) {
                                            bookedRideIds.add(rId);
                                            if (rideAdapter != null) {
                                                rideAdapter.setBookedRideIds(bookedRideIds);
                                                rideAdapter.setHasActiveRide(true);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải thông tin đặt chỗ hành khách: " + e.getMessage()));
    }

    private void fetchPassengerActiveRide() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            if (btnActiveRide != null) btnActiveRide.setVisibility(View.GONE);
            return;
        }

        db.collection("bookings")
                .whereEqualTo("passengerUid", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        activeRideId = doc.getString("rideId");

                        if (activeRideId == null || activeRideId.isEmpty()) {
                            activeRideId = doc.getId();
                        }

                        if (btnActiveRide != null) {
                            btnActiveRide.setVisibility(View.VISIBLE);
                        }
                    } else {
                        db.collection("bookings")
                                .whereEqualTo("passengerId", currentUserId)
                                .get()
                                .addOnSuccessListener(fallbackSnapshots -> {
                                    if (!fallbackSnapshots.isEmpty()) {
                                        DocumentSnapshot doc = fallbackSnapshots.getDocuments().get(0);
                                        activeRideId = doc.getString("rideId");
                                        if (activeRideId == null || activeRideId.isEmpty()) {
                                            activeRideId = doc.getId();
                                        }
                                        if (btnActiveRide != null) {
                                            btnActiveRide.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        activeRideId = null;
                                        if (btnActiveRide != null) {
                                            btnActiveRide.setVisibility(View.GONE);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (btnActiveRide != null) btnActiveRide.setVisibility(View.GONE);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải lịch sử đặt chỗ: " + e.getMessage());
                    if (btnActiveRide != null) {
                        btnActiveRide.setVisibility(View.GONE);
                    }
                });
    }

    private void initFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}