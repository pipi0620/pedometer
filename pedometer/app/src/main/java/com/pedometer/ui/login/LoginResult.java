package com.pedometer.ui.login;

import androidx.annotation.Nullable;

import com.pedometer.client.model.UserInfo;

/**
 * Authentication result : success (user details) or error message.
 */
class LoginResult {
    @Nullable
    private UserInfo success;
    @Nullable
    private Integer error;

    LoginResult(@Nullable Integer error) {
        this.error = error;
    }

    LoginResult(@Nullable UserInfo success) {
        this.success = success;
    }

    @Nullable
    UserInfo getSuccess() {
        return success;
    }

    @Nullable
    Integer getError() {
        return error;
    }
}
