package com.michael.iot.server;

/**
 * 设备数据监听器接口
 */
public interface DeviceDataListener {
    /**
     * 设备数据更新回调
     * @param device 设备信息
     * @param data 更新的数据
     */
    void onDataUpdate(DeviceInfo device, String data);
}






