package com.rdkp63.ecom_auth_services.service.impl;

import com.rdkp63.ecom_auth_services.dto.AuthToken;
import com.rdkp63.ecom_auth_services.dto.responseDTO.UserResponse;
import com.rdkp63.ecom_auth_services.dto.requestDTO.LoginRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RegisterRequest;
import com.rdkp63.ecom_auth_services.entity.RefreshToken;
import com.rdkp63.ecom_auth_services.entity.Role;
import com.rdkp63.ecom_auth_services.entity.User;
import com.rdkp63.ecom_auth_services.repository.RoleRepository;
import com.rdkp63.ecom_auth_services.repository.UserRepository;
import com.rdkp63.ecom_auth_services.security.JwtTokenProvider;
import com.rdkp63.ecom_auth_services.service.AuthService;
import com.rdkp63.ecom_auth_services.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImplementation implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.expiration-ms}")
    private Long expiresInMs;

    @Override
    public UserResponse register(RegisterRequest request) {

        //Check if email already exists
        log.info("Checking if email already exists or not");
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email " + request.getEmail() + " already registered");
        }

        String username = generateUsername(request.getFullName());

        log.info("The roles in request is: {}", request.getRoles().toString());
        Set<Role> roles = null;
        if (request.getRoles() == null) {
            log.info("Looking for the default role 'USER' in DB");
            //Fetch USER roles
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Default USER role missing in DB"));
            log.info("Found role in DB: USER - {}", defaultRole.toString());

            //Build Role entity
            roles = Arrays.stream(
                    new Role[]{
                            defaultRole
                    }
            ).collect(Collectors.toSet());
        } else {
            roles = request.getRoles()
                    .stream()
                    .map(
                            role-> roleRepository.findByName(role).orElseThrow(() -> new RuntimeException("Unable to find role with name: " + role))
                    )
                    .collect(Collectors.toSet());
        }

        log.info("The roles of the user is: {}", roles);

        //Build User entity
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roles(roles)
                .active(true)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Built user is: {}", user);

        User saved = userRepository.save(user);
        log.info("Saved user in the DB: {}", saved);

        return UserResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .build();
    }

    @Override
    public AuthToken login(LoginRequest request) {

        //Validate credentials via AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // If authenticate() throws no exception → success
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new RuntimeException("User not found with email: "+request.getEmail())
                );

        // Generate JWT token
        String jwtToken = jwtTokenProvider.createToken(
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList(),
                user.getId()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthToken.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(expiresInMs)
                .build();
    }

    public ResponseEntity<Map<String, String>> logout(Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // We recommend user identity is email (as set in UserDetails)
            String email = auth.getName();
            // find user
            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                Integer updated = refreshTokenService.revokeAllRefreshTokensForUser(user.getId());
                return ResponseEntity
                        .ok(
                                Map.of(
                                        "message", "Logged out from all devices.",
                                        "row_updated", updated.toString()
                                )
                        );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","User not found"));
            }
        }

        // else: try token-based logout (revoke single refresh token)
        if (request != null && request.containsKey("refreshToken")) {
            String refreshToken = request.get("refreshToken");
            refreshTokenService.revokeByToken(refreshToken);
            return ResponseEntity.ok(Map.of("message","Refresh token revoked."));
        }

        return ResponseEntity.badRequest().body(Map.of("message","No authentication or refreshToken provided."));
    }

    // ---------------------- Helpers ------------------------

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    private String generateUsername(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "user" + System.currentTimeMillis(); // fallback
        }
        return fullName.toLowerCase().replaceAll("\\s+", "_");
    }
}
