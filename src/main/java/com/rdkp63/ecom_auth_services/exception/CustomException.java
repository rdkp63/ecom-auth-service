package com.rdkp63.ecom_auth_services.exception;

import com.rdkp63.ecom_auth_services.enums.ErrorDetails;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorDetails errorDetails;

    public CustomException(ErrorDetails errorDetails) {
        super(errorDetails.getErrorMessage());
        this.errorDetails = errorDetails;
    }
}
