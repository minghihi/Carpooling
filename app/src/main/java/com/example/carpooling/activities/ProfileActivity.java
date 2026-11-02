package com.example.carpooling.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpooling.R;
import com.example.carpooling.utils.BottomNavHelper;
import com.example.carpooling.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserRole;
    private Button btnSettings, btnSupport, btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPrefManager prefManager;
    private String userRole = "PASSENGER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

        if (getIntent() != null && getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        } else {

            String prefRole = prefManager.getUserRole();
            if (prefRole != null && !prefRole.isEmpty()) {
                userRole = prefRole;
            }
        }

        initViews();
        setupUserInfo();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupUserInfo();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserRole = findViewById(R.id.tvUserRole);

        btnSettings = findViewById(R.id.btnSettings);
        btnSupport = findViewById(R.id.btnSupport);
        btnLogout = findViewById(R.id.btnLogout);


        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
        });


        btnSupport.setOnClickListener(v -> showSupportDialog());


        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void setupUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }

        String userName = prefManager.getUserName();
        if (userName != null && !userName.isEmpty()) {
            tvUserName.setText(userName);
        } else {
            tvUserName.setText("Thành viên Carpooling");
        }

        tvUserRole.setText("Vai trò: " + (userRole.equalsIgnoreCase("DRIVER") ? "Tài xế (Driver)" : "Hành khách (Passenger)"));
    }

    private void showSupportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gửi yêu cầu hỗ trợ");

        final EditText input = new EditText(this);
        input.setHint("Nhập nội dung cần hỗ trợ hoặc báo lỗi...");
        input.setPadding(40, 30, 40, 30);
        builder.setView(input);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String content = input.getText().toString().trim();
            if (!content.isEmpty()) {
                FirebaseUser user = mAuth.getCurrentUser();
                Map<String, Object> supportMap = new HashMap<>();
                supportMap.put("uid", user != null ? user.getUid() : "unknown");
                supportMap.put("email", user != null ? user.getEmail() : "unknown");
                supportMap.put("content", content);
                supportMap.put("timestamp", System.currentTimeMillis());

                db.collection("supports").add(supportMap)
                        .addOnSuccessListener(documentReference -> Toast.makeText(this, "✅ Đã gửi yêu cầu hỗ trợ thành công!", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private void setupBottomNavigation() {
        BottomNavHelper.setupNav(this, userRole, 3);
    }

    private void performLogout() {
        SharedPrefManager.logoutUser(this);
    }
}