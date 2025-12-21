package com.rdkp63.ecom_auth_services.repository;

import com.rdkp63.ecom_auth_services.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.enabled = false WHERE u.id = :userId")
    void disableUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = false WHERE u.id = :userId")
    void lockUser(@Param("userId") Long userId);
}
