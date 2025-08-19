package com.michael.iot.server;

/**
 * 设备状态枚举
 */
public enum DeviceStatus {
    ONLINE("在线"),
    OFFLINE("离线"),
    CONNECTING("连接中"),
    DISCONNECTING("断开中");

    private final String description;

    DeviceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}






