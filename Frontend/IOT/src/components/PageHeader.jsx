export default function PageHeader({ title, subtitle }) {
  return (
    <div className="flex shrink-0 items-center justify-between">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
        <p className="text-sm text-slate-500">{subtitle}</p>
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
  )
}
