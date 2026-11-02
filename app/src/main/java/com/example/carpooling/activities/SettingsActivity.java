package com.example.carpooling.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carpooling.R;
import com.example.carpooling.utils.SharedPrefManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText edtProfileName, edtProfilePhone, edtProfileDob, edtProfileEmail;
    private Button btnSaveProfile, btnChangePassword, btnSupport;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        edtProfileDob = findViewById(R.id.edtProfileDob);
        edtProfileEmail = findViewById(R.id.edtProfileEmail);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnSupport = findViewById(R.id.btnSupport);
    }

    private void loadUserData() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String dob = documentSnapshot.getString("dob");
                        String email = documentSnapshot.getString("email");

                        if (name != null && edtProfileName != null) edtProfileName.setText(name);
                        if (phone != null && edtProfilePhone != null) edtProfilePhone.setText(phone);
                        if (dob != null && edtProfileDob != null) edtProfileDob.setText(dob);
                        if (email != null && edtProfileEmail != null) edtProfileEmail.setText(email);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> {
                String newName = edtProfileName != null ? edtProfileName.getText().toString().trim() : "";
                String newPhone = edtProfilePhone != null ? edtProfilePhone.getText().toString().trim() : "";
                String newDob = edtProfileDob != null ? edtProfileDob.getText().toString().trim() : "";

                if (newName.isEmpty()) {
                    Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentUserId == null) {
                    Toast.makeText(this, "Lỗi: Không tìm thấy phiên đăng nhập!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSaveProfile.setEnabled(false);
                btnSaveProfile.setText("Đang lưu...");

                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("uid", currentUserId);
                updateMap.put("name", newName);
                updateMap.put("phone", newPhone);
                updateMap.put("dob", newDob);
                if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
                    updateMap.put("email", mAuth.getCurrentUser().getEmail());
                }

                db.collection("users").document(currentUserId)
                        .set(updateMap, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            btnSaveProfile.setEnabled(true);
                            btnSaveProfile.setText("Lưu thay đổi thông tin");

                            SharedPrefManager.getInstance(this).saveUserSession(
                                    currentUserId,
                                    SharedPrefManager.getInstance(this).getUserRole(),
                                    newName
                            );

                            Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            btnSaveProfile.setEnabled(true);
                            btnSaveProfile.setText("Lưu thay đổi thông tin");
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null || user.getEmail() == null) {
                    Toast.makeText(this, "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Đổi mật khẩu nhanh");

                final TextInputEditText input = new TextInputEditText(this);
                input.setHint("Nhập mật khẩu mới (ví dụ: 111111)");
                input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);

                builder.setPositiveButton("Xác nhận", (dialog, which) -> {
                    String newPassword = input.getText().toString().trim();
                    if (newPassword.isEmpty()) {
                        Toast.makeText(this, "Mật khẩu không được để trống!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "✅ Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> {
                                if (e.getMessage() != null && e.getMessage().contains("sensitive")) {
                                    Toast.makeText(this, "Vui lòng đăng xuất ra rồi đăng nhập lại 1 lần để xác thực!", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                });
                builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
                builder.show();
            });
        }

        if (btnSupport != null) {
            btnSupport.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, SupportCenterActivity.class);
                startActivity(intent);
            });
        }
    }
}