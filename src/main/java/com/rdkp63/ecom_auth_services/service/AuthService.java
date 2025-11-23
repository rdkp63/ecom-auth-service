package com.rdkp63.ecom_auth_services.service;

import com.rdkp63.ecom_auth_services.dto.AuthToken;
import com.rdkp63.ecom_auth_services.dto.ResponseDTO.UserResponse;
import com.rdkp63.ecom_auth_services.dto.requestDTO.LoginRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RegisterRequest;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthToken login(LoginRequest request);
}
