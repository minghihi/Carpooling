package com.example.carpooling.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.models.SystemLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SystemLogAdapter extends RecyclerView.Adapter<SystemLogAdapter.LogViewHolder> {

    private List<SystemLog> logList;

    public SystemLogAdapter(List<SystemLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_system_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        SystemLog log = logList.get(position);

        holder.tvAction.setText("Hành động: " + log.getAction());
        holder.tvTarget.setText("Mã đối tượng tác động: " + log.getTarget());
        holder.tvAdminId.setText("Admin ID: " + log.getAdminId());

        if (log.getTimestamp() != null) {
            Date date = log.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            holder.tvTimestamp.setText(sdf.format(date));
        } else {
            holder.tvTimestamp.setText("Vừa xong");
        }
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvAction, tvTarget, tvAdminId, tvTimestamp;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAction = itemView.findViewById(R.id.tvAction);
            tvTarget = itemView.findViewById(R.id.tvTarget);
            tvAdminId = itemView.findViewById(R.id.tvAdminId);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}