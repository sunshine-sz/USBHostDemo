package com.sunshine.usbdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fitsleep.sunshinelibrary.utils.ToastUtils;
import com.google.gson.Gson;
import com.sunshine.usbdemo.mode.CloseBatteryTxOrder;
import com.sunshine.usbdemo.mode.DeviceInfo;
import com.sunshine.usbdemo.mode.GetDeviceId;
import com.sunshine.usbdemo.mode.GetTokenTxOrder;
import com.sunshine.usbdemo.mode.OpenBatteryTxOrder;
import com.sunshine.usbdemo.mode.OpenLockTxOrder;
import com.sunshine.usbdemo.utils.AESUtils;
import com.sunshine.usbdemo.utils.Config;
import com.sunshine.usbdemo.utils.DataTransfer;
import com.sunshine.usbdemo.utils.GlobalParameterUtils;
import com.sunshine.usbdemo.utils.HexUtil;
import com.sunshine.usbdemo.utils.OkHttpClientManager;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Request;
import zxing.android.view.QrCodeActivity;

public class MainActivity extends MPermissionsActivity {

    private static final String LOGTAG = MainActivity.class.getSimpleName();
    /**
     * 圆形锁
     */
    public static byte[] KEY = {32, 87, 47, 82, 54, 75, 63, 71, 48, 80, 65, 88, 17, 99, 45, 43};

