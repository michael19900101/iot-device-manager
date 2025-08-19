package com.michael.iot.server;

import java.time.LocalDateTime;

/**
 * 物联网设备信息
 */
public class DeviceInfo {
    private String deviceId;           // 设备ID
    private String deviceName;         // 设备名称
    private String deviceType;         // 设备类型
    private String ipAddress;          // IP地址
    private int port;                  // 端口
    private DeviceStatus status;       // 设备状态
    private LocalDateTime lastHeartbeat; // 最后心跳时间
    private LocalDateTime connectTime;   // 连接时间
    private LocalDateTime disconnectTime; // 断开时间
    private String lastData;             // 最新上报的数据
    private LocalDateTime lastDataTime;  // 数据上报时间

    public DeviceInfo(String deviceId, String deviceName, String deviceType) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.status = DeviceStatus.OFFLINE;
    }

    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(LocalDateTime connectTime) {
        this.connectTime = connectTime;
    }

    public LocalDateTime getDisconnectTime() {
        return disconnectTime;
    }

    public void setDisconnectTime(LocalDateTime disconnectTime) {
        this.disconnectTime = disconnectTime;
    }

    public String getLastData() {
        return lastData;
    }

    public void setLastData(String lastData) {
        this.lastData = lastData;
    }

    public LocalDateTime getLastDataTime() {
        return lastDataTime;
    }

    public void setLastDataTime(LocalDateTime lastDataTime) {
        this.lastDataTime = lastDataTime;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", status=" + status +
                ", lastHeartbeat=" + lastHeartbeat +
                ", connectTime=" + connectTime +
                '}';
    }
}
