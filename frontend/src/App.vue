<template>
  <div id="app">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1><el-icon><Monitor /></el-icon> 物联网设备管理系统</h1>
          <div class="connection-status">
            <el-tag :type="wsConnected ? 'success' : 'danger'" size="small">
              <el-icon><Connection /></el-icon>
              {{ wsConnected ? '已连接' : '未连接' }}
            </el-tag>
            <el-button 
              v-if="!wsConnected" 
              type="primary" 
              size="small" 
              @click="resetWebSocketConnection"
              style="margin-left: 10px;"
            >
              <el-icon><Refresh /></el-icon>
              重连
            </el-button>
          </div>
        </div>
      </el-header>
      
      <el-main>
        <div class="stats-container">
          <!-- 统计卡片 -->
          <el-card class="stat-card">
            <template #header>
              <div class="card-header">
                <span>设备总数</span>
              </div>
            </template>
            <div class="stat-number">{{ deviceStats.total }}</div>
          </el-card>
          
          <el-card class="stat-card">
            <template #header>
              <div class="card-header">
                <span>在线设备</span>
              </div>
            </template>
            <div class="stat-number online">{{ deviceStats.online }}</div>
          </el-card>
          
          <el-card class="stat-card">
            <template #header>
              <div class="card-header">
                <span>离线设备</span>
              </div>
            </template>
            <div class="stat-number offline">{{ deviceStats.offline }}</div>
          </el-card>
          
          
        </div>

        <el-row :gutter="20" style="margin-top: 20px;">
          <!-- 设备列表（包含筛选器） -->
          <el-col :span="24">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>设备列表</span>
                  <el-button type="primary" @click="refreshDevices">
                    <el-icon><Refresh /></el-icon>
                    刷新
                  </el-button>
                </div>
              </template>
              
              <!-- 筛选器 -->
              <div class="filter-section">
                <el-form :model="filterForm" class="filter-form">
                  <el-row :gutter="20">
                    <el-col :xs="24" :sm="12" :md="6" :lg="6">
                      <el-form-item label="状态">
                        <el-select v-model="filterForm.status" placeholder="选择状态" clearable style="width: 100%">
                          <el-option label="全部" value=""></el-option>
                          <el-option label="在线" value="ONLINE"></el-option>
                          <el-option label="离线" value="OFFLINE"></el-option>
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12" :md="6" :lg="6">
                      <el-form-item label="设备类型">
                        <el-select v-model="filterForm.deviceType" placeholder="选择设备类型" clearable style="width: 100%">
                          <el-option label="全部" value=""></el-option>
                          <el-option label="传感器" value="传感器"></el-option>
                          <el-option label="控制器" value="控制器"></el-option>
                          <el-option label="摄像头" value="摄像头"></el-option>
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12" :md="6" :lg="6">
                      <el-form-item label="设备名称">
                        <el-input v-model="filterForm.deviceName" placeholder="输入设备名称" clearable></el-input>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12" :md="6" :lg="6">
                      <el-form-item>
                        <el-button type="primary" @click="applyFilter" style="margin-right: 10px;">筛选</el-button>
                        <el-button @click="resetFilter">重置</el-button>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>
              </div>
              
              <el-table :data="paginatedDevices" style="width: 100%" v-loading="loading">
                <el-table-column prop="deviceId" label="设备ID" width="180"></el-table-column>
                <el-table-column prop="deviceName" label="设备名称" width="150"></el-table-column>
                <el-table-column prop="deviceType" label="设备类型" width="120"></el-table-column>
                <el-table-column prop="ipAddress" label="IP地址" width="140"></el-table-column>
                <el-table-column prop="port" label="端口" width="80"></el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.status === 'ONLINE' ? 'success' : 'danger'">
                      {{ scope.row.status === 'ONLINE' ? '在线' : '离线' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="connectTime" label="连接时间" width="180">
                  <template #default="scope">
                    {{ formatTime(scope.row.connectTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="lastHeartbeat" label="最后心跳" width="180">
                  <template #default="scope">
                    {{ formatTime(scope.row.lastHeartbeat) }}
                  </template>
                </el-table-column>
                <el-table-column label="最新数据" width="200">
                  <template #default="scope">
                    <div v-if="scope.row.lastData" class="table-data">
                      <div class="data-preview">{{ formatDataPreview(scope.row.lastData) }}</div>
                      <div class="data-time">{{ formatTime(scope.row.lastDataTime) }}</div>
                    </div>
                    <span v-else class="no-data">暂无数据</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="scope">
                    <el-button size="small" @click="viewDevice(scope.row)">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
              
              <!-- 分页组件 -->
              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="filteredDevices.length"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>

    <!-- 设备详情对话框 -->
    <el-dialog v-model="deviceDialogVisible" title="设备详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备ID">{{ selectedDevice?.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ selectedDevice?.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ selectedDevice?.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedDevice?.status === 'ONLINE' ? 'success' : 'danger'">
            {{ selectedDevice?.status === 'ONLINE' ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ selectedDevice?.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ selectedDevice?.port }}</el-descriptions-item>
        <el-descriptions-item label="连接时间">{{ formatTime(selectedDevice?.connectTime) }}</el-descriptions-item>
        <el-descriptions-item label="最后心跳">{{ formatTime(selectedDevice?.lastHeartbeat) }}</el-descriptions-item>
        <el-descriptions-item label="断开时间" v-if="selectedDevice?.disconnectTime">
          {{ formatTime(selectedDevice?.disconnectTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="最新数据" v-if="selectedDevice?.lastData">
          <div class="device-data">
            <div class="data-content">{{ selectedDevice?.lastData }}</div>
            <div class="data-time">{{ formatTime(selectedDevice?.lastDataTime) }}</div>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 通知 -->
    <el-notification
      v-model:visible="notificationVisible"
      :title="notificationTitle"
      :message="notificationMessage"
      :type="notificationType"
      :duration="3000"
    />
    
    <!-- 设备离线弹窗提示 -->
    <el-dialog
      v-model="offlineAlertVisible"
      title="设备离线提醒"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      center
      @close="handleOfflineAlertClose"
    >
      <div class="offline-alert-content">
        <el-icon class="offline-icon" color="#E6A23C" size="48">
          <Warning />
        </el-icon>
        <div class="offline-message">
          <h3>{{ offlineDeviceInfo.deviceName }} 设备已离线</h3>
          <p>设备ID: {{ offlineDeviceInfo.deviceId }}</p>
          <p>离线时间: {{ formatTime(offlineDeviceInfo.disconnectTime) }}</p>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-checkbox v-model="tempEnableOfflineAlert" size="small">
            不再显示离线提醒
          </el-checkbox>
          <el-button type="primary" @click="confirmOfflineAlert">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

export default {
  name: 'App',
  setup() {
    // 响应式数据
    const wsConnected = ref(false)
    const devices = ref([])
    const loading = ref(false)
    const deviceDialogVisible = ref(false)
    const selectedDevice = ref(null)
    const notificationVisible = ref(false)
    const notificationTitle = ref('')
    const notificationMessage = ref('')
    const notificationType = ref('info')
    
    // 设备离线提示相关
    const offlineAlertVisible = ref(false)
    const offlineDeviceInfo = ref({})
    const enableOfflineAlert = ref(true) // 是否启用离线设备弹窗提示
    const tempEnableOfflineAlert = ref(true) // 临时存储用户的选择
    

    
    // 分页相关数据
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    // WebSocket连接相关数据
    const wsReconnectAttempts = ref(0) // 重连尝试次数
    const maxReconnectAttempts = 4     // 最大重连尝试次数
    const hasShownError = ref(false)   // 是否已显示错误提示

    // 筛选表单
    const filterForm = reactive({
      status: '',
      deviceType: '',
      deviceName: ''
    })

    // WebSocket连接
    let ws = null

    // 计算属性
    const deviceStats = computed(() => {
      const total = devices.value.length
      const online = devices.value.filter(d => d.status === 'ONLINE').length
      const offline = total - online
      return { total, online, offline }
    })

    const filteredDevices = computed(() => {
      return devices.value.filter(device => {
        if (filterForm.status && device.status !== filterForm.status) return false
        if (filterForm.deviceType && device.deviceType !== filterForm.deviceType) return false
        if (filterForm.deviceName && !device.deviceName.includes(filterForm.deviceName)) return false
        return true
      })
    })

    const paginatedDevices = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredDevices.value.slice(start, end)
    })



    // 方法
    const connectWebSocket = (isReconnect = false) => {
      // 检查重连次数限制
      if (wsReconnectAttempts.value >= maxReconnectAttempts) {
        // 只在第一次达到限制时显示错误提示
        if (!hasShownError.value) {
          ElMessage.error(`WebSocket连接失败，已尝试${maxReconnectAttempts}次，请检查服务器状态`)
          hasShownError.value = true
        }
        return
      }
      
      if (isReconnect) {
        wsReconnectAttempts.value++
        console.log(`WebSocket重连尝试 ${wsReconnectAttempts.value}/${maxReconnectAttempts}`)
      } else {
        wsReconnectAttempts.value++
        console.log(`WebSocket连接尝试 ${wsReconnectAttempts.value}/${maxReconnectAttempts}`)
      }

      try {
        ws = new WebSocket('ws://localhost:8889/ws')
        
        ws.onopen = () => {
          wsConnected.value = true
          // 重置重连计数和错误提示标志
          wsReconnectAttempts.value = 0
          hasShownError.value = false
          ElMessage.success('WebSocket连接成功')
          // 连接成功后请求设备列表
          requestDeviceList()
        }
        
        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            handleWebSocketMessage(data)
          } catch (error) {
            console.error('解析WebSocket消息失败:', error)
          }
        }
        
        ws.onclose = () => {
          wsConnected.value = false
          // 尝试重连
          setTimeout(() => connectWebSocket(true), 3000)
        }
        
        ws.onerror = (error) => {
          console.error('WebSocket错误:', error)
          wsConnected.value = false
          // 连接错误时也尝试重连
          if (isReconnect) {
            setTimeout(() => connectWebSocket(true), 3000)
          }
        }
      } catch (error) {
        console.error('连接WebSocket失败:', error)
        // 捕获到异常时也尝试重连
        if (isReconnect) {
          setTimeout(() => connectWebSocket(true), 3000)
        }
      }
    }

    const handleWebSocketMessage = (data) => {
      switch (data.type) {
        case 'DEVICE_STATUS_CHANGE':
          handleDeviceStatusChange(data)
          break
        case 'DEVICE_LIST_UPDATE':
          handleDeviceListUpdate(data)
          break
        case 'DEVICE_DATA_UPDATE':
          handleDeviceDataUpdate(data)
          break

        default:
          console.log('未知消息类型:', data.type)
      }
    }

    const handleDeviceStatusChange = (data) => {
      // 更新设备状态
      const deviceIndex = devices.value.findIndex(d => d.deviceId === data.deviceId)
      if (deviceIndex !== -1) {
        const oldStatus = devices.value[deviceIndex].status
        devices.value[deviceIndex].status = data.newStatus
        if (data.newStatus === 'ONLINE') {
          devices.value[deviceIndex].connectTime = new Date().toISOString()
        } else {
          devices.value[deviceIndex].disconnectTime = new Date().toISOString()
        }
        
        // 如果设备从在线变为离线，显示弹窗提示
        if (oldStatus === 'ONLINE' && data.newStatus === 'OFFLINE' && enableOfflineAlert.value) {
          // 显示弹窗提示
          offlineDeviceInfo.value = {
            deviceName: data.deviceName,
            deviceId: data.deviceId,
            disconnectTime: new Date().toISOString()
          }
          // 重置临时选择为当前设置
          tempEnableOfflineAlert.value = enableOfflineAlert.value
          offlineAlertVisible.value = true
          
          // 同时显示消息提示
          ElMessage({
            message: `${data.deviceName} 设备已离线`,
            type: 'warning',
            duration: 3000,
            showClose: true
          })
        }
        
        // 如果设备从离线变为在线，显示上线提示
        if (oldStatus === 'OFFLINE' && data.newStatus === 'ONLINE') {
          ElMessage({
            message: `${data.deviceName} 设备已上线`,
            type: 'success',
            duration: 3000,
            showClose: true
          })
        }
      }

      // 显示通知
      showNotification(
        '设备状态变更',
        `设备 ${data.deviceName} (${data.deviceId}) ${data.newStatus === 'ONLINE' ? '上线' : '下线'}`,
        data.newStatus === 'ONLINE' ? 'success' : 'warning'
      )
    }

    const handleDeviceListUpdate = (data) => {
      if (data.devices && data.devices.length > 0) {
        // 更新设备列表，保持现有设备的引用以触发响应式更新
        data.devices.forEach(newDevice => {
          const existingIndex = devices.value.findIndex(d => d.deviceId === newDevice.deviceId)
          if (existingIndex !== -1) {
            // 更新现有设备的所有字段
            const existingDevice = devices.value[existingIndex]
            Object.assign(existingDevice, newDevice)
          } else {
            // 添加新设备
            devices.value.push(newDevice)
          }
        })
        
        // 移除不在新列表中的设备
        const newDeviceIds = data.devices.map(d => d.deviceId)
        devices.value = devices.value.filter(d => newDeviceIds.includes(d.deviceId))
      } else {
        devices.value = []
      }
    }

    const handleDeviceDataUpdate = (data) => {
      // 更新设备数据
      const deviceIndex = devices.value.findIndex(d => d.deviceId === data.deviceId)
      if (deviceIndex !== -1) {
        const device = devices.value[deviceIndex]
        device.lastData = data.data
        device.lastDataTime = new Date().toISOString()
        
        // 显示数据更新通知
        showNotification(
          '设备数据更新',
          `设备 ${data.deviceName} (${data.deviceId}) 上报了新数据`,
          'info'
        )
      }
    }



    const showNotification = (title, message, type = 'info') => {
      notificationTitle.value = title
      notificationMessage.value = message
      notificationType.value = type
      notificationVisible.value = true
    }

    const applyFilter = () => {
      // 筛选逻辑已在computed中实现
      // 应用筛选时回到第一页
      currentPage.value = 1
    }

    const resetFilter = () => {
      filterForm.status = ''
      filterForm.deviceType = ''
      filterForm.deviceName = ''
      // 重置筛选时回到第一页
      currentPage.value = 1
    }

    const handleSizeChange = (newSize) => {
      pageSize.value = newSize
      currentPage.value = 1 // 切换每页显示数量时回到第一页
    }

    const handleCurrentChange = (newPage) => {
      currentPage.value = newPage
    }

    const requestDeviceList = () => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'REQUEST_DEVICE_LIST' }))
      }
    }

    const resetWebSocketConnection = () => {
      // 重置重连计数和错误提示标志
      wsReconnectAttempts.value = 0
      hasShownError.value = false
      // 关闭现有连接
      if (ws) {
        ws.close()
      }
      // 重新连接
      connectWebSocket()
    }

    const refreshDevices = () => {
      loading.value = true
      requestDeviceList()
      setTimeout(() => {
        loading.value = false
      }, 1000)
    }

    const viewDevice = (device) => {
      selectedDevice.value = device
      deviceDialogVisible.value = true
    }

    const formatTime = (timeStr) => {
      if (!timeStr) return '-'
      return new Date(timeStr).toLocaleString('zh-CN')
    }

    const formatDataPreview = (dataStr) => {
      if (!dataStr) return ''
      try {
        const data = JSON.parse(dataStr)
        const preview = []
        if (data.temperature !== undefined) preview.push(`温度: ${data.temperature}°C`)
        if (data.humidity !== undefined) preview.push(`湿度: ${data.humidity}%`)
        return preview.join(', ')
      } catch (e) {
        return dataStr.substring(0, 30) + (dataStr.length > 30 ? '...' : '')
      }
    }

    // 确认离线提醒设置
    const confirmOfflineAlert = () => {
      enableOfflineAlert.value = tempEnableOfflineAlert.value
      offlineAlertVisible.value = false
    }

    // 处理弹框关闭事件
    const handleOfflineAlertClose = () => {
      // 当用户直接关闭弹框时，保持原有设置不变
      tempEnableOfflineAlert.value = enableOfflineAlert.value
      offlineAlertVisible.value = false
    }







    // 生命周期
    onMounted(() => {
      connectWebSocket()
    })

    onUnmounted(() => {
      if (ws) {
        ws.close()
      }
    })

    return {
      wsConnected,
      devices,
      loading,
      deviceDialogVisible,
      selectedDevice,
      notificationVisible,
      notificationTitle,
      notificationMessage,
      notificationType,
      filterForm,
      deviceStats,
      filteredDevices,
      paginatedDevices,
      currentPage,
      pageSize,
      wsReconnectAttempts,
      maxReconnectAttempts,
      hasShownError,
      applyFilter,
      resetFilter,
      handleSizeChange,
      handleCurrentChange,
      refreshDevices,
      resetWebSocketConnection,
      viewDevice,
      formatTime,
      formatDataPreview,
      offlineAlertVisible,
      offlineDeviceInfo,
      enableOfflineAlert,
      tempEnableOfflineAlert,
      confirmOfflineAlert,
      handleOfflineAlertClose
    }
  }
}
</script>

<style scoped>
#app {
  height: 100vh;
}

