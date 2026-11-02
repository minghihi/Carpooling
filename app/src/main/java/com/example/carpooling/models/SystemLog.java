package com.example.carpooling.models;

import com.google.firebase.Timestamp;

public class SystemLog {
    private String logId;
    private String adminId;
    private String action;
    private String target;
    private Timestamp timestamp;

    public SystemLog() {

    }

    public SystemLog(String logId, String adminId, String action, String target, Timestamp timestamp) {
        this.logId = logId;
        this.adminId = adminId;
        this.action = action;
        this.target = target;
        this.timestamp = timestamp;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}