import { useMemo, useState } from 'react'
import { Thermometer, Droplets, Sun, Search, ChevronLeft, ChevronRight } from 'lucide-react'
import PageHeader from '../components/PageHeader'

const PAGE_SIZE = 10

const SENSOR_TYPES = ['All', 'Temperature', 'Humidity', 'Light','Time']

const typeStyle = {
  Temperature: { icon: Thermometer, unit: '°C', badge: 'bg-orange-50 text-orange-600 ring-orange-200', value: 'text-orange-500' },
  Humidity:    { icon: Droplets,    unit: '%',  badge: 'bg-blue-50 text-blue-600 ring-blue-200',       value: 'text-blue-600' },
  Light:       { icon: Sun,         unit: 'lux', badge: 'bg-yellow-50 text-yellow-600 ring-yellow-200', value: 'text-yellow-600' },
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec']
const pad = (n) => String(n).padStart(2, '0')
const formatTime = (d) =>
  `${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}, ${pad(d.getHours())}:${pad(d.getMinutes())}`

// Dữ liệu mẫu (250 bản ghi) — sau này thay bằng dữ liệu từ API
function buildMockData() {
  const rows = []
  const start = new Date(2025, 8, 16, 8, 0) // 16/09/2025 08:00
  let id = 1
  for (let step = 0; rows.length < 250; step++) {
    const time = formatTime(new Date(start.getTime() + step * 5 * 60000))
    rows.push({ id: id++, type: 'Temperature', value: (26 + 5 * Math.abs(Math.sin(step / 5))).toFixed(1), time })
    rows.push({ id: id++, type: 'Humidity',    value: (58 + 14 * Math.abs(Math.cos(step / 7))).toFixed(1), time })
    rows.push({ id: id++, type: 'Light',       value: Math.round(380 + 160 * Math.abs(Math.sin(step / 9))), time })
  }
  return rows.slice(0, 250)
}
const ALL_RECORDS = buildMockData()

// Tạo dãy số trang kèm dấu "…" khi có nhiều trang
function getPages(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = [1]
  if (current > 3) pages.push('…')
  for (let p = Math.max(2, current - 1); p <= Math.min(total - 1, current + 1); p++) pages.push(p)
  if (current < total - 2) pages.push('…')
  pages.push(total)
  return pages
}

export default function DataSensor() {
  const [sensorType, setSensorType] = useState('All')
  const [keyword, setKeyword] = useState('') // nội dung đang gõ
  const [query, setQuery] = useState('')     // nội dung đã bấm Search
  const [page, setPage] = useState(1)

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return ALL_RECORDS.filter(
      (r) =>r
    )
  }, [sensorType, query])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const startIdx = (currentPage - 1) * PAGE_SIZE
  const rows = filtered.slice(startIdx, startIdx + PAGE_SIZE)

  const handleSearch = () => {
  
    setQuery(keyword)
    setPage(1)
  }

  return (
    <div className="flex h-full flex-col gap-4">
      <PageHeader title="Data Sensor" subtitle="Historical sensor readings" />

      {/* Bộ lọc: dropdown loại cảm biến + tìm theo thời gian */}
      <div className="flex shrink-0 items-center gap-3 rounded-2xl bg-white p-3 shadow-sm ring-1 ring-slate-100">
        <select
          
          onChange={(e) => { setSensorType(e.target.value) }}
          aria-label="Lọc theo loại cảm biến"
          className="h-10 w-44 rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {SENSOR_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>

        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            placeholder="Nhập giá trị cần tìm"
            className="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <button
          type="button"
          onClick={handleSearch}
          className="flex h-10 items-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
        >
          <Search size={16} />
          Search
        </button>
      </div>

      {/* Bảng dữ liệu */}
      <div className="flex min-h-0 flex-1 flex-col rounded-2xl bg-white shadow-sm ring-1 ring-slate-100">
        <div className="flex shrink-0 items-center justify-between px-5 py-3">
          <h2 className="text-base font-semibold text-slate-900">Sensor Records</h2>
          <p className="text-xs text-slate-500">
            {filtered.length === 0
              ? 'No records'
              : `Showing ${startIdx + 1}-${startIdx + rows.length} of ${filtered.length} records`}
          </p>
        </div>

        <div className="min-h-0 flex-1 overflow-auto">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-50 text-xs font-medium text-slate-500">
              <tr>
                <th className="px-5 py-2.5">ID</th>
                <th className="px-5 py-2.5">Sensor Type</th>
                <th className="px-5 py-2.5">Value</th>
                <th className="px-5 py-2.5">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((r) => {
                const s = typeStyle[r.type]
                const Icon = s.icon
                return (
                  <tr key={r.id} className="hover:bg-slate-50/70">
                    <td className="px-5 py-2.5 text-slate-500">#{r.id}</td>
                    <td className="px-5 py-2.5">
                      <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ${s.badge}`}>
                        <Icon size={12} />
                        {r.type}
                      </span>
                    </td>
                    <td className="px-5 py-2.5">
                      <span className={`font-bold ${s.value}`}>{r.value}</span>
                      <span className="ml-1 text-xs text-slate-400">{s.unit}</span>
                    </td>
                    <td className="px-5 py-2.5 text-slate-500">{r.time}</td>
                  </tr>
                )
              })}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-5 py-10 text-center text-slate-400">
                    Không tìm thấy bản ghi nào. Thử đổi loại cảm biến hoặc từ khóa thời gian.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang */}
        <div className="flex shrink-0 items-center justify-between border-t border-slate-100 px-5 py-3">
          <p className="text-xs text-slate-500">Page {currentPage} / {totalPages}</p>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => setPage(currentPage - 1)}
              disabled={currentPage === 1}
              aria-label="Trang trước"
              className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft size={16} />
            </button>

            {getPages(currentPage, totalPages).map((p, i) =>
              p === '…' ? (
                <span key={`dots-${i}`} className="px-1 text-slate-400">…</span>
              ) : (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPage(p)}
                  aria-current={p === currentPage ? 'page' : undefined}
                  className={`h-8 min-w-8 rounded-lg px-2 text-sm ${
                    p === currentPage
                      ? 'bg-blue-600 font-medium text-white'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  {p}
                </button>
              )
            )}

            <button
              type="button"
              onClick={() => setPage(currentPage + 1)}
              disabled={currentPage === totalPages}
              aria-label="Trang sau"
              className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
