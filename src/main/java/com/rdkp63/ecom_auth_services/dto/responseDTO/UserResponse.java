package com.rdkp63.ecom_auth_services.dto.responseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
}
