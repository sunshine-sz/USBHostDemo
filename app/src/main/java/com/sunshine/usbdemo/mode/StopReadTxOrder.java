package com.sunshine.usbdemo.mode;

/**
 * 停止gps读取锁状态
 * Created by sunshine on 2017/3/25.
 */

public class StopReadTxOrder extends TxOrder {
    /**
     *
     */
    public StopReadTxOrder() {
        super(TYPE.STOP_READ);
        add(new byte[]{0x01,0x01});
    }
}
