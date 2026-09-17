package com.careflow.serviceops.api.dto;

import java.util.List;

public class LoginResponse {

    private String token;

    private String email;

    private List<String> roles;

    public LoginResponse() {
    }

    public LoginResponse(String token, String email, List<String> roles) {
        this.token = token;
        this.email = email;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}