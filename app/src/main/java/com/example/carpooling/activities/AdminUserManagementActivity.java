package com.example.carpooling.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUserManagementActivity extends AppCompatActivity {

    private RecyclerView rvAdminUsers;
    private EditText edtSearchUser;
    private Button btnBack;
    private FirebaseFirestore db;
    private List<User> userList = new ArrayList<>();
    private List<User> filteredUserList = new ArrayList<>();
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_management);

        db = FirebaseFirestore.getInstance();
        rvAdminUsers = findViewById(R.id.rvAdminUsers);
        edtSearchUser = findViewById(R.id.edtSearchUser);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvAdminUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(filteredUserList);
        rvAdminUsers.setAdapter(userAdapter);

        fetchUsersFromFirestore();

        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchUsersFromFirestore() {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            userList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    if (user.getUserId() == null || user.getUserId().isEmpty()) {
                        user.setUserId(doc.getId());
                    }
                    userList.add(user);
                }
            }
            filteredUserList.clear();
            filteredUserList.addAll(userList);
            userAdapter.notifyDataSetChanged();
        });
    }

    private void filterUsers(String query) {
        filteredUserList.clear();
        String lowerCaseQuery = query.toLowerCase().trim();
        for (User user : userList) {
            String name = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
            String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
            if (name.contains(lowerCaseQuery) || email.contains(lowerCaseQuery)) {
                filteredUserList.add(user);
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    private void showUserActionDialog(User user, int position) {
        CharSequence[] options = {
                user.isActive() ? "🔒 Khóa tài khoản" : "🔓 Mở khóa tài khoản",
                "🗑️ Xóa tài khoản vĩnh viễn"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quản lý tài khoản: " + (user.getUsername() != null ? user.getUsername() : user.getEmail()));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {

                boolean newStatus = !user.isActive();
                db.collection("users").document(user.getUserId())
                        .update("active", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            user.setActive(newStatus);
                            userAdapter.notifyItemChanged(position);

                            Map<String, Object> logData = new HashMap<>();
                            logData.put("adminId", "quangminh867@gmail.com");
                            logData.put("action", newStatus ? "MỞ KHÓA TÀI KHOẢN" : "KHÓA TÀI KHOẢN");
                            logData.put("target", "User ID: " + user.getUserId() + " (" + user.getEmail() + ")");
                            logData.put("timestamp", FieldValue.serverTimestamp());
                            db.collection("system_logs").add(logData);

                            Toast.makeText(this, "Đã cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else if (which == 1) {

                new AlertDialog.Builder(this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            db.collection("users").document(user.getUserId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Map<String, Object> logData = new HashMap<>();
                                        logData.put("adminId", "quangminh867@gmail.com");
                                        logData.put("action", "XÓA TÀI KHOẢN");
                                        logData.put("target", "User ID: " + user.getUserId() + " (" + user.getEmail() + ")");
                                        logData.put("timestamp", FieldValue.serverTimestamp());
                                        db.collection("system_logs").add(logData);

                                        userList.remove(user);
                                        filteredUserList.remove(position);
                                        userAdapter.notifyItemRemoved(position);
                                        Toast.makeText(this, "Đã xóa tài khoản và ghi nhận nhật ký!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        builder.show();
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> list;

        public UserAdapter(List<User> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = list.get(position);
            holder.tvUserName.setText(user.getUsername() != null && !user.getUsername().isEmpty() ? user.getUsername() : "Chưa cập nhật tên");
            holder.tvUserEmail.setText(user.getEmail());
            holder.tvUserRole.setText("Vai trò: " + (user.getRole() != null ? user.getRole() : "PASSENGER"));

            boolean isActive = user.isActive();
            holder.tvUserStatus.setText(isActive ? "Hoạt động" : "Đã khóa");
            holder.tvUserStatus.setTextColor(isActive ? 0xFF28a745 : 0xFFdc3545);
            holder.itemView.setOnClickListener(v -> showUserActionDialog(user, holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvUserEmail, tvUserRole, tvUserStatus;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
                tvUserRole = itemView.findViewById(R.id.tvUserRole);
                tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
            }
        }
    }
}