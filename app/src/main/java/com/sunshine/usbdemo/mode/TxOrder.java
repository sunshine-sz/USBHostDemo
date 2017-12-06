package com.sunshine.usbdemo.mode;


import com.sunshine.usbdemo.utils.GlobalParameterUtils;

import java.util.Collection;
import java.util.Random;

/**
 * 发送指令基类
 * Created by sunshine on 2017/2/24.
 */

public class TxOrder extends Order {
    private Random random = new Random();

    /**
     * @param type 指令类型
     */
    public TxOrder(TYPE type) {
        super(type);
    }

    @Override
    public void add(byte data) {
        super.add(data);
    }

    @Override
    public void add(byte... dataArray) {
        super.add(dataArray);
    }

    @Override
    public void add(int position, byte data) {
        super.add(position, data);
    }

    @Override
    public void add(int position, byte... dataArray) {
        super.add(position, dataArray);
    }

    @Override
    public void remove(int position) {
        super.remove(position);
    }

    @Override
    public void addAll(Collection<? extends Byte> collection) {
        super.addAll(collection);
    }

    @Override
    public void clear() {
        super.clear();
    }

    /**
     * 将指令类型、数据列表按照规定的协议拼凑成字符串， 然后用来进行传输（在此项目是通过蓝牙传输）
     *
     * @return String
     * @Title: generateString
     */
    public String generateString() {
        StringBuilder builder = new StringBuilder();
        // 命令类型
        int typeValue = getType().getValue();
        int type = ((typeValue >> 8) & 0x00ff);// 命令类型高8位
        int code = ((typeValue) & 0x00ff);// 命令类型低8位
        // 拼凑命令类型
        builder.append(formatByte2HexStr((byte) type));
        builder.append(formatByte2HexStr((byte) code));

        // 拼凑数据
        for (int i = 0; i < size(); i++) {
            byte value = get(i);//获取数据
            builder.append(formatByte2HexStr(value));//拼凑数据
        }
        if (null == GlobalParameterUtils.getInstance().getToken() || GlobalParameterUtils.getInstance().getToken().length < 4) {
            GlobalParameterUtils.getInstance().sendBroadcast("","");
        }else {
            //添加token
            for (int i = 0; i < 4; i++) {
                builder.append(formatByte2HexStr(GlobalParameterUtils.getInstance().getToken()[i]));
            }
        }
        // 如果数据总位数不够，在数据后面补0
        for (int i = builder.length() / 2; i < 16; i++) {
            builder.append(formatByte2HexStr((byte) random.nextInt(127)));
        }
        // 生成字符串形式的指令
        String orderStr = builder.toString();
        return orderStr;
    }

    @Override
    public String toString() {
        return "TxOrder [generateString()=" + generateString() + "]";
    }
}
