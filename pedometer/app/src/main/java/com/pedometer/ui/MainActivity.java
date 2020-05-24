package com.pedometer.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.pedometer.BR;
import com.pedometer.IMyStepCountService;
import com.pedometer.MyApplication;
import com.pedometer.R;
import com.pedometer.StepCounterService;
import com.pedometer.client.ApiException;
import com.pedometer.client.api.StepInfoesApi;
import com.pedometer.client.model.StepInfo;
import com.pedometer.client.model.UserInfo;
import com.pedometer.databinding.*;
import com.pedometer.ui.login.LoginActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public class MainActivity extends AppCompatActivity {
    private int steps = 0;
    private ArrayList<StepInfo> stepInfos;
    private com.github.mikephil.charting.charts.LineChart stepsChart;
    private com.github.mikephil.charting.charts.LineChart distanceChart;
    private com.github.mikephil.charting.charts.LineChart caloriesChart;
    private ActivityMainBindingImpl binding;
    private IMyStepCountService mAidl;
    private Timer timer;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //连接后拿到 Binder，转换成 AIDL，在不同进程会返回个代理
            mAidl = IMyStepCountService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAidl = null;
        }
    };

    public MainActivity() {

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (binding == null) return;
                if (mAidl == null) return;
                try {
                    steps = mAidl.getSteps();
                    binding.setSteps(steps + " Step");
                    binding.setDistance(getDistance(steps) + " m");//步长=身高*0.37 单位：米。身高单位：厘米
                    binding.setCalories(getCalories(steps) + "Kcal");//卡路里，总的能量 = (体重÷2000 )*总步数
                    binding.notifyChange();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, 100);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        stepsChart = findViewById(R.id.main_activity_steps_chart);
        distanceChart = findViewById(R.id.main_activity_distance_chart);
        caloriesChart = findViewById(R.id.main_activity_calories_chart);

        initCharts(stepsChart);
        initCharts(distanceChart);
        initCharts(caloriesChart);

        Intent it = new Intent(getApplicationContext(), StepCounterService.class);
        startService(it);
        Intent intent1 = new Intent(getApplicationContext(), StepCounterService.class);
        bindService(intent1, mConnection, BIND_AUTO_CREATE);
        AsyncTask.execute(this::uploadData);
        AsyncTask.execute(this::loadChartDatas);

    }

    private SimpleDateFormat sdf = new SimpleDateFormat("M-d");

    private void initCharts(LineChart chart) {
        Description description = new Description();
        description.setEnabled(false);
        chart.setDescription(description);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter((v, axisBase) -> sdf.format(new Date((long) v)));

        Legend legend = chart.getLegend();
        //设置显示类型，LINE CIRCLE SQUARE EMPTY 等等 多种方式，查看LegendForm 即可
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(12f);
        //显示位置 左下方
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        //是否绘制在图表里面
        legend.setDrawInside(false);
        legend.setEnabled(false);
    }

    private LineData initLineDataSet(List<Entry> entrys, String label) {
        LineDataSet lineDataSet = new LineDataSet(entrys, label);
        lineDataSet.setLineWidth(1f);
        lineDataSet.setCircleRadius(3f);
        //设置曲线值的圆点是实心还是空心
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setValueTextSize(10f);
        //设置折线图填充
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFormLineWidth(1f);
        lineDataSet.setFormSize(15.f);
        lineDataSet.setDrawValues(false);
        //设置曲线展示为圆滑曲线（如果不设置则默认折线）
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        return new LineData(lineDataSet);
    }

    private int getDistance(int steps) {
        return (int) (steps * MyApplication.UserInfo.getHeight() * 0.01 * 0.37);
    }

    private int getCalories(int steps) {
        return (int) (steps * (MyApplication.UserInfo.getWeight() / 2000));
    }

    private void loadChartDatas() {
        StepInfoesApi api = new StepInfoesApi();
        //获取近30天的运动数据
        try {
            stepInfos = api.stepInfoesGet(System.currentTimeMillis() - 2592000000L - System.currentTimeMillis() % 86400000L);
            if (stepInfos == null || stepInfos.size() == 0) {
                return;
            }
            ArrayList<Entry> stepEntrys = new ArrayList<>();
            ArrayList<Entry> distanceEntrys = new ArrayList<>();
            ArrayList<Entry> caloriesEntrys = new ArrayList<>();
//            for (int i = 0; i < 30; i++) {
            for (int i = 0; i < stepInfos.size(); i++) {
                StepInfo info = stepInfos.get(i);
                stepEntrys.add(new Entry(i, info.getTodaySteps()));
                distanceEntrys.add(new Entry(i, getDistance(info.getTodaySteps())));
                caloriesEntrys.add(new Entry(i, getCalories(info.getTodaySteps())));
//                stepEntrys.add(new Entry(i, (float) (Math.random() * i * 10000)));
//                distanceEntrys.add(new Entry(i, (float) (Math.random() * i * 10000)));
//                caloriesEntrys.add(new Entry(i, (float) (Math.random() * i * 10000)));
            }
            runOnUiThread(() -> {
                stepsChart.setData(initLineDataSet(stepEntrys, "Steps"));
                distanceChart.setData(initLineDataSet(distanceEntrys, "Distance"));
                caloriesChart.setData(initLineDataSet(caloriesEntrys, "Calories"));

                stepsChart.invalidate();
                distanceChart.invalidate();
                caloriesChart.invalidate();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        timer.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, UserProfileActivity.class));
                return true;
            case R.id.action_logout:
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            default:
                return false;
        }

    }

    /**
     * 上传当前步数到服务器。
     */
    private void uploadData() {

        try {
            StepInfo stepInfo = new StepInfo();
            stepInfo.setId(0);
            stepInfo.setTimeStamp(System.currentTimeMillis());
            stepInfo.setUserId(MyApplication.UserInfo.getId());
            stepInfo.setTodaySteps(steps);
            stepInfo.setDistance((float) getDistance(steps));//步长=身高*0.37 单位：米。身高单位：厘米
            stepInfo.setCalories((float) getCalories(steps));//卡路里，总的能量 = (体重÷2000 )*总步数

            StepInfoesApi api = new StepInfoesApi();
            api.stepInfoesPostStepInfo(stepInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
