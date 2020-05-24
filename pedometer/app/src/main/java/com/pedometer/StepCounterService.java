package com.pedometer;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class StepCounterService extends Service implements SensorEventListener {
    private final String TAG = "emmmm";
    private final String SP_STEP = "emmmm.Steps";
    private final String KEY_TODAY_STEP = "TodaySteps";
    private final String KEY_STEP = "Steps";
    private final String KEY_STEP_TIME = "StepsTime";

    private SensorManager mSensorManager;
    private Sensor mStepSensor;
    private static int sensorTotalStep = 0;
    private static long sensorTotalStepTime = 0;
    private static int todayStep = 0;

    private IBinder mIBinder = new IMyStepCountService.Stub() {
        @Override
        public int getSteps() throws RemoteException {
            return todayStep;
        }
    };

    public StepCounterService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        todayStep = getSharedPreferences(SP_STEP, MODE_PRIVATE).getInt(KEY_TODAY_STEP, 0);
        sensorTotalStep = getSharedPreferences(SP_STEP, MODE_PRIVATE).getInt(KEY_STEP, 0);
        sensorTotalStepTime = getSharedPreferences(SP_STEP, MODE_PRIVATE).getLong(KEY_STEP_TIME, 0);
        if (mStepSensor != null) {
            mSensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //重难点：
        //1.传感器的值是累计值，记录的是开机到现在的步数
        //2.传感器中的计数会在关机时丢失！
        //坑：
        //event.timestamp也是从开机到现在的时间戳，并且是纳秒级的
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int eventSteps = (int) event.values[0];
            //将传感器的时间转换成当前时间
            long nowTimestamp = System.currentTimeMillis();

            //如果时间戳之间已经相差了一天，则今日步数=传感器当前步数-记录的当前步数。
            Log.e(TAG, "nowTimestamp / 86400000: 当前时间:" + nowTimestamp / 86400000);

            if (nowTimestamp / 86400000 > sensorTotalStepTime / 86400000) {
                todayStep = sensorTotalStep <= eventSteps ? (int) (eventSteps - sensorTotalStep) : (int) (eventSteps);
            }
            //如果还没有跨天，则记录增量
            else {
                todayStep += sensorTotalStep <= eventSteps ? (int) (eventSteps - sensorTotalStep) : (int) (eventSteps);
            }

            sensorTotalStepTime = nowTimestamp;
            sensorTotalStep = eventSteps;
            getSharedPreferences(SP_STEP, MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_TODAY_STEP, todayStep)
                    .putInt(KEY_STEP, sensorTotalStep)
                    .putLong(KEY_STEP_TIME, sensorTotalStepTime)
                    .commit();
        }
    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
