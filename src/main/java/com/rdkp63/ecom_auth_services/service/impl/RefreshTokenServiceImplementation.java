package com.rdkp63.ecom_auth_services.service.impl;

import com.rdkp63.ecom_auth_services.entity.RefreshToken;
import com.rdkp63.ecom_auth_services.repository.RefreshTokenRepository;
import com.rdkp63.ecom_auth_services.repository.UserRepository;
import com.rdkp63.ecom_auth_services.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImplementation implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Override
    public RefreshToken createRefreshToken(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: "+userId));

        // generate unique token
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now()) || refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new RuntimeException("Refresh token expired or revoked");
        }
        return refreshToken;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Revoke (bulk) all refresh tokens for a user in a single DB update.
     *
     * @return
     */
    @Override
    @Transactional
    public Integer revokeAllRefreshTokensForUser(Long userId) {
        return refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Override
    public void revokeByToken(String token) {

    }
}
