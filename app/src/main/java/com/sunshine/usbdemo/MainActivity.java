package com.sunshine.usbdemo;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fitsleep.sunshinelibrary.inter.OnItemClickListener;
import com.fitsleep.sunshinelibrary.utils.KeyboardUtils;
import com.fitsleep.sunshinelibrary.utils.ToastUtils;
import com.fitsleep.sunshinelibrary.view.AlertView;
import com.sunshine.usbdemo.mode.CloseBatteryTxOrder;
import com.sunshine.usbdemo.mode.GetDeviceId;
import com.sunshine.usbdemo.mode.GetTokenTxOrder;
import com.sunshine.usbdemo.mode.OpenBatteryTxOrder;
import com.sunshine.usbdemo.mode.OpenLockTxOrder;
import com.sunshine.usbdemo.mode.ResetLockTxOrder;
import com.sunshine.usbdemo.mode.UpdateTxOrder;
import com.sunshine.usbdemo.utils.AESUtils;
import com.sunshine.usbdemo.utils.Config;
import com.sunshine.usbdemo.utils.DataTransfer;
import com.sunshine.usbdemo.utils.GlobalParameterUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import static com.sunshine.usbdemo.mode.Order.formatByte2HexStr;

public class MainActivity extends AppCompatActivity {

    private static final String LOGTAG = MainActivity.class.getSimpleName();
    /**
     * 圆形锁
     */
    public static byte[] KEY = {58, 96, 67, 42, 92, 01, 33, 31, 41, 30, 15, 78, 12, 19, 40, 37};

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
    private byte[] oldPassword;
    private byte[] newPasswordBytes;
    private Button btUpdate;
    private ProgressBar progressBar;
    private byte[] mFileBuffer = null;
    private int currentProgress = 0;
    private Button btLoadData;
    private boolean isUpdate = false;

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
                byte[] aq_mi = {0x07, 0x01, 0x08, Config.MTX_KEY[0], Config.MTX_KEY[1], Config.MTX_KEY[2], Config.MTX_KEY[3], Config.MTX_KEY[4], Config.MTX_KEY[5], Config.MTX_KEY[6], Config.MTX_KEY[7], token[0], token[1], token[2], token[3], 0x00};
                byte[] bytes = AESUtils.Encrypt(aq_mi, KEY);
                new MyThread(bytes).start();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        byte[] aq_mi = {0x07, 0x02, 0x08, Config.MTX_KEY[8], Config.MTX_KEY[9], Config.MTX_KEY[10], Config.MTX_KEY[11], Config.MTX_KEY[12], Config.MTX_KEY[13], Config.MTX_KEY[14], Config.MTX_KEY[15], GlobalParameterUtils.getInstance().getToken()[0], GlobalParameterUtils.getInstance().getToken()[1], GlobalParameterUtils.getInstance().getToken()[2], GlobalParameterUtils.getInstance().getToken()[3], 0x00};
                        byte[] bytes = AESUtils.Encrypt(aq_mi, KEY);
                        new MyThread(bytes).start();
                    }
                }, 1000);
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

        findViewById(R.id.bt_send_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                final AlertView mAlertViewExt = new AlertView("输入指令", "只需要输入token之前的字节即可！", "取消", null, new String[]{"完成"}, MainActivity.this, AlertView.Style.Alert, new OnItemClickListener() {
                    @Override
                    public void onItemClick(Object o, int position) {
                        KeyboardUtils.hideSoftInput(MainActivity.this);
                        if (position == 0) {
                            final String newPassword = etName.getText().toString().trim();
                            if (TextUtils.isEmpty(newPassword) || newPassword.length() % 2 != 0) {
                                ToastUtils.showMessage("格式不对");
                                return;
                            }
                            final byte[] token = GlobalParameterUtils.getInstance().getToken();
                            if (null == token || token.length < 4) {
                                return;
                            }
                            Random random = new Random();
                            StringBuilder builder = new StringBuilder();
                            builder.append(newPassword);
                            //添加token
                            for (int i = 0; i < 4; i++) {
                                builder.append(formatByte2HexStr(GlobalParameterUtils.getInstance().getToken()[i]));
                            }
                            // 如果数据总位数不够，在数据后面补0
                            for (int i = builder.length() / 2; i < 16; i++) {
                                builder.append(formatByte2HexStr((byte) random.nextInt(127)));
                            }
                            // 生成字符串形式的指令
                            String orderStr = builder.toString();
                            byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(orderStr), KEY);
                            new MyThread(bytes).start();
                        }
                    }
                });
                ViewGroup extView = (ViewGroup) LayoutInflater.from(MainActivity.this).inflate(R.layout.alertext_form, null);
                etName = (EditText) extView.findViewById(R.id.etName);
                etName.setInputType(InputType.TYPE_CLASS_TEXT);
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


        btLoadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });

        btUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFileBuffer == null || mFileBuffer.length == 0) {
                    ToastUtils.showMessage("还未成功加载文件");
                    return;
                }
                byte[] data = new byte[7];
                data[0] = 0x01;
                data[1] = 0x01;
                data[2] = mFileBuffer[0];
                data[3] = mFileBuffer[1];
                data[4] = mFileBuffer[2];
                data[5] = mFileBuffer[3];
                data[6] = mFileBuffer[4];
                currentProgress = 5;
                byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new UpdateTxOrder(data).generateString()), KEY);
                new MyThread(bytes).start();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data == null) return;
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())) {//使用第三方应用打开
                String path = uri.getPath();
                mInfoTextView.append("\n文件路径：" + path);
                if (loadFile(path)) {

                }
                return;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                String path = getPath(this, uri);
                mInfoTextView.append("\n文件路径：" + path);
                if (loadFile(path)) {

                }
            } else {//4.4以下下系统调用方法
                String path = getRealPathFromURI(uri);
                mInfoTextView.append("\n文件路径：" + path);
                if (loadFile(path)) {

                }
            }
        }
    }


    private boolean loadFile(String path) {
        try {
            //加载文件
            File file = new File(path);
            InputStream inputStream = new FileInputStream(file);
            mFileBuffer = new byte[(int) file.length()];
            inputStream.close();
            //读取文件数据
            InputStream stream;
            File f = new File(path);
            stream = new FileInputStream(f);
            stream.read(mFileBuffer, 0, mFileBuffer.length);
            stream.close();
            mInfoTextView.append("\n 升级文件大小:" + mFileBuffer.length + "字节");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
        btUpdate = (Button) findViewById(R.id.bt_update_data);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        btLoadData = (Button) findViewById(R.id.bt_load_data);
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
                                result.append("token:" + AESUtils.bytes2HexString(token));
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
                            Config.password = newPasswordBytes;
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
                        new UpdateThread().start();
                    } else if (AESUtils.bytes2HexString(mingwen).startsWith("11020101")) {
                        isUpdate = false;
                        result.append("\n拒绝升级:" + AESUtils.bytes2HexString(mingwen));
                    }
                    break;
                case 0x99:
                    int current = msg.arg1;
                    float progress = current*1.0f/mFileBuffer.length*1.0f*100;
                    progressBar.setProgress((int) progress);
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

    private int currentPkg = 0;

    class UpdateThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (isUpdate) {
                try {
                    if (currentProgress < mFileBuffer.length) {
                        //延迟500ms
                        Thread.sleep(500);
                        //合集指令
                        byte[] sendData = new byte[1026];
                        sendData[0] = (byte) currentPkg;
                        int diff = mFileBuffer.length - currentProgress;
                        System.arraycopy(mFileBuffer, currentProgress, sendData, 0, diff >= 1024 ? 1024 : diff);
                        int crc = 0;
                        for (int i = 0; i < sendData.length; i++) {
                            crc ^= sendData[i];
                        }
                        sendData[1025] = (byte) crc;
                        new MyThread(sendData).start();
                        currentPkg++;
                        currentProgress += (diff >= 1024 ? 1024 : diff);
                        Message message = handler.obtainMessage();
                        message.what = 0x99;
                        message.arg1 = currentProgress;
                        handler.sendMessage(message);

                    } else {
                        isUpdate = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            ;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
