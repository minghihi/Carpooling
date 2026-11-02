package com.example.carpooling.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.models.SupportRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminSupportActivity extends AppCompatActivity {

    private RecyclerView rvAdminSupport;
    private Button btnBack;
    private FirebaseFirestore db;
    private List<SupportRequest> supportList = new ArrayList<>();
    private SupportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_support);

        db = FirebaseFirestore.getInstance();
        rvAdminSupport = findViewById(R.id.rvAdminSupport);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvAdminSupport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SupportAdapter(supportList);
        rvAdminSupport.setAdapter(adapter);

        fetchSupportRequests();
    }

    private void fetchSupportRequests() {
        db.collection("supports").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    supportList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SupportRequest request = doc.toObject(SupportRequest.class);
                        if (request != null) {
                            if (request.getSupportId() == null || request.getSupportId().isEmpty()) {
                                request.setSupportId(doc.getId());
                            }
                            supportList.add(request);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh sách khiếu nại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private class SupportAdapter extends RecyclerView.Adapter<SupportAdapter.ViewHolder> {
        private List<SupportRequest> list;

        public SupportAdapter(List<SupportRequest> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_support, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SupportRequest request = list.get(position);
            holder.tvCategory.setText("Danh mục: " + (request.getCategory() != null ? request.getCategory() : "Khác"));
            holder.tvDetail.setText(request.getContent() != null ? request.getContent() : "Không có nội dung");

            if (holder.tvStatus != null) {
                holder.tvStatus.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategory, tvDetail, tvStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvDetail = itemView.findViewById(R.id.tvDetail);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}