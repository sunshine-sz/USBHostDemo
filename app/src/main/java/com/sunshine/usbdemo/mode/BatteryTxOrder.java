package com.sunshine.usbdemo.mode;

/**
 * 获取电量
 * Created by sunshine on 2017/2/24.
 */

public class BatteryTxOrder extends TxOrder {

    public BatteryTxOrder() {
        super(TYPE.GET_BATTERY);
        add(new byte[]{0x01,0x01});
    }
}
