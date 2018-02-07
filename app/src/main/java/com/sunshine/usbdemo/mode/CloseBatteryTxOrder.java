package com.sunshine.usbdemo.mode;

/**
 * 作者: Sunshine
 * 时间: 2018/2/7.
 * 邮箱: 44493547@qq.com
 * 描述:
 */

public class CloseBatteryTxOrder extends TxOrder {
    /**
     * @param type 指令类型
     */
    public CloseBatteryTxOrder() {
        super(TYPE.CLOSE_BATTERY);
        add(new byte[]{0x01,0x01});
    }
}
