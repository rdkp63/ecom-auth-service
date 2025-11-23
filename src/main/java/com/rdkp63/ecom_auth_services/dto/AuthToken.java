package com.rdkp63.ecom_auth_services.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthToken {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}
