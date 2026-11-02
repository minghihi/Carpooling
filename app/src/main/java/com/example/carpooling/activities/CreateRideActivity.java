package com.example.carpooling.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.carpooling.R;
import com.example.carpooling.models.Ride;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class CreateRideActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private TextView txtOrigin, txtDestination;
    private Button btnPickOrigin, btnPickDestination;
    private EditText etDepartureTime, etSeats;
    private Button btnSubmitRide;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String editRideId = null;

    private double originLat = 0.0, originLng = 0.0;
    private double destLat = 0.0, destLng = 0.0;
    private String originAddress = "";
    private String destAddress = "";

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<Intent> pickOriginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    originLat = result.getData().getDoubleExtra("LAT", 0);
                    originLng = result.getData().getDoubleExtra("LNG", 0);
                    originAddress = result.getData().getStringExtra("ADDRESS");
                    txtOrigin.setText(originAddress);
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickDestLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    destLat = result.getData().getDoubleExtra("LAT", 0);
                    destLng = result.getData().getDoubleExtra("LNG", 0);
                    destAddress = result.getData().getStringExtra("ADDRESS");
                    txtDestination.setText(destAddress);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ride);

        txtOrigin = findViewById(R.id.txtOrigin);
        txtDestination = findViewById(R.id.txtDestination);
        btnPickOrigin = findViewById(R.id.btnPickOrigin);
        btnPickDestination = findViewById(R.id.btnPickDestination);
        etDepartureTime = findViewById(R.id.etDepartureTime);
        etSeats = findViewById(R.id.etSeats);
        btnSubmitRide = findViewById(R.id.btnSubmitRide);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (getIntent() == null || !getIntent().hasExtra("RIDE_ID")) {
            getCurrentLocationAsDefaultOrigin();
        }

        btnPickOrigin.setOnClickListener(v -> {
            Intent intent = new Intent(CreateRideActivity.this, PickLocationActivity.class);
            pickOriginLauncher.launch(intent);
        });

        btnPickDestination.setOnClickListener(v -> {
            Intent intent = new Intent(CreateRideActivity.this, PickLocationActivity.class);
            pickDestLauncher.launch(intent);
        });

        if (getIntent() != null && getIntent().hasExtra("RIDE_ID")) {
            editRideId = getIntent().getStringExtra("RIDE_ID");
            if (editRideId != null && !editRideId.isEmpty()) {
                btnSubmitRide.setText("Cập nhật chuyến đi");
                loadExistingRideData(editRideId);
            }
        }

        etDepartureTime.setOnClickListener(v -> showDateTimePicker());
        btnSubmitRide.setOnClickListener(v -> handleSaveOrUpdateRide());
    }

    private void getCurrentLocationAsDefaultOrigin() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && originAddress.isEmpty()) {
                originLat = location.getLatitude();
                originLng = location.getLongitude();
                originAddress = "Vị trí hiện tại của tôi (GPS)";
                txtOrigin.setText(originAddress);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAsDefaultOrigin();
            } else {
                Toast.makeText(this, "Cần cấp quyền vị trí để lấy tọa độ thực tế!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                String formattedTime = String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d:00",
                        year, month + 1, dayOfMonth, hourOfDay, minute);
                etDepartureTime.setText(formattedTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadExistingRideData(String rideId) {
        db.collection("rides").document(rideId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Ride ride = documentSnapshot.toObject(Ride.class);
                        if (ride != null) {
                            originAddress = ride.getOrigin();
                            destAddress = ride.getDestination();
                            originLat = ride.getLatitude();
                            originLng = ride.getLongitude();

                            txtOrigin.setText(originAddress);
                            txtDestination.setText(destAddress);
                            etDepartureTime.setText(ride.getDepartureTime());
                            etSeats.setText(String.valueOf(ride.getAvailableSeats()));
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải thông tin chuyến đi!", Toast.LENGTH_SHORT).show());
    }

    private void handleSaveOrUpdateRide() {
        String departureTime = etDepartureTime.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();

        if (originAddress.isEmpty() || destAddress.isEmpty() || departureTime.isEmpty() || seatsStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ điểm đi, điểm đến và thời gian!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            return;
        }

        String driverId = currentUser.getUid();
        int seats = Integer.parseInt(seatsStr);
        double defaultPrice = 0.0;

        btnSubmitRide.setEnabled(false);

        if (editRideId == null) {
            String rideId = db.collection("rides").document().getId();
            Ride newRide = new Ride(
                    rideId,
                    driverId,
                    originAddress,
                    destAddress,
                    departureTime,
                    defaultPrice,
                    seats,
                    originLat,
                    originLng,
                    "AVAILABLE"
            );

            db.collection("rides").document(rideId)
                    .set(newRide)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateRideActivity.this, "Đăng chuyến thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmitRide.setEnabled(true);
                        Toast.makeText(CreateRideActivity.this, "Lỗi đăng chuyến: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            java.util.Map<String, Object> updateMap = new java.util.HashMap<>();
            updateMap.put("origin", originAddress);
            updateMap.put("destination", destAddress);
            updateMap.put("latitude", originLat);
            updateMap.put("longitude", originLng);
            updateMap.put("departureTime", departureTime);
            updateMap.put("availableSeats", seats);

            db.collection("rides").document(editRideId)
                    .update(updateMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateRideActivity.this, "Cập nhật chuyến đi thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmitRide.setEnabled(true);
                        Toast.makeText(CreateRideActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}