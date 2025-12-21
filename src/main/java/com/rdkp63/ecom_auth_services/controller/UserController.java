package com.rdkp63.ecom_auth_services.controller;


import com.rdkp63.ecom_auth_services.dto.requestDTO.ChangePasswordRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.UpdateProfileRequest;
import com.rdkp63.ecom_auth_services.dto.responseDTO.UserProfileResponse;
import com.rdkp63.ecom_auth_services.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disableAccount() {
        userService.disableAccount();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lock")
    public ResponseEntity<Void> lockAccount() {
        userService.lockAccount();
        return ResponseEntity.noContent().build();
    }
}
