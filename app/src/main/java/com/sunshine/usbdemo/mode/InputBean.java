package com.sunshine.usbdemo.mode;

public class InputBean {

    private String mac;
    private String deviceId;
    private String deviceType = "4";
    private String lockKey = "58,96,67,42,92,01,33,31,41,30,15,78,12,19,40,37";
    private String password = "000000";
    private String barcode;
    private String firmwareVersion = "1.0";
    private String chipType = "1";
    private String radioName = "nokelock";
    private String gsmVersion;
    private String electricity = "100";

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getChipType() {
        return chipType;
    }

    public void setChipType(String chipType) {
        this.chipType = chipType;
    }

    public String getRadioName() {
        return radioName;
    }

    public void setRadioName(String radioName) {
        this.radioName = radioName;
    }

    public String getGsmVersion() {
        return gsmVersion;
    }

    public void setGsmVersion(String gsmVersion) {
        this.gsmVersion = gsmVersion;
    }

    public String getElectricity() {
        return electricity;
    }

    public void setElectricity(String electricity) {
        this.electricity = electricity;
    }

    @Override
    public String toString() {
        return "InputBean{" +
                "mac='" + mac + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", lockKey='" + lockKey + '\'' +
                ", password='" + password + '\'' +
                ", barcode='" + barcode + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", chipType='" + chipType + '\'' +
                ", radioName='" + radioName + '\'' +
                ", gsmVersion='" + gsmVersion + '\'' +
                ", electricity='" + electricity + '\'' +
                '}';
    }
}
