package com.sunshine.usbdemo;

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
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sunshine.usbdemo.utils.DataTransfer;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String LOGTAG = MainActivity.class.getSimpleName();
    /**
     * 圆形锁
     */
    public static byte[] KEY = {58,96,67,42,92,01,33,31,41,30,15,78,12,19,40,37};
    private Button button,button2;
    private TextView mInfoTextView;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent pendingIntent;
    private String mInfo = "设备列表:";
    byte[] mybuffer=new byte[1024];
    DataTransfer mydatatransfer=new DataTransfer(1024);
    private EditText editText;
    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initUSB();

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
                new MyThread3().start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUsbReceiver!=null){
            unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
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
    }

    /**
     * 初始化ui
     */
    private void initUI() {
        button = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
        mInfoTextView = (TextView) findViewById(R.id.info);
        editText = (EditText) findViewById(R.id.et_text);
        result = (TextView) findViewById(R.id.result);
    }


    private void checkDevice(){
       try {
           //取连接到设备上的USB设备集合
           HashMap<String, UsbDevice> map = usbManager.getDeviceList();
           //遍历集合取指定的USB设备
           for(UsbDevice device : map.values()){
               collectDeviceInfo(device);
               Log.e("device", "vid:"+device.getVendorId()+"   pid:"+device.getProductId()+"   "+device.getDeviceName());
               //VendorID 和 ProductID  十进制
               if(10473 == device.getVendorId() &&  394== device.getProductId()){
                   usbDevice = device;
               }
           }
           if (usbDevice==null){
               Log.e(LOGTAG,"没有获取到设备");
               return;
           }
           //程序是否有操作设备的权限
           if(usbManager.hasPermission(usbDevice)){
               //已有权限，执行读取或写入操作
//               new MyThread3().start();
           }else{
               //询问用户是否授予程序操作USB设备的权限
               requestPermission(usbDevice);
           }
       }catch (Exception e){
           e.printStackTrace();
           Toast.makeText(this,"设备获取异常",Toast.LENGTH_SHORT).show();
       }
    }

    private String mystring=new String();
    class MyThread3 extends Thread{
        @Override
        public void run() {
            super.run();
           // byte[] bytes = AESUtils.Encrypt(AESUtils.hexString2Bytes(new GetTokenTxOrder().generateString()), KEY);
            String sendText = editText.getText().toString().trim();
            if (TextUtils.isEmpty(sendText)||usbDevice==null){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"对象为空",Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            byte[] bytes = sendText.getBytes();
//            System.out.println("ded:"+AESUtils.bytes2HexString(bytes));
            UsbInterface usbInterface = usbDevice.getInterface(1);
            //USBEndpoint为读写数据所需的节点
            UsbEndpoint outEndpoint = usbInterface.getEndpoint(0);  //读数据节点
            UsbEndpoint inEndpoint = usbInterface.getEndpoint(1); //写数据节点
            UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
            connection.claimInterface(usbInterface, true);
            //发送数据
            int out = connection.bulkTransfer(outEndpoint, bytes, bytes.length, 300);
            Log.e("out", "out:"+out);
            //读取数据1   两种方法读取数据
            int ret = connection.bulkTransfer(inEndpoint, mybuffer, mybuffer.length, 300);
            Log.e("ret", "ret:"+ret);
            mydatatransfer.AddData(mybuffer,ret);
            if (ret>=0){
                int datalen=mydatatransfer.GetDataLen();
                byte[] mtmpbyte=new byte[datalen];
                if(mystring.length()>2048){
                    mystring=mystring.substring(datalen,mystring.length());
                }
                mydatatransfer.ReadMultiData(mtmpbyte, datalen);
                String tmpstring = new String(mtmpbyte);
                mystring+=tmpstring;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        result.setText(mystring);
                    }
                });
                Log.e("mystring", "mystring:"+mystring);
            }
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
                            new MyThread3().start();
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
                    Log.v(LOGTAG,"与设备断开连接");
                    mInfoTextView.setText("与设备断开连接");
                }
            }else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                //当设备插入时执行具体操作
                Log.v(LOGTAG,"设备接入");
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
}
