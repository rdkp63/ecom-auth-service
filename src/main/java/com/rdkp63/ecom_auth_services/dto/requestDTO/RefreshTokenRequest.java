package com.rdkp63.ecom_auth_services.dto.requestDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenRequest {
    private String refreshToken;
}
