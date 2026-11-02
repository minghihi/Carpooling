package com.example.carpooling.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.models.Booking;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookingList;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onAccept(String bookingId);
        void onCancel(String bookingId);
    }

    public BookingAdapter(List<Booking> bookingList, OnBookingActionListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver_ride, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.tvRoute.setText("Tuyến: " + (booking.getOrigin() != null ? booking.getOrigin() : "...") + " ➔ " + (booking.getDestination() != null ? booking.getDestination() : "..."));
        holder.tvDepartureTime.setText("Khởi hành: " + (booking.getDepartureTime() != null ? booking.getDepartureTime() : "Chưa có"));
        holder.tvAvailableSeats.setText("Chỗ còn: " + booking.getAvailableSeats());
        holder.tvPrice.setText(String.format("%,.0f VNĐ", booking.getPrice()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(booking.getBookingId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvDepartureTime, tvAvailableSeats, tvPrice;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvAvailableSeats = itemView.findViewById(R.id.tvAvailableSeats);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}