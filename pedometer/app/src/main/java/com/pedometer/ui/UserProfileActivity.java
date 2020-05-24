package com.pedometer.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.pedometer.MyApplication;
import com.pedometer.R;
import com.pedometer.client.ApiException;
import com.pedometer.client.api.AccountApi;
import com.pedometer.client.model.UserInfo;
import com.pedometer.databinding.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class UserProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private UserProfileActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.user_profile_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        binding.setUserId(MyApplication.UserInfo.getId());
        binding.setUserDisplayName(MyApplication.UserInfo.getUserName());
        binding.setUserPhone(MyApplication.UserInfo.getPhone());
        binding.setUserPassword(MyApplication.UserInfo.getPassword());
        binding.setUserHeight(MyApplication.UserInfo.getHeight().toString());
        binding.setUserWeight(MyApplication.UserInfo.getWeight().toString());
        binding.setActivity(this);
    }

    @Override
    public void onClick(View v) {
        UserInfo info = new UserInfo(binding.getUserId(), binding.getUserDisplayName(), binding.getUserPassword(), binding.getUserPhone(), Float.parseFloat(binding.getUserHeight()), Float.parseFloat(binding.getUserWeight()), null);
        findViewById(R.id.user_profile_progressbar).setVisibility(View.VISIBLE);
        AsyncTask.execute(() -> {
            try {
                AccountApi api = new AccountApi();
                api.accountPutUserInfo(info.getId(), info);
                this.runOnUiThread(() -> {
                    Toast.makeText(this, "Update Success!", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                this.runOnUiThread(() -> {
                    findViewById(R.id.user_profile_progressbar).setVisibility(View.INVISIBLE);
                    Toast.makeText(this, "Update Failed!", Toast.LENGTH_LONG).show();
                });

            }
        });

    }
}