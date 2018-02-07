package com.sunshine.usbdemo.mode;

import com.sunshine.usbdemo.utils.Config;

/**
 * 作者: Sunshine
 * 时间: 2018/1/21.
 * 邮箱: 44493547@qq.com
 * 描述:
 */

public class OpenBatteryTxOrder extends TxOrder {

    public OpenBatteryTxOrder() {
        super(TYPE.OPEN_BATTERY);
        byte[] bytes = {0x06, Config.password[0], Config.password[1], Config.password[2], Config.password[3], Config.password[4], Config.password[5]};
        add(bytes);
    }
}
