package com.sunshine.usbdemo.mode;


import com.sunshine.usbdemo.exception.InvalidTypeException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 指令基类
 * Created by sunshine on 2017/2/24.
 */

public class Order {

    final private TYPE type;
    /**
     * 数据列表
     */
    final private List<Byte> datas;
    public Order(TYPE type) {
        this.type = type;
        this.datas = new ArrayList<Byte>();
    }

    public enum TYPE{

        GET_TOKEN(0x0601),
        OPEN_LOCK(0x0501),
        GET_BATTERY(0x0201),
        LOCK_STATUS(0x050e),
        RESET_LOCK(0x050c),
        RESET_PASSWORD(0x0503),
        RESET_PASSWORD2(0X0504),
        RESET_AQ(0x0a01),
        STOP_READ(0x0514),
        UPDATE_VERSION(0x0301),
        GET_DEVICE_ID(0x0901),
        OPEN_BATTERY(0x1001),
        CLOSE_BATTERY(0x1003);

        final int value;

        TYPE(int value){
            this.value = value;
        }

        public int getValue(){
            return value;
        }

        /**
         * 传入字符串，得到相应的指令类型
         *
         * @param typeStr 类型字符串
         * @throws InvalidTypeException
         * @return TYPE
         */
        public static TYPE valueOfType(String typeStr)  throws InvalidTypeException {
            try {
                int value = Integer.valueOf(typeStr, 16);
                for (TYPE type : TYPE.values()) {
                    if (type.getValue() == value) {
                        return type;
                    }
                }
            } catch (Exception e) {
                throw new InvalidTypeException();
            }
            throw new InvalidTypeException();
        }
    }

    /**
     * 获取指令类型
     * @return TYPE
     */
    public TYPE getType() {
        return type;
    }
    /**
     * 根据position获取数据
     *
     * @param position 位置
     * @return byte
     */
    public byte get(int position) {
        return datas.get(position);
    }
    /**
     * 添加数据到最后的位置
     *
     * @param data
     * @return void
     */
    protected void add(byte data) {
        datas.add(data);
    }
    /**
     * 添加数据 到指定的位置
     * @Title: add
     * @param position
     * @param data
     * @return void
     */
    protected void add(int position, byte data) {
        datas.add(position, data);
    }
    /**
     * 添加所给的数组
     * @Title: add
     * @param dataArray
     * @return void
     */
    protected void add(byte... dataArray) {
        if (dataArray != null) {
            for (int i = 0; i < dataArray.length; i++) {
                add(dataArray[i]);
            }
        }
    }
    /**
     * 添加所给的数组到指定的位置
     * @Title: add
     * @param position
     * @param dataArray
     * @return void
     */
    protected void add(int position, byte... dataArray) {
        if (dataArray != null) {
            for (int i = 0; i < dataArray.length; i++) {
                add(position + i, dataArray[i]);
            }
        }
    }
    /**
     * 从数据列表中移除指定位置的数据
     * @Title: remove
     * @param position
     * @return void
     */
    protected void remove(int position) {
        datas.remove(position);
    }
    /**
     * 设置指定位置的数据
     * @Title: set
     * @param position
     * @param value
     * @return void
     */
    protected void set(int position, byte value) {
        datas.set(position, value);
    }
    /**
     * 数据列表的长度
     * @Title: size
     * @return int
     */
    public int size() {
        return datas.size();
    }
    /**
     * 清空数据列表
     * @Title: clear
     * @return void
     */
    protected void clear() {
        datas.clear();
    }
    /**
     * 添加所给的数据列表
     * @Title: addAll
     * @param collection
     * @return void
     */
    protected void addAll(Collection<? extends Byte> collection) {
        datas.addAll(collection);
    }
    /**
     * 获取数据列表
     * @Title: getDatas
     * @return List<Byte>
     */
    protected List<Byte> getDatas() {
        return datas;
    }
    /**
     * 将byte转换为16进制字符串
     * @Title: formatByte2HexStr
     * @param value
     * @return String
     */
    public static String formatByte2HexStr(byte value) {
        return String.format("%02X", value);
    }
    @Override
    public String toString() {
        return "Order [type=" + type + ", datas=" + datas + "]";
    }
}
