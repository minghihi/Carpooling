package com.example.carpooling.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpooling.R;
import com.example.carpooling.adapters.ChatAdapter;
import com.example.carpooling.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText etMessageInput;
    private Button btnSendMessage;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String rideId;
    private String currentUserId;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    private String currentUserName = "Thành viên";
    private String currentUserRole = "PASSENGER";
    private String currentUserAvatar = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rideId = getIntent().getStringExtra("RIDE_ID");
        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin phòng chat!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);

        loadCurrentUserInfo();
        listenForMessages();

        btnSendMessage.setOnClickListener(v -> {
            String messageText = etMessageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });
    }

    private void loadCurrentUserInfo() {
        if (currentUserId.isEmpty()) return;

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        if (name == null) name = documentSnapshot.getString("name");
                        if (name != null) currentUserName = name;

                        String role = documentSnapshot.getString("role");
                        if (role != null) currentUserRole = role;

                        String avatar = documentSnapshot.getString("avatarUrl");
                        if (avatar != null) currentUserAvatar = avatar;
                    }
                });
    }

    private void listenForMessages() {
        db.collection("chats").document(rideId).collection("messages")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (value != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }

                        messageList.sort((m1, m2) -> {
                            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
                            return m1.getTimestamp().compareTo(m2.getTimestamp());
                        });

                        chatAdapter.notifyDataSetChanged();

                        if (!messageList.isEmpty()) {
                            recyclerViewChat.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage(String text) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("senderId", currentUserId);
        messageMap.put("senderName", currentUserName);
        messageMap.put("senderRole", currentUserRole);
        messageMap.put("senderAvatar", currentUserAvatar);
        messageMap.put("message", text);
        messageMap.put("timestamp", FieldValue.serverTimestamp());

        db.collection("chats").document(rideId).collection("messages")
                .add(messageMap)
                .addOnSuccessListener(documentReference -> {
                    etMessageInput.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}