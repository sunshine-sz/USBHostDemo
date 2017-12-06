package com.sunshine.usbdemo.mode;

/**
 * 复位
 * Created by sunshine on 2017/2/24.
 */

public class ResetLockTxOrder extends TxOrder {

    public ResetLockTxOrder() {
        super(TYPE.RESET_LOCK);
        add(new byte[]{ 0x01, 0x01});
    }
}
