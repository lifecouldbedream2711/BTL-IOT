import { useState, useEffect } from 'react'
import { Thermometer, Droplets, Sun, Search } from 'lucide-react'
import PageHeader from '../components/PageHeader'
import Pagination from '../components/Pagination'
import { api } from '../services/api'

const PAGE_SIZES = [5, 10, 20, 50]

const SENSOR_TYPES = ['All', 'Temperature', 'Humidity', 'Light']

const typeStyle = {
  Temperature: { icon: Thermometer, unit: '°C', badge: 'bg-orange-50 text-orange-600 ring-orange-200', value: 'text-orange-500' },
  Humidity:    { icon: Droplets,    unit: '%',  badge: 'bg-blue-50 text-blue-600 ring-blue-200',       value: 'text-blue-600' },
  Light:       { icon: Sun,         unit: 'lux', badge: 'bg-yellow-50 text-yellow-600 ring-yellow-200', value: 'text-yellow-600' },
}

export default function DataSensor() {
  const [pageSize, setPageSize] = useState(10)
  const [sensorType, setSensorType] = useState('All')
  const [keyword, setKeyword] = useState('') // nội dung đang gõ
  const [query, setQuery] = useState('')     // nội dung đã bấm Search
  const [page, setPage] = useState(1)

  const [rows, setRows] = useState([])
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(false)

  // Gọi API mỗi khi filter, query, page, pageSize thay đổi
  useEffect(() => {
    let active = true

    const loadData = async () => {
      setLoading(true)
      try {
        const res = await api.getSensorData({
          sensorType,
          keyword: query,
          page,
          pageSize,
        })
        if (active && res) {
          setRows(res.items || [])
          setTotal(res.total || 0)
          setTotalPages(Math.max(1, res.totalPages || 1))
        }
      } catch (err) {
        console.error('Error fetching sensor data:', err)
      } finally {
        if (active) setLoading(false)
      }
    }

    loadData()
    return () => {
      active = false
    }
  }, [sensorType, query, page, pageSize])

  const handleSearch = () => {
    setQuery(keyword)
    setPage(1)
  }

  const startIdx = (page - 1) * pageSize

  return (
    <div className="flex h-full flex-col gap-4">
      <PageHeader title="Data Sensor" subtitle="Historical sensor readings" />

      {/* Bộ lọc: dropdown loại cảm biến + tìm theo thời gian / giá trị */}
      <div className="flex shrink-0 items-center gap-3 rounded-2xl bg-white p-3 shadow-sm ring-1 ring-slate-100">
        <select
          value={sensorType}
          onChange={(e) => {
            setSensorType(e.target.value)
            setPage(1)
          }}
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
            {total === 0
              ? 'No records'
              : `Showing ${startIdx + 1}-${startIdx + rows.length} of ${total} records`}
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
              {loading && rows.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-5 py-10 text-center text-slate-400">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : rows.map((r) => {
                const s = typeStyle[r.type] || typeStyle.Temperature
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
              {!loading && rows.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-5 py-10 text-center text-slate-400">
                    Không tìm thấy bản ghi nào. Thử đổi loại cảm biến hoặc từ khóa tìm kiếm.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang */}
        <Pagination
          page={page}
          totalPages={totalPages}
          onChange={setPage}
          pageSize={pageSize}
          pageSizes={PAGE_SIZES}
          onPageSizeChange={(n) => { setPageSize(n); setPage(1) }}
        />
      </div>
    </div>
  )
}
