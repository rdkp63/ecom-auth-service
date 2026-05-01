package com.rdkp63.ecom_auth_services.service.impl;

import com.rdkp63.ecom_auth_services.dto.AuthToken;
import com.rdkp63.ecom_auth_services.dto.requestDTO.LogoutRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RefreshTokenRequest;
import com.rdkp63.ecom_auth_services.dto.responseDTO.UserResponse;
import com.rdkp63.ecom_auth_services.dto.requestDTO.LoginRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.RegisterRequest;
import com.rdkp63.ecom_auth_services.entity.RefreshToken;
import com.rdkp63.ecom_auth_services.entity.Role;
import com.rdkp63.ecom_auth_services.entity.User;
import com.rdkp63.ecom_auth_services.enums.RoleName;
import com.rdkp63.ecom_auth_services.exception.BadCredentialsException;
import com.rdkp63.ecom_auth_services.exception.UserNotFoundException;
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
import org.springframework.security.core.AuthenticationException;
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
            Role defaultRole = roleRepository.findByName(RoleName.USER.getName())
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

        // If authenticate() throws no exception → success
        log.info("Login attempt for user {}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new UserNotFoundException("User not found with email: "+request.getEmail())
                );

        //Validate credentials via AuthenticationManager
        log.info("Validating if credentials correct or not");
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException(e.getMessage());
        }

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

    public ResponseEntity<Map<String, String>> logout(LogoutRequest request) {
        if(request == null) {
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
        } else {
            // Check whether refreshToken is expired.
            try{
                RefreshToken refreshToken = refreshTokenService
                        .findByToken(request.getRefreshToken())
                        .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

                refreshTokenService.verifyExpiration(refreshToken);
            }catch (RuntimeException e){
                throw new RuntimeException(e);
            }

            // else: try token-based logout (revoke single refresh token)
            if (request.getRefreshToken()!=null) {
                String refreshToken = request.getRefreshToken();

                refreshTokenService.revokeByToken(refreshToken);
                return ResponseEntity.ok(Map.of("message","Refresh token revoked."));
            }
        }

        return ResponseEntity.badRequest().body(Map.of("message","No authentication or refreshToken provided."));
    }

    public AuthToken refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Generate new JWT token
        String newAccessToken = jwtTokenProvider.createToken(
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getId()
        );


        return AuthToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(expiresInMs)
                .build();
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

    private Boolean isRefreshTokenExpired(RefreshToken refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        return LocalDateTime.now().isAfter(token.getExpiryDate());
    }
}
