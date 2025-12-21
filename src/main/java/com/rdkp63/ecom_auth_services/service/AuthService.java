package com.rdkp63.ecom_auth_services.service;

import com.rdkp63.ecom_auth_services.dto.AuthToken;
import com.rdkp63.ecom_auth_services.dto.responseDTO.UserResponse;
import com.rdkp63.ecom_auth_services.dto.requestDTO.LoginRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RegisterRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthToken login(LoginRequest request);

    ResponseEntity<Map<String, String>> logout(Map<String, String> request);
}
