import { useState } from 'react'
import { Thermometer, Droplets, Sun, Fan, Lightbulb, AirVent } from 'lucide-react'
import SensorCard from '../components/SensorCard'
import SensorChart from '../components/SensorChart'
import DeviceCard from '../components/DeviceCard'

// Dữ liệu mẫu — sau này thay bằng dữ liệu thật từ MQTT / API
const chartData = [
  { time: '01:36:00 AM', light: 480, humidity: 66, temp: 29 },
  { time: '01:36:03 AM', light: 520, humidity: 65, temp: 29 },
  { time: '01:36:06 AM', light: 515, humidity: 64, temp: 29 },
  { time: '01:36:09 AM', light: 510, humidity: 62, temp: 28 },
  { time: '01:36:12 AM', light: 450, humidity: 58, temp: 28 },
  { time: '01:36:15 AM', light: 400, humidity: 56, temp: 28 },
  { time: '01:36:18 AM', light: 340, humidity: 55, temp: 28 },
  { time: '01:36:21 AM', light: 305, humidity: 56, temp: 29 },
  { time: '01:36:24 AM', light: 300, humidity: 58, temp: 29 },
  { time: '01:36:27 AM', light: 265, humidity: 60, temp: 29 },
  { time: '01:36:30 AM', light: 255, humidity: 64, temp: 29 },
  { time: '01:36:33 AM', light: 335, humidity: 66, temp: 29 },
]

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const initialDevices = [
  { id: 'fan', name: 'Fan', icon: Fan, on: true },
  { id: 'light', name: 'Light', icon: Lightbulb, on: false },
  { id: 'ac', name: 'Air Conditioner', icon: AirVent, on: true },
]

export default function Dashboard() {
  const [devices, setDevices] = useState(initialDevices)
  const [pending, setPending] = useState([]) // id các thiết bị đang chờ
  const sensor = { temp: 29.4, humidity: 66.6, light: 495 }

  const toggle = async (id) => {
    if (pending.includes(id)) return // đang chờ thì bỏ qua

    setPending((p) => [...p, id])
    await sleep(500) 
    setDevices((list) => list.map((d) => (d.id === id ? { ...d, on: !d.on } : d)))
    setPending((p) => p.filter((x) => x !== id))
  }

  return (
    <div className="flex h-full flex-col gap-4">
      {/* Header */}
      <div className="flex shrink-0 items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500">Real-time IoT monitoring</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-600 ring-1 ring-emerald-200">
            ● MQTT Connected
          </span>
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-sky-500 text-sm font-semibold text-white">
            TV
          </div>
        </div>
      </div>

      {/* 3 thẻ cảm biến */}
      <div className="grid shrink-0 grid-cols-3 gap-4">
        <SensorCard icon={Thermometer} label="Temperature" value={sensor.temp}
          unit="°C" status="Normal range" color="red" />
        <SensorCard icon={Droplets} label="Humidity" value={sensor.humidity}
          unit="%" status="Comfortable" color="blue" />
        <SensorCard icon={Sun} label="Light Intensity" value={sensor.light}
          unit="lux" status="Moderate" color="yellow" />
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