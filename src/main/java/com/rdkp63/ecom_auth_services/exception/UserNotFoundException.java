package com.rdkp63.ecom_auth_services.exception;

import com.rdkp63.ecom_auth_services.enums.ErrorDetails;

public class UserNotFoundException extends CustomException{
  public UserNotFoundException(String message) {
    super(ErrorDetails.USER_NOT_FOUND);
  }
}
