package com.example.carpooling.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.models.Ride;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminRideModerationActivity extends AppCompatActivity {

    private RecyclerView rvAdminRides;
    private Button btnBack;
    private FirebaseFirestore db;
    private List<Ride> rideList = new ArrayList<>();
    private RideManagementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ride_moderation);

        db = FirebaseFirestore.getInstance();
        rvAdminRides = findViewById(R.id.rvAdminRides);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvAdminRides.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RideManagementAdapter(rideList);
        rvAdminRides.setAdapter(adapter);

        fetchAllSystemRides();
    }

    private void fetchAllSystemRides() {
        db.collection("rides").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rideList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Ride ride = doc.toObject(Ride.class);
                        if (ride != null) {
                            if (ride.getRideId() == null || ride.getRideId().isEmpty()) {
                                ride.setRideId(doc.getId());
                            }
                            rideList.add(ride);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh sách chuyến đi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void showRideActionDialog(Ride ride, int position) {
        CharSequence[] options = {
                "Hủy chuyến đi này (Khẩn cấp)",
                "ℹXem chi tiết thông tin chuyến đi"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quản lý chuyến đi: #" + ride.getRideId());
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Xác nhận hủy chuyến")
                        .setMessage("Bạn có chắc chắn muốn hủy chuyến đi này trên hệ thống?")
                        .setPositiveButton("Đồng ý hủy", (d, w) -> {
                            db.collection("rides").document(ride.getRideId())
                                    .update("status", "CANCELLED")
                                    .addOnSuccessListener(aVoid -> {
                                        ride.setStatus("CANCELLED");
                                        adapter.notifyItemChanged(position);

                                        Map<String, Object> logData = new HashMap<>();
                                        logData.put("adminId", "quangminh867@gmail.com");
                                        logData.put("action", "HỦY CHUYẾN ĐI KHẨN CẤP");
                                        logData.put("target", "Ride ID: " + ride.getRideId() + " (Driver: " + ride.getDriverId() + ")");
                                        logData.put("timestamp", FieldValue.serverTimestamp());
                                        db.collection("system_logs").add(logData);

                                        Toast.makeText(this, "Đã hủy chuyến đi và ghi nhận nhật ký!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Đóng", null)
                        .show();
            } else if (which == 1) {
                String details = "Mã chuyến: " + ride.getRideId() + "\n"
                        + "Điểm đi: " + ride.getOrigin() + "\n"
                        + "Điểm đến: " + ride.getDestination() + "\n"
                        + "Ghế trống: " + ride.getAvailableSeats() + "\n"
                        + "Trạng thái: " + ride.getStatus();

                new AlertDialog.Builder(this)
                        .setTitle("Chi Tiết Chuyến Đi")
                        .setMessage(details)
                        .setPositiveButton("Đóng", null)
                        .show();
            }
        });
        builder.show();
    }

    private class RideManagementAdapter extends RecyclerView.Adapter<RideManagementAdapter.ViewHolder> {
        private List<Ride> list;

        public RideManagementAdapter(List<Ride> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_ride, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Ride ride = list.get(position);
            holder.tvRideRoute.setText((ride.getOrigin() != null ? ride.getOrigin() : "Điểm đi") + " ➔ " + (ride.getDestination() != null ? ride.getDestination() : "Điểm đến"));
            holder.tvDriverInfo.setText("Mã tài xế: " + (ride.getDriverId() != null ? ride.getDriverId() : "Không rõ"));

            String status = ride.getStatus() != null ? ride.getStatus() : "AVAILABLE";
            holder.tvRideStatus.setText("Trạng thái: " + status);

            holder.itemView.setOnClickListener(v -> showRideActionDialog(ride, holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRideRoute, tvDriverInfo, tvRideStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRideRoute = itemView.findViewById(R.id.tvRideRoute);
                tvDriverInfo = itemView.findViewById(R.id.tvDriverInfo);
                tvRideStatus = itemView.findViewById(R.id.tvRideStatus);
            }
        }
    }
}