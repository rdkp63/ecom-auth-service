package com.rdkp63.ecom_auth_services.repository;

import com.rdkp63.ecom_auth_services.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId);
}
