package com.example.carpooling.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.models.Booking;

import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.HistoryViewHolder> {

    private List<Booking> historyList;
    private OnRateClickListener rateClickListener;

    public interface OnRateClickListener {
        void onRateClick(Booking booking);
    }

    public BookingHistoryAdapter(List<Booking> historyList, OnRateClickListener listener) {
        this.historyList = historyList;
        this.rateClickListener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Booking booking = historyList.get(position);

        holder.tvBookingId.setText("Mã đơn: #" + booking.getBookingId());

        String rawStatus = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "PENDING";
        String displayStatus = rawStatus;
        int statusColor = android.graphics.Color.parseColor("#757575");

        switch (rawStatus) {
            case "PENDING":
                displayStatus = "ĐANG CHỜ DUYỆT";
                statusColor = android.graphics.Color.parseColor("#FF9800");
                break;
            case "CONFIRMED":
                displayStatus = "ĐÃ XÁC NHẬN";
                statusColor = android.graphics.Color.parseColor("#2196F3");
                break;
            case "COMPLETED":
                displayStatus = "ĐÃ HOÀN THÀNH";
                statusColor = android.graphics.Color.parseColor("#4CAF50");
                break;
            case "CANCELLED":
            case "REJECTED":
                displayStatus = "ĐÃ HỦY";
                statusColor = android.graphics.Color.parseColor("#F44336");
                break;
        }

        holder.tvStatus.setText("Trạng thái: " + displayStatus);
        holder.tvStatus.setTextColor(statusColor);

        if ("COMPLETED".equalsIgnoreCase(rawStatus) && booking.getRating() == 0) {
            holder.btnRate.setVisibility(View.VISIBLE);
            holder.btnRate.setText("⭐ Đánh giá chuyến đi");
            holder.btnRate.setEnabled(true);
            holder.btnRate.setOnClickListener(v -> {
                if (rateClickListener != null) {
                    rateClickListener.onRateClick(booking);
                }
            });
        } else if (booking.getRating() > 0) {
            holder.btnRate.setVisibility(View.VISIBLE);
            holder.btnRate.setText("Đã đánh giá: " + booking.getRating() + " ⭐");
            holder.btnRate.setEnabled(false);
        } else {
            holder.btnRate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvStatus;
        Button btnRate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnRate = itemView.findViewById(R.id.btnRate);
        }
    }
}