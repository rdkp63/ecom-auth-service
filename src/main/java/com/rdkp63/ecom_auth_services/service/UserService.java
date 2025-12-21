package com.rdkp63.ecom_auth_services.service;

import com.rdkp63.ecom_auth_services.dto.requestDTO.ChangePasswordRequest;
import com.rdkp63.ecom_auth_services.dto.requestDTO.UpdateProfileRequest;
import com.rdkp63.ecom_auth_services.dto.responseDTO.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUser();

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    void disableAccount();

    void lockAccount();
}

