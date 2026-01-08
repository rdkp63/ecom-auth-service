package com.rdkp63.ecom_auth_services.controller;

import com.rdkp63.ecom_auth_services.dto.AuthToken;
import com.rdkp63.ecom_auth_services.dto.responseDTO.UserResponse;
import com.rdkp63.ecom_auth_services.dto.requestDTO.LoginRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RefreshTokenRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RegisterRequest;
import com.rdkp63.ecom_auth_services.entity.RefreshToken;
import com.rdkp63.ecom_auth_services.entity.Role;
import com.rdkp63.ecom_auth_services.entity.User;
import com.rdkp63.ecom_auth_services.repository.RefreshTokenRepository;
import com.rdkp63.ecom_auth_services.security.JwtTokenProvider;
import com.rdkp63.ecom_auth_services.service.AuthService;
import com.rdkp63.ecom_auth_services.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // --------------------- REGISTER ---------------------
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register API called with email={}", request.getEmail());
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---------------------- LOGIN -----------------------
    @PostMapping("/login")
    public ResponseEntity<AuthToken> login(@Valid @RequestBody LoginRequest request) {
        AuthToken token = authService.login(request);
        return ResponseEntity.ok(token);
    }

    // ---------------------- REFRESH_TOKEN ----------------
    @PostMapping("/refresh")
    public ResponseEntity<AuthToken> refresh(@RequestBody RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenProvider.createToken(
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getId()
        );

        return ResponseEntity.ok(
                AuthToken.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken.getToken()) // same refresh token
                        .expiresIn(jwtExpirationMs)
                        .build()
        );
    }

    // ---------------------- LOGOUT -----------------------
    @PostMapping("/logout")
    public ResponseEntity<Map<String,String>> logout(@RequestBody(required = false) Map<String, String> request) {
        ResponseEntity<Map<String, String>> response = authService.logout(request);
        return response;
    }
}
