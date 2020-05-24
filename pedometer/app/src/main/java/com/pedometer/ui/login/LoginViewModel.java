package com.pedometer.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Patterns;

import com.pedometer.MyApplication;
import com.pedometer.R;
import com.pedometer.client.api.AccountApi;
import com.pedometer.client.model.UserInfo;
import com.pedometer.ui.MainActivity;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private AccountApi loginRepository;

    LoginViewModel(AccountApi loginRepository) {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String userId, String password, Activity activity) {
        // can be launched in a separate asynchronous job
        AsyncTask.execute(() -> {
            UserInfo info = null;
            try {
                info = new UserInfo(userId, password, MyApplication.DeviceToken);

                info = loginRepository.accountPost(info);
                MyApplication.UserInfo = info;

            } catch (Exception e) {
                e.printStackTrace();
            }
            UserInfo finalInfo = info;
            activity.runOnUiThread(() -> {
                if (finalInfo != null) {
                    loginResult.setValue(new LoginResult(finalInfo));
                } else {
                    loginResult.setValue(new LoginResult(R.string.login_failed));
                }
            });
        });

    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}
