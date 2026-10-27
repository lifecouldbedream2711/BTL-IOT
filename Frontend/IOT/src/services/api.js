const BASE_URL = '' // Uses Vite proxy to http://localhost:8080

const TOKEN_KEY = 'iot_auth_token'
const USER_KEY = 'iot_auth_user'

function getAuthHeaders() {
  const token = localStorage.getItem(TOKEN_KEY)
  const headers = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

export const api = {
  // --- Auth APIs ---
  getToken() {
    return localStorage.getItem(TOKEN_KEY)
  },

  getCurrentUser() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
    } catch {
      return null
    }
  },

  isAuthenticated() {
    return Boolean(localStorage.getItem(TOKEN_KEY))
  },

  async login(username, password) {
    const res = await fetch(`${BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    const data = await res.json()
    if (data.code === 1000 && data.result?.token) {
      localStorage.setItem(TOKEN_KEY, data.result.token)
      localStorage.setItem(USER_KEY, JSON.stringify({
        username: data.result.username || username,
        userId: data.result.userId
      }))
      return data.result
    } else {
      throw new Error(data.message || 'Đăng nhập không thành công')
    }
  },

  async introspect(token) {
    try {
      const res = await fetch(`${BASE_URL}/api/auth/introspect`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: token || this.getToken() })
      })
      const data = await res.json()
      return data.result?.valid === true
    } catch {
      return false
    }
  },

  logout() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    window.location.href = '/login'
  },

  // 1. Dashboard APIs
  async getLatestSensors() {
    try {
      const res = await fetch(`${BASE_URL}/api/sensors/latest`, {
        headers: getAuthHeaders()
      })
      const data = await res.json()
      return data.result || { temp: 28.5, humidity: 65, light: 450 }
    } catch (e) {
      console.warn('Failed to fetch latest sensors:', e)
      return null
    }
  },

  async getSensorChart() {
    try {
      const res = await fetch(`${BASE_URL}/api/sensors/chart`, {
        headers: getAuthHeaders()
      })
      const data = await res.json()
      return data.result || []
    } catch (e) {
      console.warn('Failed to fetch sensor chart:', e)
      return []
    }
  },

  async getMqttStatus() {
    try {
      const res = await fetch(`${BASE_URL}/api/sensors/mqtt-status`, {
        headers: getAuthHeaders()
      })
      const data = await res.json()
      return data.result || { connected: false, status: 'MQTT Disconnected' }
    } catch (e) {
      return { connected: false, status: 'MQTT Disconnected' }
    }
  },

  async getDevices() {
    try {
      const res = await fetch(`${BASE_URL}/api/devices`, {
        headers: getAuthHeaders()
      })
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
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeaders()
        }
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

      const res = await fetch(`${BASE_URL}/api/sensor-data?${params.toString()}`, {
        headers: getAuthHeaders()
      })
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

      const res = await fetch(`${BASE_URL}/api/actions?${params.toString()}`, {
        headers: getAuthHeaders()
      })
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
      const res = await fetch(`${BASE_URL}/api/users/profile`, {
        headers: getAuthHeaders()
      })
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
export const login = (username, password) => api.login(username, password)
export const logout = () => api.logout()
export const getCurrentUser = () => api.getCurrentUser()
export const isAuthenticated = () => api.isAuthenticated()

export default api
