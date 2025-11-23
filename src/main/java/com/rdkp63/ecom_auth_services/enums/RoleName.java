package com.rdkp63.ecom_auth_services.enums;

import lombok.Getter;

public enum RoleName {

    USER("user", "This is normal user"),
    ADMIN("admin", "This is admin user"),
    SELLER("saler", "This is saler user");


    @Getter
    private final String name;

    @Getter
    private final String description;

    RoleName(String name, String description){
        this.name=name;
        this.description=description;
    }
}
