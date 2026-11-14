import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts'
import { Thermometer, Droplets, Sun, Wind } from 'lucide-react'

function SingleChart({
  title,
  icon: Icon,
  dataKey,
  unit,
  color,
  gradientId,
  data,
  latestValue,
  domain,
}) {
  return (
    <div className="flex flex-col min-h-0 h-full rounded-xl bg-white p-2 md:p-2.5 shadow-sm ring-1 ring-slate-100">
      <div className="flex items-center justify-between shrink-0 mb-1">
        <div className="flex items-center gap-1.5">
          <div
            className="flex h-5 w-5 items-center justify-center rounded-md"
            style={{ backgroundColor: `${color}18`, color }}
          >
            <Icon size={12} />
          </div>
          <span className="text-xs font-semibold text-slate-800">{title}</span>
        </div>
        <span className="text-xs font-bold font-mono" style={{ color }}>
          {latestValue != null ? `${latestValue} ${unit}` : '--'}
        </span>
      </div>

      <div className="flex-1 min-h-0 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 2, right: 6, left: -22, bottom: 0 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={color} stopOpacity={0.28} />
                <stop offset="95%" stopColor={color} stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="2 2" stroke="#f1f5f9" vertical={false} />
            <XAxis
              dataKey="time"
              tick={{ fontSize: 9, fill: '#94a3b8' }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              domain={domain || ['auto', 'auto']}
              tick={{ fontSize: 9, fill: '#94a3b8' }}
              tickLine={false}
              axisLine={false}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#ffffff',
                borderRadius: '6px',
                padding: '3px 8px',
                boxShadow: '0 2px 6px rgba(0,0,0,0.08)',
                border: '1px solid #e2e8f0',
                fontSize: '11px',
              }}
              formatter={(val) => [`${val} ${unit}`, title]}
            />
            <Area
              type="monotone"
              dataKey={dataKey}
              name={title}
              stroke={color}
              strokeWidth={1.8}
              fill={`url(#${gradientId})`}
              dot={false}
              isAnimationActive={false}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

export default function SensorChart({ data, sensor = {} }) {
  return (
    <div className="grid grid-cols-2 grid-rows-2 gap-2 h-full min-h-0">
      <SingleChart
        title="Temperature"
        icon={Thermometer}
        dataKey="temp"
        unit="°C"
        color="#f97316"
        gradientId="tempGrad"
        data={data}
        latestValue={sensor.temp}
      />
      <SingleChart
        title="Humidity"
        icon={Droplets}
        dataKey="humidity"
        unit="%"
        color="#2563eb"
        gradientId="humGrad"
        data={data}
        latestValue={sensor.humidity}
      />
      <SingleChart
        title="Light Intensity"
        icon={Sun}
        dataKey="light"
        unit="lux"
        color="#eab308"
        gradientId="lightGrad"
        data={data}
        latestValue={sensor.light}
      />
      <SingleChart
        title="CO2 Concentration"
        icon={Wind}
        dataKey="co2"
        unit="ppm"
        color="#10b981"
        gradientId="co2Grad"
        data={data}
        latestValue={sensor.co2}
      />
    </div>
  )
}
