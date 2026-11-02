package com.example.carpooling.adapters;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class DriverPassengerAdapter extends RecyclerView.Adapter<DriverPassengerAdapter.PassengerViewHolder> {

    private List<Booking> passengerList;
    private OnPassengerActionListener actionListener;

    public interface OnPassengerActionListener {
        void onAccept(String bookingId);
        void onReject(String bookingId);
    }

    public DriverPassengerAdapter(List<Booking> passengerList, OnPassengerActionListener actionListener) {
        this.passengerList = passengerList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public PassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver_passenger, parent, false);
        return new PassengerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PassengerViewHolder holder, int position) {
        Booking booking = passengerList.get(position);

        String status = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "PENDING";

        boolean isConfirmed = "CONFIRMED".equals(status) || "ACCEPTED".equals(status) || "APPROVED".equals(status);

        holder.tvPassengerName.setText("👤 Đang tải...");
        fetchPassengerName(booking.getPassengerUid(), holder.tvPassengerName);

        holder.tvBookingStatus.setText(status);

        if (isConfirmed) {
            holder.tvBookedSeats.setVisibility(View.GONE);
            holder.tvBookingStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);

            String passengerId = booking.getPassengerUid();

            holder.itemView.setOnClickListener(v -> showPassengerDetailDialog(v.getContext(), passengerId, booking.getPassengerEmail()));
        } else {
            holder.tvBookedSeats.setVisibility(View.VISIBLE);
            holder.tvBookingStatus.setTextColor(Color.parseColor("#FF9800"));
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(null);
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (actionListener != null && booking.getBookingId() != null) {
                actionListener.onAccept(booking.getBookingId());
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (actionListener != null && booking.getBookingId() != null) {
                actionListener.onReject(booking.getBookingId());
            }
        });
    }

    private void fetchPassengerName(String uid, TextView tvName) {
        if (uid == null || uid.isEmpty()) {
            tvName.setText("👤 Không xác định");
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        tvName.setText("👤 " + doc.getString("name"));
                    } else {
                        tvName.setText("👤 Người dùng");
                    }
                })
                .addOnFailureListener(e -> tvName.setText("👤 Người dùng"));
    }

    private void showPassengerDetailDialog(android.content.Context context, String passengerId, String defaultEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_passenger_detail, null);
        builder.setView(view);

        TextView tvName = view.findViewById(R.id.tvDetailName);
        TextView tvEmail = view.findViewById(R.id.tvDetailEmail);
        TextView tvPhone = view.findViewById(R.id.tvDetailPhone);
        Button btnClose = view.findViewById(R.id.btnCloseDetail);

        AlertDialog dialog = builder.create();

        if (passengerId != null && !passengerId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(passengerId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            tvName.setText("Tên: " + documentSnapshot.getString("name"));
                            tvEmail.setText("Email: " + documentSnapshot.getString("email"));
                            tvPhone.setText("SĐT: " + documentSnapshot.getString("phone"));
                        } else {
                            tvName.setText("Tên: Không rõ");
                            tvEmail.setText("Email: " + (defaultEmail != null ? defaultEmail : "Không rõ"));
                            tvPhone.setText("SĐT: Chưa cập nhật");
                        }
                    });
        } else {
            tvName.setText("Tên: Không rõ");
            tvEmail.setText("Email: " + (defaultEmail != null ? defaultEmail : "Không rõ"));
            tvPhone.setText("SĐT: Chưa cập nhật");
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return passengerList != null ? passengerList.size() : 0;
    }

    public static class PassengerViewHolder extends RecyclerView.ViewHolder {
        TextView tvPassengerName, tvBookedSeats, tvBookingStatus;
        Button btnAccept, btnReject;

        public PassengerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerName = itemView.findViewById(R.id.tvPassengerName);
            tvBookedSeats = itemView.findViewById(R.id.tvBookedSeats);
            tvBookingStatus = itemView.findViewById(R.id.tvBookingStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}