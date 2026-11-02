package com.example.carpooling.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.carpooling.R;
import com.example.carpooling.models.Ride;
import com.example.carpooling.utils.FareCalculator;
import com.example.carpooling.utils.MapUtils;
import com.example.carpooling.utils.SharedPrefManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng myCurrentLocation;

    private final LatLng DEFAULT_LOCATION = new LatLng(21.0362, 105.7828);

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPrefManager prefManager;

    private LinearLayout layoutBottomSheet;
    private TextView txtDriverName;
    private TextView txtRideInfo;
    private Button btnBookRide;
    private Button btnOpenList;

    private String selectedRideId = null;
    private String rideIdFromIntent = null;
    private String userRole = "PASSENGER";
    private Marker currentlySelectedMarker = null;

    private final Map<String, Marker> markerMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

        if (getIntent() != null) {
            if (getIntent().hasExtra("USER_ROLE")) {
                userRole = getIntent().getStringExtra("USER_ROLE");
            } else if (prefManager.getUserRole() != null) {
                userRole = prefManager.getUserRole();
            }

            if (getIntent().hasExtra("SELECTED_RIDE_ID")) {
                rideIdFromIntent = getIntent().getStringExtra("SELECTED_RIDE_ID");
            }
        }

        layoutBottomSheet = findViewById(R.id.layoutBottomSheet);
        txtDriverName = findViewById(R.id.txtDriverName);
        txtRideInfo = findViewById(R.id.txtRideInfo);
        btnBookRide = findViewById(R.id.btnBookRide);
        btnOpenList = findViewById(R.id.btnOpenList);

        if (layoutBottomSheet != null) {
            layoutBottomSheet.setVisibility(View.GONE);
        }

        setupRoleBasedUI();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupRoleBasedUI() {
        boolean isDriver = "DRIVER".equalsIgnoreCase(userRole);

        if (btnOpenList != null) {
            btnOpenList.setOnClickListener(v -> {
                Intent intent;
                if (isDriver) {
                    intent = new Intent(MapActivity.this, DriverActivity.class);
                } else {
                    intent = new Intent(MapActivity.this, MainActivity.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (btnBookRide != null) {
            if (isDriver) {
                btnBookRide.setVisibility(View.GONE);
            } else {
                btnBookRide.setVisibility(View.VISIBLE);
                btnBookRide.setOnClickListener(v -> {
                    if (currentlySelectedMarker != null && currentlySelectedMarker.getTag() instanceof Ride) {
                        Ride selectedRide = (Ride) currentlySelectedMarker.getTag();
                        executeBookingTransaction(selectedRide);
                    } else {
                        Toast.makeText(MapActivity.this, "Vui lòng chọn một chuyến xe hợp lệ trên bản đồ!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 13f));
        requestLocationPermission();

        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() instanceof Ride) {
                currentlySelectedMarker = marker;
                Ride ride = (Ride) marker.getTag();
                displayRideDetailsBottomSheet(ride);
            }
            return false;
        });

        mMap.setOnMapClickListener(latLng -> {
            if (layoutBottomSheet != null) {
                layoutBottomSheet.setVisibility(View.GONE);
            }
            currentlySelectedMarker = null;
            selectedRideId = null;
        });
    }

    private void displayRideDetailsBottomSheet(Ride ride) {
        selectedRideId = ride.getRideId();

        if (txtDriverName != null) {
            txtDriverName.setText("Mã chuyến xe: " + ride.getRideId() + " | Tài xế: Đang tải tên...");
        }

        String driverId = ride.getDriverId();
        if (driverId != null && !driverId.isEmpty()) {
            db.collection("users").document(driverId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String driverName = "Không rõ tên";
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                driverName = name;
                            }
                        }
                        if (txtDriverName != null) {
                            txtDriverName.setText("Mã chuyến xe: " + ride.getRideId() + " | Tài xế: " + driverName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (txtDriverName != null) {
                            txtDriverName.setText("Mã chuyến xe: " + ride.getRideId() + " | Tài xế: Không tải được");
                        }
                    });
        }

        String detailInfo = "Điểm đi: " + ride.getOrigin() + "\n"
                + "Điểm đến: " + ride.getDestination() + "\n"
                + "Khởi hành: " + ride.getDepartureTime() + "\n"
                + "Giá vé: " + String.format("%,.0f", ride.getPrice()) + " VNĐ\n"
                + "Ghế trống: " + ride.getAvailableSeats() + " | Trạng thái: " + ride.getStatus();

        if (txtRideInfo != null) {
            txtRideInfo.setText(detailInfo);
        }
        if (layoutBottomSheet != null) {
            layoutBottomSheet.setVisibility(View.VISIBLE);
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);
        getDeviceLocation();
    }

    private void getDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    myCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    if (rideIdFromIntent == null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myCurrentLocation, 15f));
                    }
                } else {
                    myCurrentLocation = DEFAULT_LOCATION;
                    Toast.makeText(MapActivity.this, "Đang định vị GPS, dùng vị trí mặc định tạm thời!", Toast.LENGTH_SHORT).show();
                }

                fetchRidesFromFirestore();
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Lỗi GPS: " + e.getMessage());
            myCurrentLocation = DEFAULT_LOCATION;
            fetchRidesFromFirestore();
        }
    }

    private void fetchRidesFromFirestore() {
        boolean isDriver = "DRIVER".equalsIgnoreCase(userRole);
        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        LatLng centerLocation = (myCurrentLocation != null) ? myCurrentLocation : DEFAULT_LOCATION;

        db.collection("rides")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mMap.clear();
                    markerMap.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        String msg = isDriver ? "Bạn chưa đăng chuyến xe nào trên hệ thống." : "Hiện tại chưa có chuyến xe nào trống chỗ!";
                        Toast.makeText(MapActivity.this, msg, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Pair<Ride, Double>> rideDistanceList = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Ride ride = doc.toObject(Ride.class);
                        if (ride != null) {
                            if (ride.getRideId() == null || ride.getRideId().isEmpty()) {
                                ride.setRideId(doc.getId());
                            }

                            if (isDriver) {
                                if (!currentUid.equalsIgnoreCase(ride.getDriverId())) {
                                    continue;
                                }
                            } else {
                                String status = ride.getStatus() != null ? ride.getStatus().trim().toUpperCase() : "";
                                boolean isAvailable = (status.equals("AVAILABLE") || status.equals("ACTIVE") || status.equals("OPEN"))
                                        && ride.getAvailableSeats() > 0;

                                if (!isAvailable) {
                                    continue;
                                }
                            }

                            double distance = 0.0;
                            if (ride.getLatitude() != 0 && ride.getLongitude() != 0) {
                                distance = MapUtils.calculateDistance(
                                        centerLocation.latitude, centerLocation.longitude,
                                        ride.getLatitude(), ride.getLongitude()
                                );
                            }

                            if (!isDriver && distance > 200.0) {
                                continue;
                            }

                            double calculatedPrice = FareCalculator.calculateRideFare(distance);
                            ride.setPrice(calculatedPrice);

                            rideDistanceList.add(new Pair<>(ride, distance));
                        }
                    }

                    Collections.sort(rideDistanceList, (p1, p2) -> Double.compare(p1.second, p2.second));

                    int count = 0;
                    for (Pair<Ride, Double> pair : rideDistanceList) {
                        Ride ride = pair.first;
                        double distance = pair.second;

                        LatLng vehiclePos;
                        if (ride.getLatitude() != 0 && ride.getLongitude() != 0) {
                            vehiclePos = new LatLng(ride.getLatitude(), ride.getLongitude());
                        } else {
                            vehiclePos = DEFAULT_LOCATION;
                        }

                        String markerTitle = isDriver ? "Chuyến đi của tôi" : ("Cách bạn: " + String.format("%.1f", distance) + " km");

                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(vehiclePos)
                                .title(markerTitle)
                                .snippet("Giá: " + String.format("%,.0f", ride.getPrice()) + "đ | Ghế: " + ride.getAvailableSeats()));

                        if (marker != null) {
                            marker.setTag(ride);
                            markerMap.put(ride.getRideId(), marker);
                            count++;
                        }
                    }

                    if (count == 0) {
                        String msg = isDriver ? "Bạn chưa tạo chuyến xe nào." : "Hiện không có chuyến xe nào trong bán kính 200km!";
                        Toast.makeText(MapActivity.this, msg, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MapActivity.this, "Đã cập nhật " + count + " chuyến xe gần bạn!", Toast.LENGTH_SHORT).show();
                    }

                    if (rideIdFromIntent != null && markerMap.containsKey(rideIdFromIntent)) {
                        Marker targetMarker = markerMap.get(rideIdFromIntent);
                        if (targetMarker != null && targetMarker.getTag() instanceof Ride) {
                            Ride ride = (Ride) targetMarker.getTag();
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetMarker.getPosition(), 15f));
                            targetMarker.showInfoWindow();
                            displayRideDetailsBottomSheet(ride);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MapActivity.this, "Lỗi lấy dữ liệu chuyến xe!", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi Firestore: " + e.getMessage());
                });
    }

    private void executeBookingTransaction(Ride ride) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện đặt ghế!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getUid().equals(ride.getDriverId())) {
            Toast.makeText(this, "Bạn không thể đặt chuyến do chính mình tạo!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String rideId = ride.getRideId();
        final double ridePrice = ride.getPrice();
        final String rideOrigin = ride.getOrigin();
        final String rideDestination = ride.getDestination();

        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Mã chuyến xe không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (layoutBottomSheet != null) {
            layoutBottomSheet.setVisibility(View.GONE);
        }

        try {
            Intent intent = new Intent(MapActivity.this, PaymentActivity.class);
            intent.putExtra("RIDE_ID", rideId);
            intent.putExtra("AMOUNT", ridePrice);
            intent.putExtra("ORIGIN", rideOrigin);
            intent.putExtra("DESTINATION", rideDestination);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi mở màn hình thanh toán: " + e.getMessage(), e);
            Toast.makeText(MapActivity.this, "Không thể mở màn hình thanh toán: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation();
        }
    }
}