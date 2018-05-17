package com.sunshine.usbdemo.mode;

public class UpdateTxOrder extends TxOrder {
    /**
     * @param type 指令类型
     */
    public UpdateTxOrder(byte[] data) {
        super(TYPE.UPDATE);
        add(data);
    }
}
