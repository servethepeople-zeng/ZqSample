package com.zqsdk.sample;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.zq.zqai.socket.main.ZQSdk;
import com.zqsdk.sample.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveVideoResolution;

import static dji.sdk.sdkmanager.LiveVideoBitRateMode.AUTO;
import static dji.sdk.sdkmanager.LiveVideoBitRateMode.find;

public class MyActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,View.OnClickListener{

    private static final String TAG = MyActivity.class.getName();
    private String[] typeArray = {"自动识别", "I串横担端挂点", "I串绝缘子串", "I串导线端挂点", "V串左横担端挂点", "V串左绝缘子串", "V串导线端挂点", "V串右绝缘子串", "V串右横担端挂点", "耐张串横担端挂点",
            "耐张串", "耐张串导线端挂点"};
    private Button button_device,button_register,button_live,button_test;
    private TextView textview_ip;
    private String ip,model;
    private Spinner sp;
    private int classId;
    private Switch aswitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        Bundle bundle = this.getIntent().getExtras();
        model = bundle.getString("model");
        ToastUtils.showToast(model);
        ArrayAdapter<String> starAdapter = new ArrayAdapter<String>(this,R.layout.item_select,typeArray);
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        sp = findViewById(R.id.spinner);
        sp.setAdapter(starAdapter);
        sp.setSelection(0);
        classId = 0;
        sp.setOnItemSelectedListener(this);

        button_register = findViewById(R.id.button_register);
        button_live = findViewById(R.id.button_live);
        button_test = findViewById(R.id.button_test);
        textview_ip = findViewById(R.id.textview_ip);
        button_device = findViewById(R.id.button_device);
        button_device.setOnClickListener(this);
        button_register.setOnClickListener(this);
        button_live.setOnClickListener(this);
        button_test.setOnClickListener(this);
        button_register.setEnabled(false);
        button_live.setEnabled(false);
        button_test.setEnabled(false);
        aswitch = findViewById(R.id.switch_log);
        aswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ZQSdk.getInstance().saveLog(isChecked);
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()){
            case R.id.spinner:
                classId = i;
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_device:
                ZQSdk.getInstance().scanDevice(this, new ZQSdk.ScanDeviceInfoListener() {
                    @Override
                    public void devicesInfo(String info) {
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(info);
                            if (jsonObject.has("ip")) {
                                ip = jsonObject.getString("ip");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textview_ip.setText("IP:"+ip);
                                        button_register.setEnabled(true);
                                    }
                                });
                                ZQSdk.getInstance().stopScan();

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            case R.id.button_register:
                ZQSdk.getInstance().receivedZqAIFeedback(ip,model, new ZQSdk.OnZQReceivedListener() {
                    @Override
                    public void checkReceivedMessage(String info) {
                        Log.d("checkReceived", info);
                    }

                    @Override
                    public void onReceivedMessage(String message) {
                        Log.d("test",message);
                        if ("connect success".equals(message)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtils.showToast("connect success");
                                    button_live.setEnabled(true);
                                }
                            });
                        }else if ("shootPhoto".equals(message)) {
                            takePhoto();
                        }else if (message.contains("/")) {
                            String[] waySplit = message.split("/");
                            if (waySplit.length == 4) {

                            } else {
                                rotate(Float.parseFloat(waySplit[0]), Float.parseFloat(waySplit[1]));
                            }

                        }else if("disconnect".equals(message)) {
                            Log.e(TAG,"AI盒子断开连接");
                        }
                    }
                });
                break;
            case R.id.button_live:
                startDJILiveShow("rtmp://" + ip + ":1935/live/stream?chnn=0");
                break;
            case R.id.button_test:
                ZQSdk.getInstance().sendRecognitionCommand(1, classId);
                ToastUtils.showToast("classId="+classId);
                break;
            default:
                break;
        }
    }

    private void startDJILiveShow(String url) {
        //禁用音频
        DJISDKManager.getInstance().getLiveStreamManager().setAudioStreamingEnabled(false);
        DJISDKManager.getInstance().getLiveStreamManager().setAudioMuted(true);
        //默认1280*720  精灵4rtk VIDEO_RESOLUTION_1280_720     御2 VIDEO_RESOLUTION_960_720
        if ("Mavic 2 Enterprise Advanced".equals(model)){
            DJISDKManager.getInstance().getLiveStreamManager().setLiveVideoResolution(LiveVideoResolution.VIDEO_RESOLUTION_960_720);
            Log.d("test","mavcie");
        }else if("Phantom 4 RTK".equals(model)){
            DJISDKManager.getInstance().getLiveStreamManager().setLiveVideoResolution(LiveVideoResolution.VIDEO_RESOLUTION_1280_720);
        }
        DJISDKManager.getInstance().getLiveStreamManager().setVideoEncodingEnabled(true);
        DJISDKManager.getInstance().getLiveStreamManager().setLiveVideoBitRateMode(AUTO);
        if (!isLiveStreamManagerOn()) {
            return;
        }
        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
            ToastUtils.showToast("already started");
            return;
        }
        new Thread() {
            @Override
            public void run() {
                DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(url);
                int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                if (result == 0) {
                    DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showToast("推流成功");
                            button_test.setEnabled(true);
                        }
                    });

                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showToast("live error result="+ result);
                        }
                    });
                }
            }
        }.start();
    }

    private boolean isLiveStreamManagerOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            ToastUtils.showToast("No live stream manager");
            return false;
        }
        return true;
    }

    public void stopDJILiveShow() {
        if (!isLiveStreamManagerOn()) {
            ZQSdk.getInstance().stopReceivedZqAI();
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().stopStream();
        ZQSdk.getInstance().stopReceivedZqAI();
    }

    private void takePhoto() {
        //停止识别
        ZQSdk.getInstance().sendRecognitionCommand(0, classId == 0?999:classId);
        Camera camera = MApplication.getCameraInstance();
        if (camera != null) {
            camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        ToastUtils.showToast("startShootPhoto=" + djiError.getDescription());
                    } else {
                        ToastUtils.showToast("ShootPhoto success");
                    }
                }
            });
        }
    }

    private void rotate(float degreeHor, float degreeVer) {
        Gimbal gimbal = MApplication.getGimbalInstance();
        if (gimbal == null) {
            return;
        }
        Rotation.Builder builder = new Rotation.Builder()
                .mode(RotationMode.RELATIVE_ANGLE)
                .yaw(degreeHor)
                .pitch(degreeVer)
                .time(0.1);

        gimbal.rotate(builder.build(), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    ToastUtils.showToast(djiError.getDescription());
                }
            }
        });
    }

}