.el-header {
  background-color: #409eff;
  color: white;
  line-height: 60px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h1 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 5px;
}

.el-main {
  padding: 20px;
  background-color: #f5f5f5;
}

.stat-card {
  text-align: center;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.stat-card .el-card__header {
  text-align: center;
  padding: 15px 20px;
}

.stat-card .el-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.stat-number {
  font-size: 2em;
  font-weight: bold;
  color: #409eff;
  margin: 0;
}

/* 统计卡片容器 - 实现等宽布局 */
.stats-container {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  align-items: stretch; /* 确保所有卡片拉伸到相同高度 */
}

.stats-container .el-card {
  flex: 1;
  min-width: 0; /* 防止卡片溢出 */
  display: flex;
  flex-direction: column;
}

.stat-number.online {
  color: #67c23a;
}

.stat-number.offline {
  color: #f56c6c;
}



.refresh-icon {
  cursor: pointer;
  color: #909399;
  transition: color 0.3s;
}

.refresh-icon:hover {
  color: #409eff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 统计卡片的标题居中 */
.stat-card .card-header {
  justify-content: center;
}

.el-table {
  margin-top: 10px;
}

.filter-form {
  width: 100%;
}

.filter-form .el-form-item {
  margin-bottom: 15px;
}

.filter-form .el-form-item__label {
  font-weight: 500;
  color: #606266;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .filter-form .el-col {
    margin-bottom: 10px;
  }
  
  .filter-form .el-form-item {
    margin-bottom: 10px;
  }
  
  .filter-form .el-button {
    width: 100%;
    margin-bottom: 5px;
  }
}

.device-data {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.device-data .data-content {
  font-family: 'Courier New', monospace;
  background-color: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  word-break: break-all;
}

.device-data .data-time {
  font-size: 11px;
  color: #909399;
}

.table-data {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.table-data .data-preview {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
}

.table-data .data-time {
  font-size: 10px;
  color: #909399;
}

.no-data {
  color: #c0c4cc;
  font-style: italic;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 10px 0;
}

.filter-section {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

/* 离线设备弹窗样式 */
.offline-alert-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 0;
}

.offline-icon {
  margin-bottom: 20px;
}

.offline-message h3 {
  color: #E6A23C;
  margin: 0 0 15px 0;
  font-size: 18px;
  font-weight: 600;
}

.offline-message p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.dialog-footer .el-checkbox {
  margin-right: 0;
}
</style>
