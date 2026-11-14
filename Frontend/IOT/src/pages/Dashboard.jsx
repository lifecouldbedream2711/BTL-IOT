import { useState, useEffect, useRef } from 'react'
import { Thermometer, Droplets, Sun, Fan, Lightbulb, AirVent, Wind } from 'lucide-react'
import SensorCard from '../components/SensorCard'
import SensorChart from '../components/SensorChart'
import DeviceCard from '../components/DeviceCard'
import { api } from '../services/api'

const DEVICE_ICONS = {
  fan: Fan,
  light: Lightbulb,
  ac: AirVent,
  led4: Lightbulb,
  4: Lightbulb,
}

const initialDevices = [
  { id: 'fan', name: 'Fan', icon: Fan, on: false },
  { id: 'light', name: 'Light', icon: Lightbulb, on: false },
  { id: 'ac', name: 'Air Conditioner', icon: AirVent, on: false },
  { id: 'led4', name: 'LED 4', icon: Lightbulb, on: false },
]

export default function Dashboard() {
  const [devices, setDevices] = useState(initialDevices)
  const [pending, setPending] = useState([]) // id các thiết bị đang chờ
  const pendingTogglesRef = useRef({}) // { [id]: { targetOn: boolean, timestamp: number } }
  const [sensor, setSensor] = useState({ temp: 28.5, humidity: 65, light: 450, co2: 600 })
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
          const now = Date.now()
          setDevices((prevList) => {
            return prevList.map((cur) => {
              const incoming = devicesRes.find((d) => d.id === cur.id || String(d.deviceId) === String(cur.id))
              if (!incoming) return cur

              const pendingToggle = pendingTogglesRef.current[cur.id]
              const isRecent = pendingToggle && (now - pendingToggle.timestamp < 10000)

              // Nếu vừa bấm click (<10s) hoặc đang có request pending:
              if (pendingToggle && isRecent) {
                const serverIsOn = incoming.status === 'ON' || incoming.on === true
                if ((pendingToggle.targetOn && serverIsOn) || (!pendingToggle.targetOn && !serverIsOn && incoming.status === 'OFF')) {
                  delete pendingTogglesRef.current[cur.id]
                  return {
                    ...incoming,
                    on: pendingToggle.targetOn,
                    icon: DEVICE_ICONS[cur.id] || Lightbulb,
                  }
                }

                return {
                  ...incoming,
                  on: pendingToggle.targetOn,
                  icon: DEVICE_ICONS[cur.id] || Lightbulb,
                }
              }

              return {
                ...incoming,
                on: incoming.status === 'ON' || incoming.on === true,
                icon: DEVICE_ICONS[cur.id] || Lightbulb,
              }
            })
          })
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

    const currentDev = devices.find((d) => d.id === id)
    const targetOn = currentDev ? !currentDev.on : true

    // 1. Cập nhật lạc quan (Optimistic Update) ngay lập tức trên UI
    setDevices((list) =>
      list.map((d) => (d.id === id ? { ...d, on: targetOn, status: targetOn ? 'ON' : 'OFF' } : d))
    )
    setPending((p) => [...p, id])
    pendingTogglesRef.current[id] = { targetOn, timestamp: Date.now() }

    try {
      const updated = await api.toggleDevice(id)
      if (updated) {
        setDevices((list) =>
          list.map((d) => {
            if (d.id !== id) return d
            const serverIsOn = updated.status === 'ON' ? true : updated.status === 'OFF' ? false : targetOn
            return {
              ...d,
              ...updated,
              on: serverIsOn,
              icon: DEVICE_ICONS[d.id] || Lightbulb,
            }
          })
        )
      }
    } catch (e) {
      console.error('Failed to toggle device:', e)
      // Rollback lại trạng thái nếu thực sự gặp lỗi mạng
      setDevices((list) =>
        list.map((d) => (d.id === id ? { ...d, on: !targetOn } : d))
      )
      delete pendingTogglesRef.current[id]
    } finally {
      setPending((p) => p.filter((x) => x !== id))
    }
  }

  // Đánh giá nhãn trạng thái cảm biến
  const tempStatus = sensor.temp > 33 ? 'High' : sensor.temp < 20 ? 'Low' : 'Normal range'
  const humStatus = sensor.humidity > 75 ? 'Humid' : sensor.humidity < 40 ? 'Dry' : 'Comfortable'
  const lightStatus = sensor.light > 500 ? 'Bright' : sensor.light < 150 ? 'Dim' : 'Moderate'
  const co2Status = (sensor.co2 || 600) > 1000 ? 'High' : (sensor.co2 || 600) < 500 ? 'Low' : 'Normal range'

  return (
    <div className="h-full flex flex-col justify-between overflow-hidden gap-2 md:gap-2.5 p-0.5">
      {/* 1. Header (shrink-0) */}
      <div className="flex shrink-0 items-center justify-between">
        <div>
          <h1 className="text-base md:text-lg font-bold text-slate-900 leading-tight">Dashboard</h1>
          <p className="text-[11px] text-slate-500">Real-time IoT Monitoring & Actuator Control</p>
        </div>
        <div className="flex items-center gap-2.5">
          <span
            className={`rounded-full px-2.5 py-0.5 text-[11px] font-medium ring-1 ${
              mqttStatus.connected
                ? 'bg-emerald-50 text-emerald-600 ring-emerald-200'
                : 'bg-rose-50 text-rose-600 ring-rose-200'
            }`}
          >
            ● {mqttStatus.status}
          </span>
          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-sky-500 text-[11px] font-semibold text-white">
            PQ
          </div>
        </div>
      </div>

      {/* 2. NỬA TRÊN: "4 đồ thị" (4 charts độc lập) xếp lưới 2 hàng x 2 cột (2x2 grid) */}
      <div className="flex-[1.15] min-h-0 w-full overflow-hidden">
        <SensorChart data={chartData} sensor={sensor} />
      </div>

      {/* 3. NỬA DƯỚI: Chia 2 cột (Trái: 4 box latest 2x2, Phải: 4 đèn 2x2) */}
      <div className="flex-1 min-h-0 w-full grid grid-cols-1 md:grid-cols-2 gap-2 md:gap-2.5 overflow-hidden">
        {/* Nửa dưới BÊN TRÁI: "4 box latest" 2x2 */}
        <div className="rounded-2xl bg-white p-2.5 md:p-3 shadow-sm ring-1 ring-slate-100 flex flex-col min-h-0 overflow-hidden">
          <div className="flex items-center justify-between mb-1.5 shrink-0">
            <h2 className="text-xs md:text-sm font-semibold text-slate-900">Latest Sensors</h2>
            <span className="text-[10px] text-slate-400 font-medium">4 Channels</span>
          </div>
          <div className="grid grid-cols-2 grid-rows-2 gap-1.5 md:gap-2 flex-1 min-h-0">
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
            <SensorCard
              icon={Wind}
              label="CO2 Concentration"
              value={sensor.co2}
              unit="ppm"
              status={co2Status}
              color="emerald"
            />
          </div>
        </div>

        {/* Nửa dưới BÊN PHẢI: "4 đèn" 2x2 */}
        <div className="rounded-2xl bg-white p-2.5 md:p-3 shadow-sm ring-1 ring-slate-100 flex flex-col min-h-0 overflow-hidden">
          <div className="flex items-center justify-between mb-1.5 shrink-0">
            <h2 className="text-xs md:text-sm font-semibold text-slate-900">Device Control</h2>
            <span className="text-[10px] text-slate-400 font-medium">4 Relays</span>
          </div>
          <div className="grid grid-cols-2 grid-rows-2 gap-1.5 md:gap-2 flex-1 min-h-0">
            {devices.map((d) => (
              <DeviceCard
                key={d.id}
                icon={d.icon}
                name={d.name}
                on={d.on}
                status={d.status}
                loading={pending.includes(d.id)}
                onToggle={() => toggle(d.id)}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
