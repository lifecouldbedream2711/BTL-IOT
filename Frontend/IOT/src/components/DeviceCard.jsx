export default function DeviceCard({ icon: Icon, name, on, loading = false, onToggle }) {
  return (
    <div
      className={`rounded-xl p-3 ring-1 transition-colors ${
        on ? 'bg-blue-50/60 ring-blue-200' : 'bg-slate-50 ring-slate-200'
      }`}
    >
      <div className="flex items-center justify-between">
        <div
          className={`flex h-10 w-10 items-center justify-center rounded-xl ${
            on ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-400'
          }`}
        >
          <Icon size={20} />
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
          className={`relative h-6 w-11 rounded-full transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
            on ? 'bg-blue-600' : 'bg-slate-300'
          } ${loading ? 'cursor-wait opacity-60' : ''}`}
        >
          <span
            className={`absolute top-0.5 left-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-white shadow transition-transform ${
              on ? 'translate-x-5' : 'translate-x-0'
            }`}
          >
            {loading && (
              <span className="h-3 w-3 animate-spin rounded-full border-2 border-slate-300 border-t-blue-600" />
            )}
          </span>
        </button>
      </div>

      <p className="mt-2 text-sm font-semibold text-slate-900">{name}</p>
      <div className="flex items-center justify-between text-xs">
        <span className={on ? 'font-medium text-blue-600' : 'font-medium text-slate-400'}>
          {on ? 'ON' : 'OFF'}
        </span>
      
      </div>
    </div>
  )
}