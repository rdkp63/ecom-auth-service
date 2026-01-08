package com.rdkp63.ecom_auth_services.dto.requestDTO;

import com.rdkp63.ecom_auth_services.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RegisterRequest {

    @NotBlank(message = "Email cannot be null or empty")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Password cannot be null or empty")
    @Size(min=8, max=16 ,message = "Password length should be min 8 and max 16")
    private String password;

    @NotBlank(message = "Username cannot be null or empty")
    private String userName;

    @NotBlank(message = "Full name cannot be null or empty")
    private String fullName;

    private List<String> roles;
}