    //    public static byte[] KEY = {32,87,47,82,54,75,63,71,48,80,65,88,17,99,45,43};
    private Button button, button2, button3, button4, button5, button6;
    private TextView mInfoTextView;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent pendingIntent;
    private String mInfo = "设备列表:";
    byte[] mybuffer = new byte[16];
    DataTransfer mydatatransfer = new DataTransfer(1024);
    private TextView result;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;
    private UsbDeviceConnection connection;
    private boolean isRunning = false;
    private EditText etName;
    private byte[] mFileBuffer = null;
    private int currentProgress = 0;
    private boolean isUpdate = false;
    public static final int QR_SCAN_REQUEST_CODE = 3638;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ToastUtils.init(getApplicationContext());
        initUI();
        initUSB();
        isRunning = true;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                checkDevice();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new OpenLockTxOrder().generateString()), KEY);
                new MyThread(bytes).start();
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new GetTokenTxOrder().generateString()), KEY);
                new MyThread(bytes).start();
            }
        });

        findViewById(R.id.bt_battery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new OpenBatteryTxOrder().generateString()), KEY);
                new MyThread(bytes).start();
            }
        });

        findViewById(R.id.bt_close_battery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new CloseBatteryTxOrder().generateString()), KEY);
                new MyThread(bytes).start();
            }
        });

        findViewById(R.id.bt_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 100);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }
        isRunning = false;
        System.exit(0);
    }

    @Override
    public void permissionSuccess(int requestCode) {
        super.permissionSuccess(requestCode);
        if (checkPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})) {
            startActivityForResult(new Intent(this, QrCodeActivity.class), QR_SCAN_REQUEST_CODE);
        }
    }

    @Override
    public void permissionFail(int requestCode) {
        super.permissionFail(requestCode);
        ToastUtils.showMessage("用户拒绝权限");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST_CODE && resultCode == RESULT_OK && null != data) {
            String code = data.getStringExtra("code");
            if (!TextUtils.isEmpty(code)) {
                Log.d(LOGTAG, "onActivityResult: " + code);
                getDeviceInfo(code);
            }
        }
    }

    private void getDeviceInfo(String code) {
        //"get", "", code, "", "", "", ""
        String url = "http://service.rocolock.com:16888/Insert";
        DeviceInfo deviceInfo = new DeviceInfo("get", "", code, "", "", "", "");
        OkHttpClientManager.postJson(url, new OkHttpClientManager.StringCallback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d(LOGTAG, "onFailure: " + e.getLocalizedMessage());
            }

            @Override
            public void onResponse(String response) {
                Log.d(LOGTAG, "onResponse: " + response);

                resetKey(response);
            }
        }, new Gson().toJson(deviceInfo));
    }

    private void resetKey(String json) {
        try {
            DeviceInfo baseSendBean = new Gson().fromJson(json, DeviceInfo.class);
            String[] split = baseSendBean.getData5().split(",");
            String[] split1 = baseSendBean.getData6().split(",");
            for (int i = 0; i < split.length; i++) {
                KEY[i] = Byte.parseByte(split[i]);
            }
            for (int i = 0; i < split1.length; i++) {
                Config.password[i] = Byte.parseByte(split1[i]);
            }
            Log.e(LOGTAG, "key:" + HexUtil.encodeHexStr(KEY));
            Log.e(LOGTAG, "password:" + HexUtil.encodeHexStr(Config.password));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUSB() {
        //获取USB管理器
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //注册权限广播
        IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);
        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        new PaseDataThread().start();
    }

    /**
     * 初始化ui
     */
    private void initUI() {
        button = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        mInfoTextView = (TextView) findViewById(R.id.info);
        result = (TextView) findViewById(R.id.result);
    }


    private void checkDevice() {
        try {
            //取连接到设备上的USB设备集合
            HashMap<String, UsbDevice> map = usbManager.getDeviceList();
            //遍历集合取指定的USB设备
            for (UsbDevice device : map.values()) {
                collectDeviceInfo(device);
                Log.e("device", "vid:" + device.getVendorId() + "   pid:" + device.getProductId() + "   " + device.getDeviceName());
                //VendorID 和 ProductID  十进制
                if (10473 == device.getVendorId() && 394 == device.getProductId()) {
                    usbDevice = device;
                }
            }
            if (usbDevice == null) {
                Log.e(LOGTAG, "没有获取到设备");
                return;
            }
            //程序是否有操作设备的权限
            if (usbManager.hasPermission(usbDevice)) {
                //已有权限，执行读取或写入操作
                UsbInterface usbInterface = usbDevice.getInterface(1);
                //USBEndpoint为读写数据所需的节点
                //读数据节点
                outEndpoint = usbInterface.getEndpoint(0);
                //写数据节点
                inEndpoint = usbInterface.getEndpoint(1);
                connection = usbManager.openDevice(usbDevice);
                connection.claimInterface(usbInterface, true);
            } else {
                //询问用户是否授予程序操作USB设备的权限
                requestPermission(usbDevice);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "设备获取异常", Toast.LENGTH_SHORT).show();
        }
    }


    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(LOGTAG, "permission is denied");
            if (action.equals(ACTION_USB_PERMISSION)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbDevice != null) {
                            mInfo = "";
                            collectDeviceInfo(usbDevice);
                            mInfoTextView.setText(String.format("权限获取成功，设备信息：%s", mInfo));
                            //读取usbDevice里的内容
//                            new MyThread3().start();
                        }
                    } else {
                        mInfoTextView.setText("权限被拒绝了");
                        Log.v(LOGTAG, "permission is denied");
                    }
                }
            } else if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbDevice != null) {
                    //close connection
                    Log.v(LOGTAG, "与设备断开连接");
                    mInfoTextView.setText("与设备断开连接");
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                //当设备插入时执行具体操作
                Log.v(LOGTAG, "设备接入");
                mInfoTextView.setText("设备接入");
            }
        }
    };


    private void collectDeviceInfo(UsbDevice usbDevice) {
        mInfo += "\n" +
                "DeviceID: " + usbDevice.getDeviceId() + "\n" +
                "DeviceName: " + usbDevice.getDeviceName() + "\n" +
                "DeviceClass: " + usbDevice.getDeviceClass() + " - "
                + translateDeviceClass(usbDevice.getDeviceClass()) + "\n" +
                "DeviceSubClass: " + usbDevice.getDeviceSubclass() + "\n" +
                "VendorID: " + usbDevice.getVendorId() + "\n" +
                "ProductID: " + usbDevice.getProductId() + "\n";

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInfoTextView.setText(mInfo);
            }
        });
    }

    private String translateDeviceClass(int deviceClass) {
        switch (deviceClass) {
            case UsbConstants.USB_CLASS_APP_SPEC:
                return "Application specific USB class";
            case UsbConstants.USB_CLASS_AUDIO:
                return "USB class for audio devices";
            case UsbConstants.USB_CLASS_CDC_DATA:
                return "USB class for CDC devices (communications device class)";
            case UsbConstants.USB_CLASS_COMM:
                return "USB class for communication devices";
            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "USB class for content security devices";
            case UsbConstants.USB_CLASS_CSCID:
                return "USB class for content smart card devices";
            case UsbConstants.USB_CLASS_HID:
                return "USB class for human interface devices (for example, mice and keyboards)";
            case UsbConstants.USB_CLASS_HUB:
                return "USB class for USB hubs";
            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "USB class for mass storage devices";
            case UsbConstants.USB_CLASS_MISC:
                return "USB class for wireless miscellaneous devices";
            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "USB class indicating that the class is determined on a per-interface basis";
            case UsbConstants.USB_CLASS_PHYSICA:
                return "USB class for physical devices";
            case UsbConstants.USB_CLASS_PRINTER:
                return "USB class for printers";
            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "USB class for still image devices (digital cameras)";
            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor specific USB class";
            case UsbConstants.USB_CLASS_VIDEO:
                return "USB class for video devices";
            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "USB class for wireless controller devices";
            default:
                return "Unknown USB class!";

        }
    }

    public boolean hasPermission(UsbDevice device) {
        return usbManager.hasPermission(device);
    }

    public void requestPermission(UsbDevice usbDevice) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, pendingIntent);
    }


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    break;
                case 1://监听的数据
                    int datalen = mydatatransfer.GetDataLen();
                    byte[] mtmpbyte = new byte[datalen];
                    mydatatransfer.ReadMultiData(mtmpbyte, datalen);
                    byte[] mingwen = AESUtils.Decrypt(mtmpbyte, KEY);
                    for (byte i : mingwen) {
                        Log.e("decrypt", "i:" + i);
                    }
                    Log.e("mystring", "mystring:" + AESUtils.bytes2HexString(mingwen));
                    if (AESUtils.bytes2HexString(mingwen).startsWith("0602")) {
                        if (mingwen != null && mingwen.length == 16) {
                            if (mingwen[0] == 0x06 && mingwen[1] == 0x02) {
                                byte[] token = new byte[4];
                                token[0] = mingwen[3];
                                token[1] = mingwen[4];
                                token[2] = mingwen[5];
                                token[3] = mingwen[6];
                                byte[] version = new byte[2];
                                version[0] = mingwen[7];
                                version[1] = mingwen[8];
                                GlobalParameterUtils.getInstance().setToken(token);
                                result.append("token:" + AESUtils.bytes2HexString(token) + "版本号:" + AESUtils.bytes2HexString(version));
                            }
                        }

                        byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new GetDeviceId().generateString()), KEY);
                        new MyThread(bytes).start();
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("0902")) {
                        byte[] x = new byte[12];
                        System.arraycopy(mingwen, 4, x, 0, 12);
                        mInfoTextView.append("\n设备编号:" + AESUtils.bytes2HexString(x));
                        result.append("\n设备编号:" + AESUtils.bytes2HexString(x));
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("0502")) {
                        result.append("\n开锁反馈:" + AESUtils.bytes2HexString(mingwen));
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("050D")) {
                        result.append("\n关锁反馈:" + AESUtils.bytes2HexString(mingwen));
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("0505")) {
                        if (AESUtils.bytes2HexString(mingwen).startsWith("05050100")) {

                        }
                        result.append("\n修改密码反馈:" + AESUtils.bytes2HexString(mingwen));
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("0703")) {
                        if (AESUtils.bytes2HexString(mingwen).startsWith("07030100")) {
                            KEY = Config.MTX_KEY;
                        }
                        result.append("\n修改密钥反馈:" + AESUtils.bytes2HexString(mingwen));
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("1002")) {
                        result.append("\n开启电池仓反馈:" + AESUtils.bytes2HexString(mingwen));
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("1004")) {
                        result.append("\n关闭电池仓反馈:" + AESUtils.bytes2HexString(mingwen));
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("11020100")) {
                        result.append("\n准备升级:" + AESUtils.bytes2HexString(mingwen));
                        isUpdate = true;
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("11020101")) {
                        isUpdate = false;
                        result.append("\n拒绝升级:" + AESUtils.bytes2HexString(mingwen));
                    }
                    break;
            }
        }
    };


    class MyThread extends Thread {

        byte[] bytes;

        public MyThread(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public void run() {
            super.run();
            try {
                sleep(1000);
                //发送数据
                int out = connection.bulkTransfer(outEndpoint, bytes, bytes.length, 3000);
                Log.e("out", "out:" + out);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    class PaseDataThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    sleep(500);
                    //读取数据1   两种方法读取数据
                    if (inEndpoint != null) {
                        int ret = connection.bulkTransfer(inEndpoint, mybuffer, mybuffer.length, 3000);
                        Log.e("ret", "ret:" + ret);
                        mydatatransfer.AddData(mybuffer, ret);
                        if (ret >= 0) {
                            handler.sendEmptyMessage(1);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
