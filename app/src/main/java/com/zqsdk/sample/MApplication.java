package com.zqsdk.sample;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;


public class MApplication extends Application {

    private static BaseProduct product;
    private static Camera camera = null;
    private static Application app = null;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        app = this;
    }

    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();
        return product;
    }

    public static synchronized Camera getCameraInstance() {

        if (getProductInstance() == null) return null;

        if(camera == null) {

            if (getProductInstance() instanceof Aircraft) {
                camera = ((Aircraft) getProductInstance()).getCamera();

            } else if (getProductInstance() instanceof HandHeld) {
                camera = ((HandHeld) getProductInstance()).getCamera();
            }
        }

        return camera;
    }

    public static synchronized Gimbal getGimbalInstance() {

        if (getProductInstance() == null) return null;

        Gimbal gimbal = null;

        if (getProductInstance() instanceof Aircraft){
            gimbal = ((Aircraft) getProductInstance()).getGimbal();

        } else if (getProductInstance() instanceof HandHeld) {
            gimbal = ((HandHeld) getProductInstance()).getGimbal();
        }

        return gimbal;
    }

    public static Application getInstance() {
        return MApplication.app;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }
    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }
}
