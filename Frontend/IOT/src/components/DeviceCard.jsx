export default function DeviceCard({ icon: Icon, name, on, loading = false, onToggle }) {
  return (
    <div
      className={`rounded-xl p-2 md:p-2.5 ring-1 transition-colors flex flex-col justify-between min-h-0 ${
        on ? 'bg-blue-50/60 ring-blue-200' : 'bg-slate-50 ring-slate-200/80'
      }`}
    >
      <div className="flex items-center justify-between">
        <div
          className={`flex h-7 w-7 md:h-8 md:w-8 items-center justify-center rounded-lg ${
            on ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-400'
          }`}
        >
          <Icon size={16} />
        </div>

        {/* Công tắc bật/tắt */}
        <button
          type="button"
          role="switch"
          aria-checked={on}
          aria-busy={loading}
          aria-label={`Bật/tắt ${name}`}
          onClick={onToggle}
          disabled={loading}
          className={`relative h-5 w-9 md:h-6 md:w-11 rounded-full transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
            on ? 'bg-blue-600' : 'bg-slate-300'
          } ${loading ? 'cursor-wait opacity-60' : ''}`}
        >
          <span
            className={`absolute top-0.5 left-0.5 flex h-4 w-4 md:h-5 md:w-5 items-center justify-center rounded-full bg-white shadow transition-transform ${
              on ? 'translate-x-4 md:translate-x-5' : 'translate-x-0'
            }`}
          >
            {loading && (
              <span className="h-2.5 w-2.5 md:h-3 md:w-3 animate-spin rounded-full border-2 border-slate-300 border-t-blue-600" />
            )}
          </span>
        </button>
      </div>

      <div className="mt-1 flex items-center justify-between">
        <p className="text-xs md:text-sm font-semibold text-slate-900 truncate">{name}</p>
        <span className={`text-[11px] font-semibold ${on ? 'text-blue-600' : 'text-slate-400'}`}>
          {on ? 'ON' : 'OFF'}
        </span>
      </div>
    </div>
  )
}
