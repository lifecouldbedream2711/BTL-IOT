import {
  ResponsiveContainer, ComposedChart, Area, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend,
} from 'recharts'

export default function SensorChart({ data }) {
  return (
    <div className="flex min-h-0 flex-1 flex-col rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-100">
      <div>
        <h2 className="text-base font-semibold text-slate-900">Sensor Trends</h2>
        <p className="text-xs text-slate-500">Temperature · Humidity · Light</p>
      </div>

      {/* flex-1 + min-h-0 để biểu đồ tự co giãn theo chiều cao còn lại */}
      <div className="mt-2 min-h-0 flex-1">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart data={data} margin={{ top: 8, right: 16, left: -16, bottom: 0 }}>
            
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
            <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#64748b' }} tickLine={false} axisLine={false} />
            <YAxis domain={[0, 600]} ticks={[0, 150, 300, 450, 600]}
              tick={{ fontSize: 11, fill: '#64748b' }} tickLine={false} axisLine={false} />
            <Tooltip />
            <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 12 }} />
            <Area type="monotone" dataKey="light" name="Light" stroke="#eab308"
              strokeWidth={2} fill="url(#lightFill)" dot={false} />
            <Line type="monotone" dataKey="humidity" name="Humidity" stroke="#2563eb"
              strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="temp" name="Temp" stroke="#f97316"
              strokeWidth={2} dot={false} />
          </ComposedChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
