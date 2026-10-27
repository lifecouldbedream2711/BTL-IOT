const BASE_URL = '' // Uses Vite proxy to http://localhost:8080

export const api = {
  // 1. Dashboard APIs
  async getLatestSensors() {
    try {
      const res = await fetch(`${BASE_URL}/api/sensors/latest`)
      const data = await res.json()
      return data.result || { temp: 28.5, humidity: 65, light: 450 }
    } catch (e) {
      console.warn('Failed to fetch latest sensors:', e)
      return null
    }
  },

  async getSensorChart() {
    try {
      const res = await fetch(`${BASE_URL}/api/sensors/chart`)
      const data = await res.json()
      return data.result || []
    } catch (e) {
      console.warn('Failed to fetch sensor chart:', e)
      return []
    }
  },

  async getMqttStatus() {
    try {
      const res = await fetch(`${BASE_URL}/api/sensors/mqtt-status`)
      const data = await res.json()
      return data.result || { connected: false, status: 'MQTT Disconnected' }
    } catch (e) {
      return { connected: false, status: 'MQTT Disconnected' }
    }
  },

  async getDevices() {
    try {
      const res = await fetch(`${BASE_URL}/api/devices`)
      const data = await res.json()
      return data.result || []
    } catch (e) {
      console.warn('Failed to fetch devices:', e)
      return []
    }
  },

  async toggleDevice(id) {
    try {
      const res = await fetch(`${BASE_URL}/api/devices/${id}/toggle`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })
      const data = await res.json()
      return data.result
    } catch (e) {
      console.error('Failed to toggle device:', e)
      throw e
    }
  },

  // 2. Data Sensor API
  async getSensorData({ sensorType = 'All', keyword = '', page = 1, pageSize = 10 } = {}) {
    try {
      const params = new URLSearchParams()
      if (sensorType && sensorType !== 'All') params.append('sensorType', sensorType)
      if (keyword && keyword.trim()) params.append('keyword', keyword.trim())
      params.append('page', String(page))
      params.append('pageSize', String(pageSize))

      const res = await fetch(`${BASE_URL}/api/sensor-data?${params.toString()}`)
      const data = await res.json()
      return data.result || { items: [], total: 0, page: 1, pageSize, totalPages: 1 }
    } catch (e) {
      console.error('Failed to fetch sensor data:', e)
      return { items: [], total: 0, page: 1, pageSize, totalPages: 1 }
    }
  },

  // 3. Active History API
  async getActions({ device = 'All Devices', status = 'All Status', actionStatus = 'All Action Status', time = '', page = 1, pageSize = 10 } = {}) {
    try {
      const params = new URLSearchParams()
      if (device && device !== 'All Devices') params.append('device', device)
      if (status && status !== 'All Status') params.append('status', status)
      if (actionStatus && actionStatus !== 'All Action Status') params.append('actionStatus', actionStatus)
      if (time && time.trim()) params.append('time', time.trim())
      params.append('page', String(page))
      params.append('pageSize', String(pageSize))

      const res = await fetch(`${BASE_URL}/api/actions?${params.toString()}`)
      const data = await res.json()
      return data.result || { items: [], total: 0, page: 1, pageSize, totalPages: 1 }
    } catch (e) {
      console.error('Failed to fetch actions:', e)
      return { items: [], total: 0, page: 1, pageSize, totalPages: 1 }
    }
  },

  // 4. User Profile API
  async getUserProfile() {
    try {
      const res = await fetch(`${BASE_URL}/api/users/profile`)
      const data = await res.json()
      return data.result
    } catch (e) {
      console.warn('Failed to fetch user profile:', e)
      return null
    }
  }
}

export const fetchActions = (params) => api.getActions(params)
export const fetchSensorData = (params) => api.getSensorData(params)
export const fetchSensorsLatest = () => api.getLatestSensors()
export const fetchSensorChart = () => api.getSensorChart()
export const fetchMqttStatus = () => api.getMqttStatus()
export const fetchDevices = () => api.getDevices()
export const toggleDevice = (id) => api.toggleDevice(id)
export const fetchUserProfile = () => api.getUserProfile()

export default api
