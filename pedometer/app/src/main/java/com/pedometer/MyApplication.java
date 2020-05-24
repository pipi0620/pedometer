package com.pedometer;

import android.app.Application;

import com.google.firebase.iid.FirebaseInstanceId;
import com.pedometer.client.model.UserInfo;

public class MyApplication extends Application {
    public static String DeviceToken = null;
    public static UserInfo UserInfo = null;

    @Override
    public void onCreate() {
        super.onCreate();
        DeviceToken = FirebaseInstanceId.getInstance().getToken();
    }
}
