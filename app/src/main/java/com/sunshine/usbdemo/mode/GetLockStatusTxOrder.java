package com.sunshine.usbdemo.mode;

/**
 * 获取锁状态
 * Created by sunshine on 2017/2/24.
 */

public class GetLockStatusTxOrder extends TxOrder {
    /**
     *  获取锁状态
     */
    public GetLockStatusTxOrder() {
        super(TYPE.LOCK_STATUS);
        add(new byte[]{0x01,0x01});
    }
}
