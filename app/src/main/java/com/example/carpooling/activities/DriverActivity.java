package com.example.carpooling.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.adapters.BookingAdapter;
import com.example.carpooling.adapters.DriverPassengerAdapter;
import com.example.carpooling.utils.BottomNavHelper;
import com.example.carpooling.utils.SharedPrefManager;
import com.example.carpooling.models.Booking;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DriverActivity extends AppCompatActivity {

    private static final String TAG = "DriverActivity";

    private TextView tvDriverName, tvTotalRides, tvEmptyRides, tvDriverRating;
    private Button btnCreateRide, btnOpenMap, btnLogout;
    private RecyclerView rvPendingBookings;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefManager prefManager;

    private BookingAdapter bookingAdapter;
    private List<Booking> pendingList;

    private FusedLocationProviderClient driverLocationClient;
    private LocationCallback driverLocationCallback;
    private String trackingRideId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

        driverLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupDriverHeader();
        setupRecyclerView();
        setupClickListeners();
        BottomNavHelper.setupNav(this, "DRIVER", 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDriverRidesFromFirestore();
        loadDriverRating();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSendingDriverLocation();
    }

    private void initViews() {
        tvDriverName = findViewById(R.id.tvDriverName);
        tvTotalRides = findViewById(R.id.tvTotalRides);
        tvEmptyRides = findViewById(R.id.tvEmptyRides);
        tvDriverRating = findViewById(R.id.tvDriverRating);
        btnCreateRide = findViewById(R.id.btnCreateRide);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnLogout = findViewById(R.id.btnLogout);
        rvPendingBookings = findViewById(R.id.rvDriverRides);
    }

    private void setupDriverHeader() {
        String userName = prefManager.getUserName();
        if (userName != null && !userName.isEmpty()) {
            tvDriverName.setText("Xin chào, " + userName);
        } else {
            tvDriverName.setText("Xin chào Tài xế");
        }
    }

    private void setupRecyclerView() {
        rvPendingBookings.setLayoutManager(new LinearLayoutManager(this));
        pendingList = new ArrayList<>();

        bookingAdapter = new BookingAdapter(pendingList, new BookingAdapter.OnBookingActionListener() {
            @Override
            public void onAccept(String bookingId) {
                for (Booking ride : pendingList) {
                    if (ride.getBookingId() != null && ride.getBookingId().equals(bookingId)) {
                        showDriverOptionsDialog(ride);
                        break;
                    }
                }
            }

            @Override
            public void onCancel(String bookingId) {
                updateRideStatusInFirestore(bookingId, "CANCELLED");
            }
        });

        rvPendingBookings.setAdapter(bookingAdapter);
    }

    private void setupClickListeners() {
        if (btnCreateRide != null) {
            btnCreateRide.setOnClickListener(v -> {
                Intent intent = new Intent(DriverActivity.this, CreateRideActivity.class);
                startActivity(intent);
            });
        }

        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                Intent intent = new Intent(DriverActivity.this, MapActivity.class);
                intent.putExtra("USER_ROLE", "DRIVER");
                startActivity(intent);
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }
    }

    private String getCurrentDriverId() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return String.valueOf(prefManager.getUserId());
    }

    private void loadDriverRating() {
        String currentDriverId = getCurrentDriverId();
        if (currentDriverId == null || currentDriverId.isEmpty() || currentDriverId.equals("-1")) return;

        db.collection("ratings")
                .whereEqualTo("driverId", currentDriverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        double totalScore = 0;
                        int count = queryDocumentSnapshots.size();

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Double rating = doc.getDouble("rating");
                            if (rating == null) {
                                rating = doc.getDouble("score");
                            }
                            if (rating != null) {
                                totalScore += rating;
                            }
                        }

                        double averageRating = totalScore / count;
                        String formattedRating = String.format("%.1f", averageRating);

                        if (tvDriverRating != null) {
                            tvDriverRating.setText(formattedRating);
                        }
                    } else {
                        if (tvDriverRating != null) {
                            tvDriverRating.setText("5.0");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải điểm đánh giá: ", e));
    }

    private void fetchDriverRidesFromFirestore() {
        String currentUserId = getCurrentDriverId();
        if (currentUserId == null || currentUserId.isEmpty() || currentUserId.equals("-1")) return;

        db.collection("rides")
                .whereEqualTo("driverId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pendingList.clear();

                    List<Booking> activeRides = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Booking rideItem = doc.toObject(Booking.class);
                        if (rideItem != null) {
                            if (rideItem.getBookingId() == null || rideItem.getBookingId().isEmpty()) {
                                rideItem.setBookingId(doc.getId());
                            }

                            String status = rideItem.getStatus();
                            if (status == null || "AVAILABLE".equalsIgnoreCase(status) ||
                                    "IN_PROGRESS".equalsIgnoreCase(status) || "FULL".equalsIgnoreCase(status)) {
                                activeRides.add(rideItem);
                            }
                        }
                    }

                    pendingList.addAll(activeRides);

                    if (tvTotalRides != null) {
                        tvTotalRides.setText(String.valueOf(queryDocumentSnapshots.size()));
                    }

                    bookingAdapter.notifyDataSetChanged();

                    if (pendingList.isEmpty()) {
                        if (tvEmptyRides != null) tvEmptyRides.setVisibility(View.VISIBLE);
                        rvPendingBookings.setVisibility(View.GONE);
                    } else {
                        if (tvEmptyRides != null) tvEmptyRides.setVisibility(View.GONE);
                        rvPendingBookings.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lấy danh sách chuyến xe: ", e));
    }

    private void showDriverOptionsDialog(Booking ride) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_driver_ride_manage, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvDialogRoute = dialogView.findViewById(R.id.tvDialogRoute);
        TextView tvDialogTime = dialogView.findViewById(R.id.tvDialogTime);
        TextView tvDialogSeats = dialogView.findViewById(R.id.tvDialogSeats);

        tvDialogRoute.setText("Tuyến: " + (ride.getOrigin() != null ? ride.getOrigin() : "Chưa có") + " ➔ " + (ride.getDestination() != null ? ride.getDestination() : "Chưa có"));
        tvDialogTime.setText("Thời gian: " + (ride.getDepartureTime() != null ? ride.getDepartureTime() : "Chưa có"));
        tvDialogSeats.setText("Số ghế trống: " + ride.getAvailableSeats() + " ghế");

        Button btnEditRide = dialogView.findViewById(R.id.btnEditRide);
        Button btnCallPassenger = dialogView.findViewById(R.id.btnCallPassenger);
        Button btnChatPassenger = dialogView.findViewById(R.id.btnChatPassenger);
        Button btnManagePassengers = dialogView.findViewById(R.id.btnManagePassengers);
        Button btnStartRide = dialogView.findViewById(R.id.btnStartRide);
        Button btnCompleteRide = dialogView.findViewById(R.id.btnCompleteRide);
        Button btnCancelRide = dialogView.findViewById(R.id.btnCancelRide);

        String currentStatus = ride.getStatus() != null ? ride.getStatus().trim().toUpperCase() : "";
        boolean isInProgress = "IN_PROGRESS".equals(currentStatus);

        if (isInProgress) {
            btnStartRide.setText("🗺Xem bản đồ trực tuyến");
            btnStartRide.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"));
        } else {
            btnStartRide.setText("Bắt đầu hành trình");
            btnStartRide.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"));
        }

        if (btnCallPassenger != null) {
            btnCallPassenger.setOnClickListener(v -> {
                String phone = "0987654321";
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(android.net.Uri.parse("tel:" + phone));
                startActivity(intent);
            });
        }

        if (btnChatPassenger != null) {
            btnChatPassenger.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(DriverActivity.this, ChatActivity.class);
                intent.putExtra("RIDE_ID", ride.getBookingId());
                startActivity(intent);
            });
        }

        if (btnManagePassengers != null) {
            btnManagePassengers.setOnClickListener(v -> showPassengersListDialog(ride.getBookingId()));
        }

        btnEditRide.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(DriverActivity.this, CreateRideActivity.class);
            intent.putExtra("RIDE_ID", ride.getBookingId());
            startActivity(intent);
        });

        btnStartRide.setOnClickListener(v -> {
            dialog.dismiss();
            if (isInProgress) {
                Intent intent = new Intent(DriverActivity.this, DriverLiveMapActivity.class);
                intent.putExtra("RIDE_ID", ride.getBookingId());
                startActivity(intent);
            } else {
                updateRideStatusInFirestore(ride.getBookingId(), "IN_PROGRESS");
                Intent intent = new Intent(DriverActivity.this, DriverLiveMapActivity.class);
                intent.putExtra("RIDE_ID", ride.getBookingId());
                startActivity(intent);
            }
        });

        btnCompleteRide.setOnClickListener(v -> {
            dialog.dismiss();
            updateRideStatusInFirestore(ride.getBookingId(), "COMPLETED");
        });

        btnCancelRide.setOnClickListener(v -> {
            dialog.dismiss();
            updateRideStatusInFirestore(ride.getBookingId(), "CANCELLED");
        });

        dialog.show();
    }

    private void showPassengersListDialog(String rideId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_driver_passengers, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        RecyclerView rv = view.findViewById(R.id.rvDialogPassengers);
        Button btnClose = view.findViewById(R.id.btnCloseDialog);

        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Booking> list = new ArrayList<>();
        DriverPassengerAdapter adapter = new DriverPassengerAdapter(list, new DriverPassengerAdapter.OnPassengerActionListener() {
            @Override
            public void onAccept(String bookingId) {
                updateBookingStatus(bookingId, "CONFIRMED", dialog, rideId);
            }

            @Override
            public void onReject(String bookingId) {
                updateBookingStatus(bookingId, "REJECTED", dialog, rideId);
            }
        });
        rv.setAdapter(adapter);

        db.collection("bookings")
                .whereEqualTo("rideId", rideId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    list.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) {
                            if (b.getBookingId() == null) b.setBookingId(doc.getId());
                            list.add(b);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateBookingStatus(String bookingId, String status, AlertDialog parentDialog, String rideId) {
        db.collection("bookings").document(bookingId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật trạng thái đơn!", Toast.LENGTH_SHORT).show();
                    parentDialog.dismiss();
                    showPassengersListDialog(rideId);
                });
    }

    private void updateRideStatusInFirestore(String rideId, String newStatus) {
        db.collection("rides").document(rideId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if ("COMPLETED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
                        db.collection("bookings")
                                .whereEqualTo("rideId", rideId)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                        db.collection("bookings").document(doc.getId())
                                                .update("status", "COMPLETED".equals(newStatus) ? "COMPLETED" : "CANCELLED");
                                    }
                                });
                        stopSendingDriverLocation();
                    }

                    String msg = " Đã cập nhật trạng thái chuyến đi!";
                    if ("IN_PROGRESS".equals(newStatus)) {
                        msg = "Chuyến đi đang diễn ra!";
                    } else if ("COMPLETED".equals(newStatus)) {
                        msg = "Chuyến đi đã hoàn thành!";
                    } else if ("CANCELLED".equals(newStatus)) {
                        msg = "Đã hủy chuyến đi!";
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    fetchDriverRidesFromFirestore();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void stopSendingDriverLocation() {
        if (driverLocationClient != null && driverLocationCallback != null) {
            driverLocationClient.removeLocationUpdates(driverLocationCallback);
            trackingRideId = null;
        }
    }

    private void performLogout() {
        stopSendingDriverLocation();
        SharedPrefManager.logoutUser(this);
    }
}