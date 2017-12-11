package com.sunshine.usbdemo.mode;

/**
 * 作者: Sunshine
 * 时间: 2017/12/8.
 * 邮箱: 44493547@qq.com
 * 描述:
 */

public class GetDeviceId extends TxOrder {
    /**
     * @param type 指令类型
     */
    public GetDeviceId() {
        super(TYPE.GET_DEVICE_ID);
        add(new byte[]{0x01,0x01});
    }
}
