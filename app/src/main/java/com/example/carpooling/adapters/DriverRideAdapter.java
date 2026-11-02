package com.example.carpooling.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.activities.CurrentRideActivity;
import com.example.carpooling.models.Ride;

import java.util.List;

public class DriverRideAdapter extends RecyclerView.Adapter<DriverRideAdapter.DriverRideViewHolder> {

    private Context context;
    private List<Ride> rideList;

    public DriverRideAdapter(Context context, List<Ride> rideList) {
        this.context = context;
        this.rideList = rideList;
    }

    @NonNull
    @Override
    public DriverRideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.item_driver_ride, parent, false);
        return new DriverRideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverRideViewHolder holder, int position) {
        Ride ride = rideList.get(position);

        holder.tvRoute.setText(ride.getOrigin() + " ➔ " + ride.getDestination());
        holder.tvDepartureTime.setText("Khởi hành: " + ride.getDepartureTime());
        holder.tvAvailableSeats.setText("Chỗ còn: " + ride.getAvailableSeats());
        holder.tvPrice.setText(ride.getPrice() + " VNĐ");

        String status = ride.getStatus() != null ? ride.getStatus().trim().toUpperCase() : "";
        if (status.equals("FULL")) {
            holder.btnBookRide.setText("🚗 Vào phòng Gọi/Chat");
            holder.btnBookRide.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.btnBookRide.setEnabled(true);

            holder.btnBookRide.setOnClickListener(v -> {
                Intent intent = new Intent(context, CurrentRideActivity.class);
                intent.putExtra("RIDE_ID", ride.getRideId());
                context.startActivity(intent);
            });
        } else {
            holder.btnBookRide.setText("Đang chờ khách...");
            holder.btnBookRide.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return rideList != null ? rideList.size() : 0;
    }

    public static class DriverRideViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvDepartureTime, tvAvailableSeats, tvPrice;
        Button btnBookRide;

        public DriverRideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvAvailableSeats = itemView.findViewById(R.id.tvAvailableSeats);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnBookRide = itemView.findViewById(R.id.btnBookRide);
        }
    }
}