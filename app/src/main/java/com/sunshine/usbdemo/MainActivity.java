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
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fitsleep.sunshinelibrary.inter.OnItemClickListener;
import com.fitsleep.sunshinelibrary.utils.KeyboardUtils;
import com.fitsleep.sunshinelibrary.utils.Logger;
import com.fitsleep.sunshinelibrary.utils.MPermissionsActivity;
import com.fitsleep.sunshinelibrary.utils.ToastUtils;
import com.fitsleep.sunshinelibrary.view.AlertView;
import com.sunshine.usbdemo.mode.CloseBatteryTxOrder;
import com.sunshine.usbdemo.mode.GetDeviceId;
import com.sunshine.usbdemo.mode.GetTokenTxOrder;
import com.sunshine.usbdemo.mode.InputBean;
import com.sunshine.usbdemo.mode.OpenBatteryTxOrder;
import com.sunshine.usbdemo.mode.OpenLockTxOrder;
import com.sunshine.usbdemo.mode.ResetLockTxOrder;
import com.sunshine.usbdemo.utils.AESUtils;
import com.sunshine.usbdemo.utils.Config;
import com.sunshine.usbdemo.utils.DataTransfer;
import com.sunshine.usbdemo.utils.GlobalParameterUtils;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import zxing.android.view.QrCodeActivity;

public class MainActivity extends MPermissionsActivity {

    private static final String LOGTAG = MainActivity.class.getSimpleName();
    /**
     * 圆形锁
     */
    public static byte[] KEY = {58, 96, 67, 42, 92, 01, 33, 31, 41, 30, 15, 78, 12, 19, 40, 37};

