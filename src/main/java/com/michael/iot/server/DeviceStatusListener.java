package com.michael.iot.server;

/**
 * 设备状态监听器接口
 */
public interface DeviceStatusListener {
    /**
     * 设备状态变更回调
     * @param device 设备信息
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    void onStatusChange(DeviceInfo device, DeviceStatus oldStatus, DeviceStatus newStatus);
}






