package com.rdkp63.ecom_auth_services.service;

import com.rdkp63.ecom_auth_services.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    RefreshToken verifyExpiration(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    Integer revokeAllRefreshTokensForUser(Long userId);
    void revokeByToken(String token);
}
