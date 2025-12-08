package com.rdkp63.ecom_auth_services.repository;

import com.rdkp63.ecom_auth_services.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId);

    /**
     * Bulk update: mark all non-revoked refresh tokens for a user as revoked.
     * Returns the number of rows updated.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked=true WHERE r.user.id= :userId AND r.revoked=false")
    Integer revokeAllByUserId(@Param("userId") Long userId);
}
