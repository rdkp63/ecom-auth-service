package com.rdkp63.ecom_auth_services.service.impl;

import com.rdkp63.ecom_auth_services.dto.requestDTO.ChangePasswordRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.UpdateProfileRequest;
import com.rdkp63.ecom_auth_services.dto.responseDTO.UserProfileResponse;
import com.rdkp63.ecom_auth_services.entity.User;
import com.rdkp63.ecom_auth_services.repository.UserRepository;
import com.rdkp63.ecom_auth_services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImplementation implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getCurrentUser() {
        User user = getAuthenticatedUser();
        return map(user);
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getAuthenticatedUser();
        user.setFullName(request.getFullName());
        user.setUpdatedAt(LocalDateTime.now());
        return map(user);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public void disableAccount() {
        User user = getAuthenticatedUser();
        user.setEnabled(false);
    }

    @Override
    public void lockAccount() {
        User user = getAuthenticatedUser();
        user.setAccountNonLocked(false);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private UserProfileResponse map(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}

