package com.michael.iot.server;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 设备管理器
 */
public class DeviceManager {
    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
    
    // 设备ID -> 设备信息
    private final Map<String, DeviceInfo> devices = new ConcurrentHashMap<>();
    // 设备ID -> Netty Channel
    private final Map<String, Channel> deviceChannels = new ConcurrentHashMap<>();
    // 设备状态变更监听器
    private final List<DeviceStatusListener> statusListeners = new ArrayList<>();
    // 设备数据更新监听器
    private final List<DeviceDataListener> dataListeners = new ArrayList<>();

    /**
     * 设备上线
     */
    public void deviceOnline(String deviceId, String deviceName, String deviceType, 
                           String ipAddress, int port, Channel channel) {
        DeviceInfo deviceInfo = new DeviceInfo(deviceId, deviceName, deviceType);
        deviceInfo.setIpAddress(ipAddress);
        deviceInfo.setPort(port);
        deviceInfo.setStatus(DeviceStatus.ONLINE);
        deviceInfo.setConnectTime(LocalDateTime.now());
        deviceInfo.setLastHeartbeat(LocalDateTime.now());

        devices.put(deviceId, deviceInfo);
        deviceChannels.put(deviceId, channel);

        logger.info("设备上线: {}", deviceInfo);
        notifyStatusChange(deviceInfo, DeviceStatus.OFFLINE, DeviceStatus.ONLINE);
    }

    /**
     * 设备下线
     */
    public void deviceOffline(String deviceId) {
        DeviceInfo deviceInfo = devices.get(deviceId);
        if (deviceInfo != null) {
            DeviceStatus oldStatus = deviceInfo.getStatus();
            deviceInfo.setStatus(DeviceStatus.OFFLINE);
            deviceInfo.setDisconnectTime(LocalDateTime.now());

            deviceChannels.remove(deviceId);

            logger.info("设备下线: {}", deviceInfo);
            notifyStatusChange(deviceInfo, oldStatus, DeviceStatus.OFFLINE);
        }
    }

    /**
     * 更新设备心跳
     */
    public void updateHeartbeat(String deviceId) {
        DeviceInfo deviceInfo = devices.get(deviceId);
        if (deviceInfo != null) {
            deviceInfo.setLastHeartbeat(LocalDateTime.now());
        }
    }

    /**
     * 获取所有设备
     */
    public List<DeviceInfo> getAllDevices() {
        return new ArrayList<>(devices.values());
    }

    /**
     * 获取在线设备
     */
    public List<DeviceInfo> getOnlineDevices() {
        return devices.values().stream()
                .filter(device -> device.getStatus() == DeviceStatus.ONLINE)
                .collect(Collectors.toList());
    }

    /**
     * 获取离线设备
     */
    public List<DeviceInfo> getOfflineDevices() {
        return devices.values().stream()
                .filter(device -> device.getStatus() == DeviceStatus.OFFLINE)
                .collect(Collectors.toList());
    }

    /**
     * 根据设备ID获取设备信息
     */
    public DeviceInfo getDevice(String deviceId) {
        return devices.get(deviceId);
    }

    /**
     * 根据设备ID获取Channel
     */
    public Channel getDeviceChannel(String deviceId) {
        return deviceChannels.get(deviceId);
    }

    /**
     * 添加状态监听器
     */
    public void addStatusListener(DeviceStatusListener listener) {
        statusListeners.add(listener);
    }

    /**
     * 移除状态监听器
     */
    public void removeStatusListener(DeviceStatusListener listener) {
        statusListeners.remove(listener);
    }

    /**
     * 添加数据监听器
     */
    public void addDataListener(DeviceDataListener listener) {
        dataListeners.add(listener);
    }

    /**
     * 移除数据监听器
     */
    public void removeDataListener(DeviceDataListener listener) {
        dataListeners.remove(listener);
    }

    /**
     * 通知状态变更
     */
    private void notifyStatusChange(DeviceInfo device, DeviceStatus oldStatus, DeviceStatus newStatus) {
        for (DeviceStatusListener listener : statusListeners) {
            try {
                listener.onStatusChange(device, oldStatus, newStatus);
            } catch (Exception e) {
                logger.error("通知设备状态变更失败", e);
            }
        }
    }

    /**
     * 通知数据更新
     */
    public void notifyDataUpdate(DeviceInfo device, String data) {
        for (DeviceDataListener listener : dataListeners) {
            try {
                listener.onDataUpdate(device, data);
            } catch (Exception e) {
                logger.error("通知设备数据更新失败", e);
            }
        }
    }

    /**
     * 获取设备总数
     */
    public int getTotalDeviceCount() {
        return devices.size();
    }

    /**
     * 获取在线设备数量
     */
    public int getOnlineDeviceCount() {
        return (int) devices.values().stream()
                .filter(device -> device.getStatus() == DeviceStatus.ONLINE)
                .count();
    }

    /**
     * 获取离线设备数量
     */
    public int getOfflineDeviceCount() {
        return (int) devices.values().stream()
                .filter(device -> device.getStatus() == DeviceStatus.OFFLINE)
                .count();
    }
}
