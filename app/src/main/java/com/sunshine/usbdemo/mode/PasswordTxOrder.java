package com.sunshine.usbdemo.mode;

/**
 * 修改密码
 * Created by sunshine on 2017/2/24.
 */

public class PasswordTxOrder extends TxOrder {
    /**
     * @param type 指令类型
     */
    public PasswordTxOrder(TYPE type,byte[] bytes) {
        super(type);
        add(0, (byte) 06);
        add(bytes);
    }
}
