package com.example.carpooling.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class
SystemLogActivity extends AppCompatActivity {

    private TextView tvLogContent;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tvLogContent = new TextView(this);
        tvLogContent.setPadding(32, 32, 32, 32);
        tvLogContent.setTextSize(14);
        setContentView(tvLogContent);

        db = FirebaseFirestore.getInstance();
        loadSystemLogs();
    }

    private void loadSystemLogs() {
        tvLogContent.setText("Đang tải nhật ký hệ thống...");

        db.collection("system_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder logsBuilder = new StringBuilder("--- NHẬT KÝ HỆ THỐNG (SYSTEM LOGS) ---\n\n");
                    if (queryDocumentSnapshots.isEmpty()) {
                        logsBuilder.append("Chưa có bản ghi nhật ký nào.");
                    } else {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String adminId = doc.getString("adminId");
                            String action = doc.getString("action");
                            String target = doc.getString("target");
                            logsBuilder.append("• Hành động: ").append(action)
                                    .append("\n  Admin: ").append(adminId)
                                    .append("\n  Đối tượng: ").append(target)
                                    .append("\n----------------------------------\n");
                        }
                    }
                    tvLogContent.setText(logsBuilder.toString());
                })
                .addOnFailureListener(e -> tvLogContent.setText("Lỗi tải nhật ký: " + e.getMessage()));
    }
}