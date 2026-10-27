import { useState, useEffect } from 'react'
import { Thermometer, Droplets, Sun, Fan, Lightbulb, AirVent } from 'lucide-react'
import SensorCard from '../components/SensorCard'
import SensorChart from '../components/SensorChart'
import DeviceCard from '../components/DeviceCard'
import { api } from '../services/api'

const DEVICE_ICONS = {
  fan: Fan,
  light: Lightbulb,
  ac: AirVent,
}

const initialDevices = [
  { id: 'fan', name: 'Fan', icon: Fan, on: false },
  { id: 'light', name: 'Light', icon: Lightbulb, on: false },
  { id: 'ac', name: 'Air Conditioner', icon: AirVent, on: false },
]

export default function Dashboard() {
  const [devices, setDevices] = useState(initialDevices)
  const [pending, setPending] = useState([]) // id các thiết bị đang chờ
  const [sensor, setSensor] = useState({ temp: 28.5, humidity: 65, light: 450 })
  const [chartData, setChartData] = useState([])
  const [mqttStatus, setMqttStatus] = useState({ connected: true, status: 'MQTT Connected' })

  // Lấy dữ liệu định kỳ mỗi 2.5 giây
  useEffect(() => {
    let isMounted = true

    const fetchData = async () => {
      try {
        const [sensorRes, chartRes, devicesRes, statusRes] = await Promise.all([
          api.getLatestSensors(),
          api.getSensorChart(),
          api.getDevices(),
          api.getMqttStatus()
        ])

        if (!isMounted) return

        if (sensorRes) {
          setSensor(sensorRes)
        }

        if (chartRes && chartRes.length > 0) {
          setChartData(chartRes)
        }

        if (devicesRes && devicesRes.length > 0) {
          setDevices(devicesRes.map((d) => ({
            ...d,
            icon: DEVICE_ICONS[d.id] || Lightbulb,
          })))
        }

        if (statusRes) {
          setMqttStatus(statusRes)
        }
      } catch (e) {
        console.error('Error polling dashboard data:', e)
      }
    }

    fetchData()
    const interval = setInterval(fetchData, 2500)

    return () => {
      isMounted = false
      clearInterval(interval)
    }
  }, [])

  const toggle = async (id) => {
    if (pending.includes(id)) return // đang chờ thì bỏ qua

    setPending((p) => [...p, id])
    try {
      const updated = await api.toggleDevice(id)
      if (updated) {
        setDevices((list) =>
          list.map((d) => (d.id === id ? { ...d, on: updated.on, status: updated.status } : d))
        )
      }
    } catch (e) {
      console.error('Failed to toggle device:', e)
    } finally {
      setPending((p) => p.filter((x) => x !== id))
    }
  }

  // Đánh giá nhãn trạng thái cảm biến
  const tempStatus = sensor.temp > 33 ? 'High' : sensor.temp < 20 ? 'Low' : 'Normal range'
  const humStatus = sensor.humidity > 75 ? 'Humid' : sensor.humidity < 40 ? 'Dry' : 'Comfortable'
  const lightStatus = sensor.light > 500 ? 'Bright' : sensor.light < 150 ? 'Dim' : 'Moderate'

  return (
    <div className="flex h-full flex-col gap-4">
      {/* Header */}
      <div className="flex shrink-0 items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500">Real-time IoT monitoring</p>
        </div>
        <div className="flex items-center gap-3">
          <span
            className={`rounded-full px-3 py-1 text-xs font-medium ring-1 ${
              mqttStatus.connected
                ? 'bg-emerald-50 text-emerald-600 ring-emerald-200'
                : 'bg-rose-50 text-rose-600 ring-rose-200'
            }`}
          >
            ● {mqttStatus.status}
          </span>
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-sky-500 text-sm font-semibold text-white">
            PQ
          </div>
        </div>
      </div>

      {/* 3 thẻ cảm biến */}
      <div className="grid shrink-0 grid-cols-3 gap-4">
        <SensorCard
          icon={Thermometer}
          label="Temperature"
          value={sensor.temp}
          unit="°C"
          status={tempStatus}
          color="red"
        />
        <SensorCard
          icon={Droplets}
          label="Humidity"
          value={sensor.humidity}
          unit="%"
          status={humStatus}
          color="blue"
        />
        <SensorCard
          icon={Sun}
          label="Light Intensity"
          value={sensor.light}
          unit="lux"
          status={lightStatus}
          color="yellow"
        />
      </div>

      {/* Biểu đồ */}
      <SensorChart data={chartData} />

      {/* Điều khiển thiết bị */}
      <div className="shrink-0 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-100">
        <h2 className="text-base font-semibold text-slate-900">Device Control</h2>
        <p className="mb-3 text-xs text-slate-500">Manage connected devices</p>
        <div className="grid grid-cols-3 gap-4">
          {devices.map((d) => (
            <DeviceCard
              key={d.id}
              icon={d.icon}
              name={d.name}
              on={d.on}
              loading={pending.includes(d.id)}
              onToggle={() => toggle(d.id)}
            />
          ))}
        </div>
      </div>
    </div>
  )
}