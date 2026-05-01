package com.rdkp63.ecom_auth_services.exception;

import com.rdkp63.ecom_auth_services.enums.ErrorDetails;

public class BadCredentialsException extends CustomException{
  public BadCredentialsException(String message) {
    super(ErrorDetails.BAD_CREDENTIALS);
  }
}
