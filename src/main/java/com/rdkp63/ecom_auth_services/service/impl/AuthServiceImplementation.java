package com.rdkp63.ecom_auth_services.service.impl;

import com.rdkp63.ecom_auth_services.dto.AuthToken;
import com.rdkp63.ecom_auth_services.dto.ResponseDTO.UserResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
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
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email "+request.getEmail() + " already registered");
        }

        String username = generateUsername(request.getFullName());

        //Fetch USER roles
        String defaultRoleString = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default USER role missing in DB"));

        //Build Role entity
        Role defaultRole = Role.builder().name("USER").build();

        //Build User entity
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roles(Set.of(defaultRole))
                .active(true)
                .emailVerified(false)
                .build();

        User saved = userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
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
                        .map(role-> role.getName())
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
