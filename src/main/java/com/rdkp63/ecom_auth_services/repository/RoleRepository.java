package com.rdkp63.ecom_auth_services.repository;

import com.rdkp63.ecom_auth_services.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<String> findByName(String name);

    boolean existsByName(String name);
}
