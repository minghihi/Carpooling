package com.example.carpooling.models;

import com.google.firebase.firestore.PropertyName;

public class SupportRequest {
    private String supportId;
    private String userId;
    private String category;

    @PropertyName("content")
    private String content;

    private String status;
    private Object timestamp;

    public SupportRequest() {
    }

    public SupportRequest(String supportId, String userId, String category, String content, String status, Object timestamp) {
        this.supportId = supportId;
        this.userId = userId;
        this.category = category;
        this.content = content;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getSupportId() {
        return supportId;
    }

    public void setSupportId(String supportId) {
        this.supportId = supportId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category != null ? category : "Khác";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @PropertyName("content")
    public String getContent() {
        return content != null && !content.isEmpty() ? content : "Không có nội dung";
    }

    @PropertyName("content")
    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }
}