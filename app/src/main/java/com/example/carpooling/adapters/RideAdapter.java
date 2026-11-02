package com.example.carpooling.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.activities.MapActivity;
import com.example.carpooling.models.Ride;

import java.util.ArrayList;
import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private Context context;
    private List<Ride> rideList;
    private OnBookClickListener listener;

    private List<String> bookedRideIds = new ArrayList<>();
    private boolean hasActiveRide = false;

    public interface OnBookClickListener {
        void OnBookClick(Ride ride);
    }

    public RideAdapter(Context context, List<Ride> rideList, OnBookClickListener listener) {
        this.context = context;
        this.rideList = rideList;
        this.listener = listener;
    }

    public void setBookedRideIds(List<String> bookedRideIds) {
        this.bookedRideIds = bookedRideIds != null ? bookedRideIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setHasActiveRide(boolean hasActiveRide) {
        this.hasActiveRide = hasActiveRide;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ride, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        Ride ride = rideList.get(position);

        holder.tvRoute.setText(ride.getOrigin() + " ➔ " + ride.getDestination());
        holder.tvDepartureTime.setText("Khởi hành: " + ride.getDepartureTime());
        holder.tvAvailableSeats.setText("Chỗ còn: " + ride.getAvailableSeats());
        String formattedPrice = String.format("%,.0f VNĐ", ride.getPrice());
        holder.tvPrice.setText(formattedPrice);

        boolean isBooked = bookedRideIds.contains(ride.getRideId());

        if (isBooked) {
            holder.btnBookRide.setText("Đã đặt chỗ");
            holder.btnBookRide.setEnabled(false);
            holder.btnBookRide.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.btnBookRide.setOnClickListener(null);
        } else if (hasActiveRide) {
            holder.btnBookRide.setText("Có chuyến đi khác đang hiện hành");
            holder.btnBookRide.setEnabled(false);
            holder.btnBookRide.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.btnBookRide.setOnClickListener(null);
        } else {
            holder.btnBookRide.setText("Đặt chỗ");
            holder.btnBookRide.setEnabled(true);
            holder.btnBookRide.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));

            holder.btnBookRide.setOnClickListener(v -> {
                Intent intent = new Intent(context, MapActivity.class);
                intent.putExtra("SELECTED_RIDE_ID", ride.getRideId());
                intent.putExtra("USER_ROLE", "PASSENGER");
                context.startActivity(intent);

                if (listener != null) {
                    listener.OnBookClick(ride);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return rideList != null ? rideList.size() : 0;
    }

    public static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvDepartureTime, tvAvailableSeats, tvPrice;
        Button btnBookRide;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvAvailableSeats = itemView.findViewById(R.id.tvAvailableSeats);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnBookRide = itemView.findViewById(R.id.btnBookRide);
        }
    }
}