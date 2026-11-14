const colorStyles = {
  red: { icon: 'bg-red-50 text-red-500 ring-red-100', value: 'text-red-600' },
  blue: { icon: 'bg-blue-50 text-blue-500 ring-blue-100', value: 'text-blue-600' },
  yellow: { icon: 'bg-amber-50 text-amber-500 ring-amber-100', value: 'text-amber-600' },
  emerald: { icon: 'bg-emerald-50 text-emerald-500 ring-emerald-100', value: 'text-emerald-600' },
  green: { icon: 'bg-emerald-50 text-emerald-500 ring-emerald-100', value: 'text-emerald-600' },
}

export default function SensorCard({ icon: Icon, label, value, unit, color = 'blue', status }) {
  const style = colorStyles[color] || colorStyles.blue

  return (
    <div className="rounded-xl bg-slate-50/70 p-2 md:p-2.5 ring-1 ring-slate-200/70 flex flex-col justify-between min-h-0">
      <div className="flex items-center justify-between">
        <div className={`flex h-7 w-7 md:h-8 md:w-8 items-center justify-center rounded-lg ring-1 ${style.icon}`}>
          <Icon size={16} />
        </div>

      </div>

      <div className="mt-1">
        <p className="text-lg md:text-xl font-bold leading-tight text-slate-900">
          {value ?? '--'}
          <span className="ml-1 text-[11px] font-medium text-slate-500">{unit}</span>
        </p>
        <p className="text-[11px] font-medium text-slate-500 truncate">{label}</p>
      </div>
    </div>
  )
}
