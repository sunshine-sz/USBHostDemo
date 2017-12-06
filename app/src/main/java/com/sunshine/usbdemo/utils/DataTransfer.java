package com.sunshine.usbdemo.utils;

public class DataTransfer {
    private byte DataBuffer[];
    private int ReadPos=0;
    private int WritePos=0;
    private int BufferLen=0;
    private int DataLen=0;

    public DataTransfer(int mBufferLen){
        DataBuffer=new byte[mBufferLen];
        BufferLen=mBufferLen;
    }

    public void AddData(byte mbyte) {
        DataBuffer[WritePos]=mbyte;
        WritePos++;
        if(DataLen<BufferLen){
            DataLen++;
        }
        if(WritePos==BufferLen){
            WritePos=0;
        }
    }
    public void AddData(byte []mbyte,int len){
        int i;
        for(i=0;i<len;i++){
            AddData(mbyte[i]);
        }
    }
    public byte ReadData(){
        if(DataLen==0){
            return 0;
        }
        byte mbyte=DataBuffer[ReadPos];
        return mbyte;
    }
    public byte ReadData(int Index){
        if(DataLen<=Index){
            return 0;
        }
        byte mbyte=DataBuffer[(ReadPos+Index)%BufferLen];
        return mbyte;
    }
    public int DeleteFrontData(){
        if(DataLen==0){
            return 0;
        }
        ReadPos++;
        DataLen--;
        if(ReadPos==BufferLen){
            ReadPos=0;
        }
        return 1;
    }
    public int GetDataLen(){
        return DataLen;
    }

    public int ReadMultiData(byte [] mbuffer,int Len){
        if(Len>DataLen){
            return 0;
        }

        int i;
        for(i=0;i<Len;i++){
            mbuffer[i]=ReadData();
            DeleteFrontData();
        }
        return 1;
    }
    /**
     * 浮点转换为字节
     *
     * @param f
     * @return
     */
    public byte[] float2byte(float f) {

        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        // 翻转数组
        int len = b.length;
        // 建立一个与源数组元素类型相同的数组
        byte[] dest = new byte[len];
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;

    }

    /**
     * 字节转换为浮点
     *
     * @param b 字节（至少4个字节）
     * @param index 开始位置
     * @return
     */
    public float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }
}
