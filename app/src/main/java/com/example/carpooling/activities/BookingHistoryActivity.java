package com.example.carpooling.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.adapters.BookingHistoryAdapter;
import com.example.carpooling.utils.BottomNavHelper;
import com.example.carpooling.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class BookingHistoryActivity extends AppCompatActivity {

    private static final String TAG = "BookingHistoryActivity";
    private RecyclerView rvBookingHistory;
    private BookingHistoryAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userRole = "PASSENGER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getIntent() != null && getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        initViews();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookingHistory();
    }

    private void initViews() {
        rvBookingHistory = findViewById(R.id.rvBookingHistory);
        rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingHistoryAdapter(bookingList, booking -> {
            if (!"DRIVER".equals(userRole)) {
                if ("PENDING".equalsIgnoreCase(booking.getStatus()) || "CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                    showCancelBookingDialog(booking);
                } else {
                    showRatingDialog(booking);
                }
            }
        });
        rvBookingHistory.setAdapter(adapter);
    }

    private void loadBookingHistory() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUid = mAuth.getCurrentUser().getUid();

        if ("DRIVER".equals(userRole)) {
            // Truy vấn collection "rides" cho tài xế và ánh xạ dữ liệu tương ứng
            db.collection("rides")
                    .whereEqualTo("driverId", currentUid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        bookingList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Booking booking = new Booking();
                            booking.setBookingId(doc.getId());
                            booking.setRideId(doc.getId());

                            String status = doc.getString("status");
                            if (status == null) status = "COMPLETED";
                            booking.setStatus(status);

                            // Lọc các trạng thái hiển thị lịch sử cho tài xế
                            if ("COMPLETED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status) || "FULL".equalsIgnoreCase(status)) {
                                bookingList.add(booking);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (bookingList.isEmpty()) {
                            Toast.makeText(this, "Bạn chưa có lịch sử chuyến đi nào", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi tải lịch sử tài xế: " + e.getMessage());
                        Toast.makeText(this, "Không thể tải lịch sử chuyến đi", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Giữ nguyên phần dành cho Hành khách
            db.collection("bookings")
                    .whereEqualTo("passengerUid", currentUid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        bookingList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                if (booking.getBookingId() == null || booking.getBookingId().isEmpty()) {
                                    booking.setBookingId(doc.getId());
                                }
                                bookingList.add(booking);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (bookingList.isEmpty()) {
                            Toast.makeText(this, "Bạn chưa có lịch sử đặt chỗ nào", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi tải lịch sử hành khách: " + e.getMessage());
                        Toast.makeText(this, "Không thể tải lịch sử đặt chỗ", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showCancelBookingDialog(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đặt chỗ")
                .setMessage("Bạn có chắc chắn muốn hủy đơn đặt chỗ này không? Số ghế sẽ được hoàn trả.")
                .setPositiveButton("Đồng ý hủy", (dialog, which) -> {
                    cancelBookingAndRestoreSeats(booking.getBookingId(), booking.getRideId(), booking.getBookedSeats());
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void cancelBookingAndRestoreSeats(String bookingId, String rideId, int bookedSeats) {
        if (bookingId == null || bookingId.isEmpty() || rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Lỗi thông tin đơn đặt chỗ!", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference bookingRef = db.collection("bookings").document(bookingId);
        DocumentReference rideRef = db.collection("rides").document(rideId);

        db.runTransaction(transaction -> {
            DocumentSnapshot rideSnapshot = transaction.get(rideRef);
            Long currentSeats = rideSnapshot.getLong("availableSeats");
            if (currentSeats == null) currentSeats = 0L;
            transaction.update(bookingRef, "status", "CANCELLED");
            transaction.update(rideRef, "availableSeats", currentSeats + bookedSeats);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Hủy đặt chỗ thành công! Đã hoàn lại ghế.", Toast.LENGTH_LONG).show();
            loadBookingHistory();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi hủy đơn: " + e.getMessage());
            Toast.makeText(this, "Hủy thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showRatingDialog(Booking booking) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        builder.setView(dialogView);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etReview = dialogView.findViewById(R.id.etReview);
        Button btnSubmitRating = dialogView.findViewById(R.id.btnSubmitRating);

        AlertDialog dialog = builder.create();

        btnSubmitRating.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String reviewText = etReview.getText().toString().trim();

            btnSubmitRating.setEnabled(false);

            db.collection("bookings").document(booking.getBookingId())
                    .update("rating", rating, "review", reviewText, "status", "COMPLETED")
                    .addOnSuccessListener(aVoid -> {

                        if (booking.getRideId() != null && !booking.getRideId().isEmpty()) {
                            db.collection("rides").document(booking.getRideId())
                                    .update("status", "COMPLETED");

                            db.collection("rides").document(booking.getRideId()).get()
                                    .addOnSuccessListener(rideDoc -> {
                                        if (rideDoc.exists()) {
                                            String driverId = rideDoc.getString("driverId");
                                            if (driverId != null && !driverId.isEmpty()) {
                                                Map<String, Object> ratingData = new HashMap<>();
                                                ratingData.put("driverId", driverId);
                                                ratingData.put("passengerUid", mAuth.getCurrentUser().getUid());
                                                ratingData.put("rating", (double) rating);
                                                ratingData.put("review", reviewText);
                                                ratingData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                                                db.collection("ratings").add(ratingData);
                                            }
                                        }
                                    });
                        }

                        Toast.makeText(this, "Cảm ơn bạn đã đánh giá! Chuyến đi đã hoàn thành.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        loadBookingHistory();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmitRating.setEnabled(true);
                        Toast.makeText(this, "Lỗi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void setupBottomNavigation() {
        BottomNavHelper.setupNav(this, userRole, 2);
    }
}