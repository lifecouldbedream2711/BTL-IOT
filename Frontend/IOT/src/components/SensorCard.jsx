const colorStyles = {
  red:    { icon: 'bg-red-50 text-red-500',       badge: 'bg-slate-100 text-slate-600' },
  blue:   { icon: 'bg-blue-50 text-blue-500',     badge: 'bg-slate-100 text-slate-600' },
  yellow: { icon: 'bg-yellow-50 text-yellow-500', badge: 'bg-slate-100 text-slate-600' },
}

export default function SensorCard({ icon: Icon, label, value, unit, color = 'blue' }) {
  const style = colorStyles[color]

  return (
    <div className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-100">
      <div className="flex items-start justify-between">
        <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${style.icon}`}>
          <Icon size={20} />
        </div>
        
      </div>

      <p className="mt-3 text-3xl font-bold leading-none text-slate-900">
        {value ?? '--'}
        <span className="ml-1 text-sm font-medium text-slate-500">{unit}</span>
      </p>
      <p className="mt-1 text-sm text-slate-500">{label}</p>
    </div>
  )
}
