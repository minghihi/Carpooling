package com.example.carpooling.models;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String senderId;
    private String senderName;
    private String senderRole;
    private String senderAvatar;
    private String message;
    private Timestamp timestamp;

    public ChatMessage() {

    }

    public ChatMessage(String senderId, String senderName, String senderRole, String senderAvatar, String message, Timestamp timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.senderAvatar = senderAvatar;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getSenderRole() { return senderRole; }
    public String getSenderAvatar() { return senderAvatar; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}