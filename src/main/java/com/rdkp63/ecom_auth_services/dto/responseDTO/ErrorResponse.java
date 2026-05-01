package com.rdkp63.ecom_auth_services.dto.responseDTO;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {

    private HttpStatus status;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime timestamp;
}
