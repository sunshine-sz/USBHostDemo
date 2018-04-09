package com.sunshine.usbdemo.utils;

import android.content.IntentFilter;

import java.util.UUID;

/**
 * 作者：LiZhao
 * 时间：2017.2.8 11:26
 * 邮箱：44493547@qq.com
 * 备注：
 */
public class Config {

    public static final String NOT_SUPPORTED = "com.sunshine.blelibrary.config.not_supported";


    public static final UUID bltServerUUID = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");
    public static final UUID readDataUUID = UUID.fromString("000036f6-0000-1000-8000-00805f9b34fb");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID writeDataUUID = UUID.fromString("000036f5-0000-1000-8000-00805f9b34fb");
    public static final UUID OAD_SERVICE_UUID = UUID.fromString("f000ffc0-0451-4000-b000-000000000000");
    public static final UUID OAD_READ_UUID = UUID.fromString("f000ffc1-0451-4000-b000-000000000000");
    public static final UUID OAD_WRITE_UUID = UUID.fromString("f000ffc2-0451-4000-b000-000000000000");

    public static final UUID bltFilterServerUUID = UUID.fromString("0000fbca-0000-1000-8000-00805f9b34fb");

    /**
     * 马蹄锁
     */
    public static byte[] MTX_KEY = {32,87,47,82,54,75,63,71,48,80,65,88,17,99,45,43};

    /**
     * 圆形锁
     */
    public static byte[] KEY = {58,96,67,42,92,01,33,31,41,30,15,78,12,19,40,37};

    public static byte[] password = {0x30, 0x30, 0x30, 0x30, 0x30, 0x30};
    public static final String TOKEN_ACTION = "com.sunshine.blelibrary.config.token_action";
    public static final String BATTERY_ACTION = "com.sunshine.blelibrary.config.battery_action";
    public static final String OPEN_ACTION = "com.sunshine.blelibrary.config.open_action";
    public static final String CLOSE_ACTION = "com.sunshine.blelibrary.config.close_action";
    public static final String LOCK_STATUS_ACTION = "com.sunshine.blelibrary.config.lock_status_action";
    public static final String PASSWORD_ACTION = "com.sunshine.blelibrary.config.password_action";
    public static final String AQ_ACTION = "com.sunshine.blelibrary.config.aq_action";
    public static final String SCAN_QR_ACTION = "com.sunshine.blelibrary.config.scan_qr_action";
    public static final String RESET_ACTION = "com.sunshine.blelibrary.config.reset_action";
    public static final String LOCK_RESULT = "com.sunshine.blelibrary.config.lock_result_action";
    public static final String SEND_AQ_ACTION = "com.sunshine.blelibrary.config.SEND_AQ_ACTION";
    public static final String UPDATE_VERSION_ACTION ="com.sunshine.blelibrary.config.UPDATE_VERSION_ACTION";
    public static final String BLE_DATA = "com.sunshine.blelibrary.config.BLE_DATA";

    public static final String UPDATE_NEXT = "com.sunshine.blelibrary.config.UPDATE_NEXT";;
    public static IntentFilter initFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TOKEN_ACTION);
        intentFilter.addAction(BATTERY_ACTION);
        intentFilter.addAction(OPEN_ACTION);
        intentFilter.addAction(CLOSE_ACTION);
        intentFilter.addAction(LOCK_STATUS_ACTION);
        intentFilter.addAction(PASSWORD_ACTION);
        intentFilter.addAction(AQ_ACTION);
        intentFilter.addAction(SCAN_QR_ACTION);
        intentFilter.addAction(RESET_ACTION);
        intentFilter.addAction(LOCK_RESULT);
        intentFilter.addAction(SEND_AQ_ACTION);
        intentFilter.addAction(UPDATE_VERSION_ACTION);
        intentFilter.addAction(UPDATE_NEXT);
        intentFilter.addAction(BLE_DATA);
        return intentFilter;
    }
}

