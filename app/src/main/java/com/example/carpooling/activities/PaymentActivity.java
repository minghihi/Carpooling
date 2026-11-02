package com.example.carpooling.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.carpooling.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    private RadioGroup rgPaymentMethod;
    private AppCompatButton btnConfirmPayment;
    private TextView tvRideDetails, tvAmount;

    private String rideId;
    private double amount = 0.0;
    private String origin = "", destination = "";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        try {
            rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
            btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
            tvRideDetails = findViewById(R.id.tvRideDetails);
            tvAmount = findViewById(R.id.tvAmount);

            if (getIntent() != null) {
                rideId = getIntent().getStringExtra("RIDE_ID");
                amount = getIntent().getDoubleExtra("AMOUNT", 0.0);
                origin = getIntent().getStringExtra("ORIGIN");
                destination = getIntent().getStringExtra("DESTINATION");
            }

            if (tvRideDetails != null) {
                String routeText = "Lộ trình: " + (origin != null ? origin : "Điểm đi") + " ➔ " + (destination != null ? destination : "Điểm đến");
                tvRideDetails.setText(routeText);
            }

            if (tvAmount != null) {
                tvAmount.setText("Tổng cước phí: " + String.format("%,.0f", amount) + " VNĐ");
            }

            if (btnConfirmPayment != null) {
                btnConfirmPayment.setOnClickListener(v -> {
                    int selectedId = rgPaymentMethod.getCheckedRadioButtonId();

                    if (selectedId == R.id.rbCash) {
                        executeFinalBookingAndPayment("CASH");
                    } else if (selectedId == R.id.rbOnline) {
                        Intent intent = new Intent(PaymentActivity.this, WalletPaymentActivity.class);
                        intent.putExtra("RIDE_ID", rideId);
                        intent.putExtra("AMOUNT", amount);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Vui lòng chọn phương thức thanh toán!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi tạo: " + e.getMessage(), e);
        }
    }

    private void executeFinalBookingAndPayment(String paymentMethod) {
        if (rideId == null || rideId.isEmpty()) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        btnConfirmPayment.setEnabled(false);
        Toast.makeText(this, "Đang xử lý...", Toast.LENGTH_SHORT).show();

        DocumentReference rideRef = db.collection("rides").document(rideId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(rideRef);
            Long currentSeats = snapshot.getLong("availableSeats");

            if (currentSeats == null || currentSeats <= 0) {
                throw new FirebaseFirestoreException("Hết ghế!", FirebaseFirestoreException.Code.ABORTED);
            }

            long newSeats = currentSeats - 1;
            transaction.update(rideRef, "availableSeats", newSeats);
            if (newSeats == 0) transaction.update(rideRef, "status", "FULL");

            DocumentReference newBookingRef = db.collection("bookings").document();
            Map<String, Object> newBooking = new HashMap<>();
            newBooking.put("bookingId", newBookingRef.getId());
            newBooking.put("rideId", rideId);
            newBooking.put("passengerUid", currentUser.getUid());
            newBooking.put("passengerEmail", currentUser.getEmail() != null ? currentUser.getEmail() : "");
            newBooking.put("paymentMethod", paymentMethod);
            newBooking.put("amount", amount);
            newBooking.put("status", "PENDING");

            transaction.set(newBookingRef, newBooking);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đặt ghế thành công!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(PaymentActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            btnConfirmPayment.setEnabled(true);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}