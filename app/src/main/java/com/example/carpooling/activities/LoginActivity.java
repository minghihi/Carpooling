package com.example.carpooling.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpooling.R;
import com.example.carpooling.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN_ACTIVITY";

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            String savedRole = SharedPrefManager.getInstance(this).getUserRole();
            navigateBasedOnRole(savedRole);
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("quangminh867@gmail.com".equalsIgnoreCase(email) && "111111".equals(password)) {
                Toast.makeText(LoginActivity.this, "Đăng nhập quyền Quản trị viên thành công!", Toast.LENGTH_SHORT).show();

                SharedPrefManager.getInstance(LoginActivity.this).saveUserSession("ADMIN_DEFAULT_ID", "ADMIN", "Quản Trị Viên");

                Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            handleLogin(email, password);
        });

        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        builder.setView(dialogView);

        EditText edtEmailPhone = dialogView.findViewById(R.id.edtResetEmailPhone);
        Button btnSendResetLink = dialogView.findViewById(R.id.btnSendResetLink);

        AlertDialog dialog = builder.create();

        btnSendResetLink.setOnClickListener(v -> {
            String inputVal = edtEmailPhone.getText().toString().trim();
            if (inputVal.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Email hoặc Số điện thoại!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputVal.contains("@")) {
                mAuth.sendPasswordResetEmail(inputVal)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Đã gửi link thay đổi mật khẩu tới email " + inputVal + ". Vui lòng kiểm tra hộp thư!", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(this, "Lỗi: " + (task.getException() != null ? task.getException().getMessage() : "Không xác định"), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Số điện thoại hiện chưa hỗ trợ gửi link tự động. Vui lòng sử dụng Email!", Toast.LENGTH_LONG).show();
            }
        });

        dialog.show();
    }

    private void handleLogin(String email, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Lấy Firebase Token thất bại", task.getException());
                    } else {
                        Log.d(TAG, "Firebase Device Token hiện tại: " + task.getResult());
                    }

                    executeFirebaseLogin(email, password);
                });
    }

    private void executeFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRoleAndFinishLogin(user.getUid());
                        }
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("ĐĂNG NHẬP");
                        Toast.makeText(LoginActivity.this, "Email hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRoleAndFinishLogin(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");

                    String role = "PASSENGER";
                    String name = "";

                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("role")) {
                            role = documentSnapshot.getString("role");
                        }
                        if (documentSnapshot.contains("name")) {
                            name = documentSnapshot.getString("name");
                        }
                    }

                    SharedPrefManager.getInstance(LoginActivity.this).saveUserSession(userId, role, name);
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    navigateBasedOnRole(role);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");
                    Log.e(TAG, "Lỗi lấy thông tin User từ Firestore: " + e.getMessage());

                    SharedPrefManager.getInstance(LoginActivity.this).saveUserSession(userId, "PASSENGER", "");
                    navigateToMain();
                });
    }

    private void navigateBasedOnRole(String role) {
        if ("DRIVER".equalsIgnoreCase(role)) {
            navigateToDriver();
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            navigateToAdmin();
        } else {
            navigateToMain();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToDriver() {
        Intent intent = new Intent(LoginActivity.this, DriverActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToAdmin() {
        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        startActivity(intent);
        finish();
    }
}