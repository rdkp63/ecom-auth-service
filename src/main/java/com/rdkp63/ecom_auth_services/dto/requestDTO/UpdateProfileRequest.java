package com.rdkp63.ecom_auth_services.dto.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String fullName;
}

