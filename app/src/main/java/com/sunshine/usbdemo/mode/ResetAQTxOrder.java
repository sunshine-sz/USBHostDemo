package com.sunshine.usbdemo.mode;

/**
 * 重置密码和密钥
 * Created by sunshine on 2017/2/24.
 */

public class ResetAQTxOrder extends TxOrder {


    public ResetAQTxOrder() {
        super(TYPE.RESET_AQ);
        add(new byte[]{0x01,0x01});
    }
}
