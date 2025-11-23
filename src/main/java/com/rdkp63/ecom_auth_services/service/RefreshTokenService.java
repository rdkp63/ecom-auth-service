package com.rdkp63.ecom_auth_services.service;

import com.rdkp63.ecom_auth_services.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    RefreshToken verifyExpiration(RefreshToken token);
}
