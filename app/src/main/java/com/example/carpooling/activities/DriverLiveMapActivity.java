package com.example.carpooling.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.adapters.DriverPassengerAdapter;
import com.example.carpooling.models.Booking;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverLiveMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "DriverLiveMapActivity";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private FirebaseFirestore db;
    private String rideId;

    private Button btnBackHome;
    private RecyclerView rvPassengers;
    private DriverPassengerAdapter passengerAdapter;
    private List<Booking> passengerList = new ArrayList<>();

    private Marker driverMarker;
    private ListenerRegistration passengerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_live_map);

        rideId = getIntent().getStringExtra("RIDE_ID");
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnBackHome = findViewById(R.id.btnBackHome);
        rvPassengers = findViewById(R.id.rvPassengers);

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(DriverLiveMapActivity.this, DriverActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        setupRecyclerView();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupRecyclerView() {
        rvPassengers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        passengerAdapter = new DriverPassengerAdapter(passengerList, new DriverPassengerAdapter.OnPassengerActionListener() {
            @Override
            public void onAccept(String bookingId) {
                updateBookingStatus(bookingId, "CONFIRMED");
            }

            @Override
            public void onReject(String bookingId) {
                updateBookingStatus(bookingId, "REJECTED");
            }
        });

        rvPassengers.setAdapter(passengerAdapter);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        startLocationTrackingAndSending();
        listenToPassengersRealtime();
    }

    private void startLocationTrackingAndSending() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        mMap.setMyLocationEnabled(true);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000); // 4 giây cập nhật một lần
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                        if (driverMarker == null) {
                            driverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .title("Vị trí xe của bạn"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                        } else {
                            driverMarker.setPosition(currentLatLng);
                        }

                        if (rideId != null && !rideId.isEmpty()) {
                            Map<String, Object> locMap = new HashMap<>();
                            locMap.put("latitude", currentLatLng.latitude);
                            locMap.put("longitude", currentLatLng.longitude);

                            db.collection("rides").document(rideId).update(locMap);
                        }
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void listenToPassengersRealtime() {
        if (rideId == null) return;

        passengerListener = db.collection("bookings")
                .whereEqualTo("rideId", rideId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe hành khách: ", error);
                        return;
                    }

                    if (snapshots != null) {
                        passengerList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                if (booking.getBookingId() == null || booking.getBookingId().isEmpty()) {
                                    booking.setBookingId(doc.getId());
                                }
                                passengerList.add(booking);
                            }
                        }
                        passengerAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void updateBookingStatus(String bookingId, String status) {
        db.collection("bookings").document(bookingId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    String msg = status.equals("CONFIRMED") ? "Đã duyệt hành khách!" : "Đã từ chối hành khách!";
                    Toast.makeText(DriverLiveMapActivity.this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(DriverLiveMapActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (passengerListener != null) {
            passengerListener.remove();
        }
    }
}