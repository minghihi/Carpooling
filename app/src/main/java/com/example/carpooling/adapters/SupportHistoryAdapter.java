package com.example.carpooling.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;

import java.util.List;
import java.util.Map;

public class SupportHistoryAdapter extends RecyclerView.Adapter<SupportHistoryAdapter.ViewHolder> {

    private List<Map<String, Object>> historyList;

    public SupportHistoryAdapter(List<Map<String, Object>> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = historyList.get(position);
        String category = (String) item.get("category");
        String detail = (String) item.get("detail");
        String status = (String) item.get("status");

        holder.text1.setText("[" + status + "] " + category);
        holder.text2.setText(detail);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}