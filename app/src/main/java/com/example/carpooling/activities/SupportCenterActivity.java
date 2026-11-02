package com.example.carpooling.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.adapters.SupportHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupportCenterActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private EditText edtSupportDetail;
    private Button btnSubmitSupport;
    private RecyclerView rvSupportHistory;

    private FirebaseFirestore db;
    private String currentUserId;

    private List<Map<String, Object>> historyList;
    private SupportHistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_center);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "Anonymous";
        }

        initViews();
        setupSpinner();
        setupRecyclerView();
        loadSupportHistory();

        btnSubmitSupport.setOnClickListener(v -> submitSupportRequest());
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinnerCategory);
        edtSupportDetail = findViewById(R.id.edtSupportDetail);
        btnSubmitSupport = findViewById(R.id.btnSubmitSupport);
        rvSupportHistory = findViewById(R.id.rvSupportHistory);
    }

    private void setupSpinner() {
        String[] categories = {
                "Hỗ trợ tài khoản",
                "Vấn đề với tài xế",
                "Vấn đề an toàn khẩn cấp",
                "Vấn đề về ứng dụng"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        historyAdapter = new SupportHistoryAdapter(historyList);
        rvSupportHistory.setLayoutManager(new LinearLayoutManager(this));
        rvSupportHistory.setAdapter(historyAdapter);
    }

    private void submitSupportRequest() {
        String category = spinnerCategory.getSelectedItem().toString();
        String detail = edtSupportDetail.getText().toString().trim();

        if (detail.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mô tả chi tiết vấn đề!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitSupport.setEnabled(false);
        btnSubmitSupport.setText("Đang gửi...");

        Map<String, Object> supportData = new HashMap<>();
        supportData.put("userId", currentUserId);
        supportData.put("category", category);
        supportData.put("detail", detail);
        supportData.put("status", "Đang xử lý");
        supportData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("supports")
                .add(supportData)
                .addOnSuccessListener(documentReference -> {
                    btnSubmitSupport.setEnabled(true);
                    btnSubmitSupport.setText("Gửi yêu cầu");
                    edtSupportDetail.setText("");
                    Toast.makeText(this, "✅ Gửi yêu cầu hỗ trợ thành công!", Toast.LENGTH_SHORT).show();
                    loadSupportHistory();
                })
                .addOnFailureListener(e -> {
                    btnSubmitSupport.setEnabled(true);
                    btnSubmitSupport.setText("Gửi yêu cầu");
                    Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSupportHistory() {
        db.collection("supports")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            historyList.add(data);
                        }
                    }
                    historyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                });
    }
}