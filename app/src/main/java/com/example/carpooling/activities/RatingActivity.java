package com.example.carpooling;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RatingActivity extends AppCompatActivity {

    private TextView tvRideInfo;
    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmitRating;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String rideId;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvRideInfo = findViewById(R.id.tvRideInfo);
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);

        rideId = getIntent().getStringExtra("RIDE_ID");
        driverId = getIntent().getStringExtra("DRIVER_ID");
        String origin = getIntent().getStringExtra("ORIGIN");
        String destination = getIntent().getStringExtra("DESTINATION");

        if (origin != null && destination != null) {
            tvRideInfo.setText("Đánh giá chuyến đi từ:\n" + origin + " -> " + destination);
        } else {
            tvRideInfo.setText("Mã chuyến đi: " + (rideId != null ? rideId : "N/A"));
        }

        btnSubmitRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });
    }

    private void submitReview() {
        float ratingValue = ratingBar.getRating();
        String commentText = etComment.getText().toString().trim();

        if (ratingValue == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá tài xế!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rideId == null || driverId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin chuyến đi!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitRating.setEnabled(false);
        Toast.makeText(this, "Đang gửi đánh giá...", Toast.LENGTH_SHORT).show();

        db.collection("rides").document(rideId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");

                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            saveRatingToFirestore(ratingValue, commentText);
                        } else {
                            btnSubmitRating.setEnabled(true);
                            Toast.makeText(this, "Chuyến đi chưa hoàn thành, không thể đánh giá!", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        btnSubmitRating.setEnabled(true);
                        Toast.makeText(this, "Không tìm thấy dữ liệu chuyến đi!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSubmitRating.setEnabled(true);
                    Toast.makeText(this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveRatingToFirestore(float rating, String comment) {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("rideId", rideId);
        reviewData.put("passengerId", currentUserId);
        reviewData.put("driverId", driverId);
        reviewData.put("rating", (int) rating);
        reviewData.put("comment", comment);
        reviewData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("ratings").add(reviewData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(RatingActivity.this, "Gửi đánh giá thành công. Xin cảm ơn bạn!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitRating.setEnabled(true);
                    Toast.makeText(RatingActivity.this, "Gửi đánh giá thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}