package com.example.carpooling.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpooling.R;
import com.example.carpooling.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CurrentRideActivity extends AppCompatActivity {

    private TextView txtRideStatus, txtDriverInfo;
    private Button btnCallDriver, btnChatDriver, btnBackHome, btnOpenTrackingMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String bookingId;
    private String rideId;
    private String driverPhone = "0987654321";

    private ListenerRegistration bookingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_ride);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bookingId = getIntent().getStringExtra("BOOKING_ID");
        rideId = getIntent().getStringExtra("RIDE_ID");

        txtRideStatus = findViewById(R.id.txtRideStatus);
        txtDriverInfo = findViewById(R.id.txtDriverDetails);

        btnCallDriver = findViewById(R.id.btnCallDriver);
        btnChatDriver = findViewById(R.id.btnChatDriver);
        btnBackHome = findViewById(R.id.btnBackHome);
        btnOpenTrackingMap = findViewById(R.id.btnOpenTrackingMap);

        setupClickListeners();

        if (bookingId != null && !bookingId.isEmpty()) {
            checkBookingStatus();
        } else if (rideId != null && !rideId.isEmpty()) {
            findBookingIdByRideAndUser();
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin chuyến đi!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupClickListeners() {
        if (btnCallDriver != null) {
            btnCallDriver.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + driverPhone));
                startActivity(intent);
            });
        }

        if (btnChatDriver != null) {
            btnChatDriver.setOnClickListener(v -> {
                if (rideId != null && !rideId.isEmpty()) {
                    Intent intent = new Intent(CurrentRideActivity.this, ChatActivity.class);
                    intent.putExtra("RIDE_ID", rideId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Chưa có thông tin chuyến xe!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnOpenTrackingMap != null) {
            btnOpenTrackingMap.setOnClickListener(v -> {
                if (rideId != null && !rideId.isEmpty()) {
                    Intent intent = new Intent(CurrentRideActivity.this, PassengerTrackingActivity.class);
                    intent.putExtra("RIDE_ID", rideId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Chưa có mã chuyến đi để tracking!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(v -> {
                Intent intent = new Intent(CurrentRideActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private void findBookingIdByRideAndUser() {
        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (currentUid.isEmpty()) return;

        db.collection("bookings")
                .whereEqualTo("rideId", rideId)
                .whereEqualTo("passengerUid", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String st = doc.getString("status");
                            if (st != null && ("CANCELLED".equalsIgnoreCase(st) || "REJECTED".equalsIgnoreCase(st))) {
                                continue;
                            }
                            bookingId = doc.getId();
                            checkBookingStatus();
                            break;
                        }
                    } else {
                        db.collection("bookings")
                                .whereEqualTo("rideId", rideId)
                                .whereEqualTo("passengerId", currentUid)
                                .get()
                                .addOnSuccessListener(fallbackSnapshots -> {
                                    if (!fallbackSnapshots.isEmpty()) {
                                        for (QueryDocumentSnapshot doc : fallbackSnapshots) {
                                            String st = doc.getString("status");
                                            if (st != null && ("CANCELLED".equalsIgnoreCase(st) || "REJECTED".equalsIgnoreCase(st))) {
                                                continue;
                                            }
                                            bookingId = doc.getId();
                                            checkBookingStatus();
                                            break;
                                        }
                                    }
                                });
                    }
                });
    }

    private void checkBookingStatus() {
        if (bookingId == null || bookingId.isEmpty()) return;

        if (bookingListener != null) {
            bookingListener.remove();
        }

        bookingListener = db.collection("bookings").document(bookingId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    Booking booking = doc.toObject(Booking.class);
                    if (booking != null) {
                        if (booking.getRideId() != null && !booking.getRideId().isEmpty()) {
                            rideId = booking.getRideId();
                        }
                        loadDriverPhoneByRide(rideId);

                        String status = booking.getStatus() != null ? booking.getStatus().trim().toUpperCase() : "PENDING";

                        if ("CONFIRMED".equals(status) || "ACCEPTED".equals(status) || "APPROVED".equals(status) || "IN_PROGRESS".equals(status)) {
                            txtRideStatus.setText("Trạng thái: " + (("IN_PROGRESS".equals(status)) ? "Chuyến đi đang diễn ra!" : "Tài xế đã duyệt đơn!"));
                            if (btnOpenTrackingMap != null) {
                                btnOpenTrackingMap.setVisibility(View.VISIBLE);
                            }
                            if (txtDriverInfo != null) {
                                txtDriverInfo.setText("Tài xế đã xác nhận chỗ. Bạn có thể xem bản đồ di chuyển.");
                            }
                        } else {
                            txtRideStatus.setText("Trạng thái: Đang chờ tài xế duyệt đơn đặt chỗ...");
                            if (btnOpenTrackingMap != null) {
                                btnOpenTrackingMap.setVisibility(View.GONE);
                            }
                            if (txtDriverInfo != null) {
                                txtDriverInfo.setText("Vui lòng đợi tài xế xác nhận chỗ ngồi.");
                            }
                        }
                    }
                });
    }

    private void loadDriverPhoneByRide(String targetRideId) {
        if (targetRideId == null || targetRideId.isEmpty()) return;
        db.collection("rides").document(targetRideId).get().addOnSuccessListener(rideDoc -> {
            if (rideDoc.exists()) {
                String driverId = rideDoc.getString("driverId");
                if (driverId != null && !driverId.isEmpty()) {
                    db.collection("users").document(driverId).get().addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String phone = userDoc.getString("phone");
                            if (phone != null && !phone.isEmpty()) {
                                driverPhone = phone;
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null) bookingListener.remove();
    }
}