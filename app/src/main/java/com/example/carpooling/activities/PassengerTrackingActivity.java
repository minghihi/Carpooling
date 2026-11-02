package com.example.carpooling.activities;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpooling.R;
import com.example.carpooling.models.Ride;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class PassengerTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String rideId;
    private ListenerRegistration rideListener;
    private Marker driverMarker;
    private Button btnExitTracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_tracking);

        db = FirebaseFirestore.getInstance();
        rideId = getIntent().getStringExtra("RIDE_ID");

        btnExitTracking = findViewById(R.id.btnExitTracking);
        btnExitTracking.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapPassengerTracking);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        startTrackingDriverLocation();
    }

    private void startTrackingDriverLocation() {
        if (rideId == null || rideId.isEmpty()) return;

        rideListener = db.collection("rides").document(rideId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    Ride ride = doc.toObject(Ride.class);
                    if (ride != null) {
                        LatLng driverLatLng = new LatLng(ride.getLatitude(), ride.getLongitude());
                        if (mMap != null) {
                            if (driverMarker == null) {
                                driverMarker = mMap.addMarker(new MarkerOptions()
                                        .position(driverLatLng)
                                        .title("Vị trí tài xế"));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLatLng, 16f));
                            } else {
                                driverMarker.setPosition(driverLatLng);
                                mMap.animateCamera(CameraUpdateFactory.newLatLng(driverLatLng));
                            }
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rideListener != null) {
            rideListener.remove();
        }
    }
}