package com.example.carpooling.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpooling.R;
import com.example.carpooling.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "REGISTER_ACTIVITY";

    private EditText edtFullName, edtEmail, edtPhone, edtPassword;
    private RadioGroup rgRole;
    private RadioButton rbDriver;
    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        rgRole = findViewById(R.id.rgRole);
        rbDriver = findViewById(R.id.rbDriver);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> performRegister());

        if (tvLogin != null) {
            tvLogin.setOnClickListener(v -> finish());
        }
    }

    private void performRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String role = (rbDriver != null && rbDriver.isChecked()) ? "DRIVER" : "PASSENGER";

        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }


        if (!email.contains("@") || email.endsWith("@")) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ email hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang tạo tài khoản...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {

                            saveUserToFirestore(user.getUid(), fullName, email, phone, role);
                        }
                    } else {
                        resetRegisterButton();
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                        Toast.makeText(RegisterActivity.this, "Lỗi Auth: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String fullName, String email, String phone, String role) {

        final String emailDomain;
        if (email != null && email.contains("@")) {
            emailDomain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        } else {
            emailDomain = "";
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("name", fullName);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);
        userMap.put("organizationDomain", emailDomain);

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Lưu thông tin và phân nhóm tổ chức (" + emailDomain + ") thành công!");


                    SharedPrefManager.getInstance(RegisterActivity.this).saveUserSession(uid, role, fullName);

                    Toast.makeText(RegisterActivity.this, " Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if ("DRIVER".equalsIgnoreCase(role)) {
                        intent = new Intent(RegisterActivity.this, DriverActivity.class);
                    } else {
                        intent = new Intent(RegisterActivity.this, MainActivity.class);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetRegisterButton();
                    Log.e(TAG, "Lỗi Firestore: ", e);
                    Toast.makeText(RegisterActivity.this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetRegisterButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("ĐĂNG KÝ TÀI KHOẢN");
    }
}