    //    public static byte[] KEY = {32,87,47,82,54,75,63,71,48,80,65,88,17,99,45,43};
    private Button button, button2, button3, button4,button5,button6;
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
    private byte[] oldPassword;
    private byte[] newPasswordBytes;
    private String deviceId;
    public static final int QR_SCAN_REQUEST_CODE = 3638;
    private String barcode;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initUSB();
        isRunning = true;
        findViewById(R.id.bt_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermission(new String[]{ Manifest.permission.CAMERA}, 100);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
//                new MyThread2().start();
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
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new ResetLockTxOrder().generateString()), KEY);
                new MyThread(bytes).start();
            }
        });

        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                final AlertView mAlertViewExt = new AlertView("提示", "输入密码！", "取消", null, new String[]{"完成"}, MainActivity.this, AlertView.Style.Alert, new OnItemClickListener() {
                    @Override
                    public void onItemClick(Object o, int position) {
                        KeyboardUtils.hideSoftInput(MainActivity.this);
                        if (position == 0) {
                            final String newPassword = etName.getText().toString().trim();
                            if (newPassword.length() != 6) {
                                ToastUtils.showMessage("请输入6位数密码");
                                return;
                            }

                            final byte[] token = GlobalParameterUtils.getInstance().getToken();
                            if (null == token || token.length < 4) {
                                return;
                            }
                            byte[] oldPassword = {0x05, 0x03, 0x06, Config.password[0], Config.password[1], Config.password[2], Config.password[3], Config.password[4], Config.password[5], token[0], token[1], token[2], token[3], 0x00, 0x00, 0x00};
                            byte[] bytes = AESUtils.Encrypt(oldPassword, KEY);
                            new MyThread(bytes).start();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    char[] chars = newPassword.toCharArray();
                                    byte[] newPassword = new byte[]{0x05, 0x04, 0x06, (byte) chars[0], (byte) chars[1], (byte) chars[2], (byte) chars[3], (byte) chars[4], (byte) chars[5], token[0], token[1], token[2], token[3], 0x00, 0x00, 0x00};
                                    newPasswordBytes = new byte[]{(byte) chars[0], (byte) chars[1], (byte) chars[2], (byte) chars[3], (byte) chars[4], (byte) chars[5]};
                                    byte[] bytes = AESUtils.Encrypt(newPassword, KEY);
                                    new MyThread(bytes).start();
                                }
                            }, 1000);
                        }
                    }
                });
                ViewGroup extView = (ViewGroup) LayoutInflater.from(MainActivity.this).inflate(R.layout.alertext_form, null);
                etName = (EditText) extView.findViewById(R.id.etName);
                etName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean focus) {
                        //输入框出来则往上移动
                        boolean isOpen = imm.isActive();
                        mAlertViewExt.setMarginBottom(isOpen && focus ? 120 : 0);
                        System.out.println(isOpen);
                    }
                });
                mAlertViewExt.addExtView(extView);
                mAlertViewExt.show();
            }
        });

        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] token = GlobalParameterUtils.getInstance().getToken();
                if (null == token || token.length < 4) {
                    return;
                }
                byte[] aq_mi = {0x07, 0x01, 0x08, Config.MTX_KEY[0], Config.MTX_KEY[1], Config.MTX_KEY[2], Config.MTX_KEY[3], Config.MTX_KEY[4],Config.MTX_KEY[5], Config.MTX_KEY[6], Config.MTX_KEY[7], token[0], token[1], token[2], token[3], 0x00};
                byte[] bytes =  AESUtils.Encrypt(aq_mi,KEY);
                new MyThread(bytes).start();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        byte[] aq_mi = {0x07, 0x02, 0x08, Config.MTX_KEY[8],Config.MTX_KEY[9], Config.MTX_KEY[10], Config.MTX_KEY[11], Config.MTX_KEY[12], Config.MTX_KEY[13], Config.MTX_KEY[14], Config.MTX_KEY[15], GlobalParameterUtils.getInstance().getToken()[0], GlobalParameterUtils.getInstance().getToken()[1], GlobalParameterUtils.getInstance().getToken()[2], GlobalParameterUtils.getInstance().getToken()[3], 0x00};
                        byte[] bytes =  AESUtils.Encrypt(aq_mi,KEY);
                        new MyThread(bytes).start();
                    }
                },1000);
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

    }

    @Override
    public void permissionSuccess(int requestCode) {
        super.permissionSuccess(requestCode);
        startActivityForResult(new Intent(this, QrCodeActivity.class), QR_SCAN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST_CODE && resultCode == RESULT_OK && null != data) {
            String code = data.getStringExtra("code");
            Logger.e("####","code:"+code);
            if (!TextUtils.isEmpty(code)) {
                barcode = code;
                result.append("二维码信息："+ barcode +"\n");
                inputServer();
            }
        }
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
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.bt_password);
        button6 = (Button) findViewById(R.id.bt_key);
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
                                GlobalParameterUtils.getInstance().setToken(token);
                                result.append("token:"+AESUtils.bytes2HexString(token));
                            }
                        }

                        byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new GetDeviceId().generateString()), KEY);
                        new MyThread(bytes).start();
                    }else if (AESUtils.bytes2HexString(mingwen).startsWith("0902")){
                        byte[] x = new byte[12];
                        System.arraycopy(mingwen, 4, x, 0, 12);
                        deviceId = AESUtils.bytes2HexString(x);
                        mInfoTextView.append("\n设备编号:"+AESUtils.bytes2HexString(x));
                        result.append("\n设备编号:"+AESUtils.bytes2HexString(x));
                    }else if (AESUtils.bytes2HexString(mingwen).startsWith("0502")){
                        result.append("\n开锁反馈:"+AESUtils.bytes2HexString(mingwen));

                    }else if (AESUtils.bytes2HexString(mingwen).startsWith("050D")){
                        result.append("\n关锁反馈:"+AESUtils.bytes2HexString(mingwen));
                    }else if (AESUtils.bytes2HexString(mingwen).startsWith("0505")){
                        if (AESUtils.bytes2HexString(mingwen).startsWith("05050100")){
                            Config.password = newPasswordBytes;
                        }
                        result.append("\n修改密码反馈:"+AESUtils.bytes2HexString(mingwen));
                    }else if (AESUtils.bytes2HexString(mingwen).startsWith("0703")){
                        if (AESUtils.bytes2HexString(mingwen).startsWith("07030100")){
                            KEY = Config.MTX_KEY;
                        }
                        result.append("\n修改密钥反馈:"+AESUtils.bytes2HexString(mingwen));
                    }else if (AESUtils.bytes2HexString(mingwen).startsWith("1002")){
                        result.append("\n开启电池仓反馈:"+AESUtils.bytes2HexString(mingwen));
                    }else if (AESUtils.bytes2HexString(mingwen).startsWith("1004")){
                        result.append("\n关闭电池仓反馈:"+AESUtils.bytes2HexString(mingwen));
                    }
                    break;
            }
        }
    };

    private void inputServer() {
        InputBean bean = new InputBean();
        bean.setBarcode(barcode);
        bean.setDeviceId(deviceId);
        bean.setMac(deviceId);
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, com.alibaba.fastjson.JSON.toJSONString(bean));
        Request request = new Request.Builder()
                .url("http://app.nokelock.com:8080/newNokelock/lock/insert")
                .post(body)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e(MainActivity.class.getSimpleName(),"通讯失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String string = response.body().string();
                Logger.e(MainActivity.class.getSimpleName(),"通讯成功："+string);
                JSONObject jsonObject = JSONObject.parseObject(string);
                final String status = jsonObject.getString("status");
                if (status.equals("2000")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            result.append("入库成功\n");
                        }
                    });
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            result.append("入库失败+"+status+"\n");
                        }
                    });
                }
            }
        });
    }


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


    class PaseDataThread extends Thread{
        @Override
        public void run() {
            super.run();
            while (true){
                try {
                    sleep(500);
                    //读取数据1   两种方法读取数据
                    if (inEndpoint!=null){
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
