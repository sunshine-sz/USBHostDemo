package com.sunshine.usbdemo.utils;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

/**
 * 作者：LiZhao
 * 时间：2017.2.8 10:46
 * 邮箱：44493547@qq.com
 * 备注：全局参数
 */
public class GlobalParameterUtils {


    private GlobalParameterUtils globalParameterUtils;
    private byte[] token = null;
    private Context context;
    private String version;
    private byte CHIPTYPE;
    private byte DEVTYPE;
    private boolean isUpdate;
    private boolean busy = false;
    private String deviceId;


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getCHIPTYPE() {
        return CHIPTYPE;
    }

    public void setCHIPTYPE(byte CHIPTYPE) {
        this.CHIPTYPE = CHIPTYPE;
    }

    public int getDEVTYPE() {
        return DEVTYPE;
    }

    public void setDEVTYPE(byte DEVTYPE) {
        this.DEVTYPE = DEVTYPE;
    }

    public boolean isUpdate() {
        return isUpdate;
    }

    public void setUpdate(boolean update) {
        isUpdate = update;
    }

    private GlobalParameterUtils(){}

    public static GlobalParameterUtils getInstance(){
        return SingletonHolder.INSTANCE;
    }

    public void setContext(Context applicationContext) {
        this.context = applicationContext;
    }

    public Context getContext() {
        return context;
    }

    public void setBusy(boolean b) {
        this.busy = b;
    }

    public boolean isBusy() {
        return busy;
    }

    public void sendBroadcast(String action, String data){
        if (null!=context){
            Intent intent = new Intent(action);
            if (!TextUtils.isEmpty(data)){
                intent.putExtra("data",data);
            }
           context.sendBroadcast(intent);
        }
    }

    private static class SingletonHolder {
        /**
         * 单例对象实例
         */
        static final GlobalParameterUtils INSTANCE = new GlobalParameterUtils();
    }
}
