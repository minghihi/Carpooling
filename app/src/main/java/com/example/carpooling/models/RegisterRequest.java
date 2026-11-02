package com.example.carpooling.models;

public class RegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String role;

    public RegisterRequest(String fullName, String email, String phone, String password, String role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}