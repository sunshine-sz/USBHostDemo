package com.sunshine.usbdemo.mode;


import com.sunshine.usbdemo.utils.Config;

/**
 * 开锁指令
 * Created by sunshine on 2017/2/24.
 */

public class OpenLockTxOrder extends TxOrder {
    public OpenLockTxOrder() {
        super(TYPE.OPEN_LOCK);
        byte[] bytes = {0x06, Config.password[0], Config.password[1], Config.password[2], Config.password[3], Config.password[4], Config.password[5]};
        add(bytes);
    }
}
