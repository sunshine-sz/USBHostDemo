# USBHostDemo
USB Host简单通讯

> Android系统对USB的支持在3.1之后，并且还是需要手机支持。也就是说必须手机支持并且系统在3.1以上才可以。下面就介绍下android中USB Host的使用

1. 官方文档

	- UsbManager  USB的管理类，可以获取设备的状态，与连接的设备进行通讯
	- UsbDevice   USB设备的抽象类，它包含一个或者多个UsbInterface
	- UsbInterface	通讯接口，包含UsbEndpoint通讯接点
	- UsbEndpoint		是UsbInterface通讯通道，分读、写
	- UsbDeviceConnection 	与device建立的连接，并在endpoint传输数据。
	- UsbRequest		USB请求包，只有异步通讯的时候才用到
	- UsbConstants  USB常量类 与Linux内核的linux / usb / ch9.h

2. 使用介绍

> Usb的插入和拔出是以系统广播的形式发送的，只要我们注册这个广播即可

		 //获取USB管理器
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //注册权限广播
        IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
        //usb插入广播
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        //usb拔出广播
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);

> 在很多时候，我们需要插入设备的时候来启动应用，或者提示用户，一般都是在清单文件中的主activity里面加入过滤，然后指定类别。

		<intent-filter>
	    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
  		</intent-filter>

  		<meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
	    	android:resource="@xml/device_filter" />

	   //device_filter文件
	   <?xml version="1.0" encoding="utf-8"?>
		<resources>
		    <usb-device
		        product-id="394"
		        vendor-id="10473" />
		</resources>

> 使用流程图

![这里写图片描述](http://img.blog.csdn.net/20171204172337848?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvdTAxMDU2MjkyNQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)


### 代码实现

1. 初始化

		//获取USB管理器
		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		//注册权限广播
		IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
		usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, usbFilter);
		pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

2. 查找设备、检查权限

		try {
		     //取连接到设备上的USB设备集合
		     HashMap<String, UsbDevice> map = usbManager.getDeviceList();
		     //遍历集合取指定的USB设备
		     for(UsbDevice device : map.values()){
		          //展示设备信息
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

		     }else{
		         //询问用户是否授予程序操作USB设备的权限
		         requestPermission(usbDevice);
		     }
		 }catch (Exception e){
		     e.printStackTrace();
		     Toast.makeText(this,"设备获取异常",Toast.LENGTH_SHORT).show();
		 }

3. 数据通讯（读取、写入）

> 1. ***通讯操作需要放在其他线程中操作。***
> 2. ***UsbInterface接口以及通讯节点个数和类型是硬件设定的，使用时需要确认。***

		class SendThread extends Thread{
	        @Override
	        public void run() {
	            super.run();
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
	                Log.e("mystring", "mystring:"+mystring);
	            }
	        }
	    }

## 小结

	基本都是官方的一些现成的api，直接拿来按照流程使用即可，不同的是设备反馈的数据，有些是需要做转换的，否则接受的数据直接使用是长度正确但是数值都是为0。我就遇到了这样的问题。

	对于一般的程序员来说，我们平时调试应用就需要用数据线连接电脑，但是如果做USB开发的时候也需要连接手机，这样就无法进行实时调试了。这里推荐一个插件**ADB WIFI Connect**，可以无缝接入。

