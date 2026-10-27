import { useEffect, useState } from 'react'
import { Clock, Snowflake, Fan, Lightbulb, X, Search, Activity } from 'lucide-react'
import PageHeader from '../components/PageHeader'
import Pagination from '../components/Pagination'
import { fetchActions } from '../services/api'

const PAGE_SIZES = [5, 10, 20, 50]

const DEVICES = ['Air Conditioner', 'Fan', 'Light']
const STATUSES = ['ON', 'OFF', 'PENDING']
const ACTION_STATUSES = ['Success', 'Pending', 'Failed']

const deviceIcon = { 'Air Conditioner': Snowflake, Fan, Light: Lightbulb }
const statusStyle = {
  ON: 'bg-emerald-50 text-emerald-600 ring-emerald-200',
  OFF: 'bg-red-50 text-red-600 ring-red-200',
  PENDING: 'bg-amber-50 text-amber-600 ring-amber-200',
}
const actionStyle = {
  Success: 'bg-emerald-50 text-emerald-600 ring-emerald-200',
  Pending: 'bg-amber-50 text-amber-600 ring-amber-200',
  Failed: 'bg-red-50 text-red-600 ring-red-200',
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec']
const pad = (n) => String(n).padStart(2, '0')
const formatTime = (d) =>
  `${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}, ` +
  `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`

// Dữ liệu mẫu dự phòng khi API chưa sẵn sàng
function buildMockData() {
  const base = new Date(2025, 8, 16, 23, 22, 0).getTime()
  const rows = []
  for (let id = 120; id >= 1; id--) {
    const d = new Date(base - (120 - id) * 8 * 60000)
    d.setSeconds((id * 37) % 60)
    rows.push({
      id,
      device: ['Air Conditioner', 'Fan', 'Light', 'Light'][id % 4],
      status: (id * 7) % 5 < 3 ? 'ON' : 'OFF',
      action: id % 23 === 0 ? 'Failed' : id % 9 === 0 ? 'Pending' : 'Success',
      ts: d.getTime(),
      time: formatTime(d),
    })
  }
  return rows
}
const FALLBACK_RECORDS = buildMockData()

const selectClass =
  'h-10 shrink-0 rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500'

export default function ActivateHistory() {
  const [pageSize, setPageSize] = useState(10)
  const [device, setDevice] = useState('All Devices')
  const [status, setStatus] = useState('All Status')
  const [action, setAction] = useState('All Action Status')
  const [time, setTime] = useState('')
  const [page, setPage] = useState(1)

  const [rows, setRows] = useState([])
  const [totalRecords, setTotalRecords] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  useEffect(() => {
    let isMounted = true

    const loadData = async () => {
      const result = await fetchActions({
        device,
        status,
        actionStatus: action,
        time,
        page,
        pageSize,
      })

      if (!isMounted) return

      if (result && result.items) {
        setRows(result.items)
        setTotalRecords(result.total)
        setTotalPages(Math.max(1, result.totalPages))
      } else {
        // Fallback local filtering
        const t = time ? new Date(time).getTime() : null
        const filtered = FALLBACK_RECORDS.filter(
          (r) =>
            (device === 'All Devices' || r.device === device) &&
            (status === 'All Status' || r.status === status) &&
            (action === 'All Action Status' || r.action === action) &&
            (t === null || (r.ts >= t && r.ts <= t + 999))
        )
        const total = filtered.length
        const pages = Math.max(1, Math.ceil(total / pageSize))
        const curPage = Math.min(page, pages)
        const start = (curPage - 1) * pageSize
        setRows(filtered.slice(start, start + pageSize))
        setTotalRecords(total)
        setTotalPages(pages)
      }
    }

    loadData()
    const timer = setInterval(loadData, 3000)
    return () => {
      isMounted = false
      clearInterval(timer)
    }
  }, [device, status, action, time, page, pageSize])

  const currentPage = Math.min(page, totalPages)
  const startIdx = (currentPage - 1) * pageSize

  const withReset = (setter) => (e) => {
    setter(e.target.value)
    setPage(1)
  }

  const hasFilter =
    time || device !== 'All Devices' || status !== 'All Status' || action !== 'All Action Status'

  const clearAll = () => {
    setTime('')
    setDevice('All Devices')
    setStatus('All Status')
    setAction('All Action Status')
    setPage(1)
  }

  return (
    <div className="flex h-full flex-col gap-4">
      <PageHeader title="Active History" subtitle="Device activity log" />

      {/* Một hàng ngang: bộ lọc bên trái, ô tìm kiếm thời gian chính bên phải */}
      <div className="shrink-0 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-100 ml-auto">
        <div className="flex flex-wrap items-center gap-2">
          <select value={device} onChange={withReset(setDevice)} aria-label="Lọc theo thiết bị" className={selectClass}>
            <option>All Devices</option>
            {DEVICES.map((d) => (
              <option key={d}>{d}</option>
            ))}
          </select>

          <select value={status} onChange={withReset(setStatus)} aria-label="Lọc theo trạng thái" className={selectClass}>
            <option>All Status</option>
            {STATUSES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>

          <select value={action} onChange={withReset(setAction)} aria-label="Lọc theo trạng thái hành động" className={selectClass}>
            <option>All Action Status</option>
            {ACTION_STATUSES.map((a) => (
              <option key={a}>{a}</option>
            ))}
          </select>

          {/* Ô thời gian */}
          <div className="flex shrink-0 gap-2">
            <div className="flex w-[250px] items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 focus-within:ring-2 focus-within:ring-blue-500">
              <Clock size={16} className="shrink-0 text-slate-400" />
              <input
                type="input"
                placeholder="Example: 2025-09-16T23:22:15"
                value={time}
                onChange={withReset(setTime)}
                aria-label="Lọc theo thời gian"
                className="h-9 w-full min-w-0 bg-transparent text-sm text-slate-800 focus:outline-none"
              />
            </div>

            <button
              type="button"
              className="flex h-10 shrink-0 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              <Search size={15} /> Search
            </button>
          </div>

          {hasFilter && (
            <button
              type="button"
              onClick={clearAll}
              className="flex h-10 shrink-0 items-center gap-1 rounded-lg px-2 text-sm text-slate-500 hover:bg-slate-100"
            >
              <X size={14} /> Clear
            </button>
          )}
        </div>
      </div>

      {/* Bảng nhật ký */}
      <div className="flex min-h-0 flex-1 flex-col rounded-2xl bg-white shadow-sm ring-1 ring-slate-100">
        <div className="flex shrink-0 items-center justify-between px-5 py-3">
          <h2 className="text-base font-semibold text-slate-900">Activity Log</h2>
          <p className="text-xs text-slate-500">
            {totalRecords === 0
              ? 'No records'
              : `Showing ${startIdx + 1}-${startIdx + rows.length} of ${totalRecords} records`}
          </p>
        </div>

        <div className="min-h-0 flex-1 overflow-auto">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-slate-50 text-xs font-medium text-slate-500">
              <tr>
                <th className="px-5 py-2.5">ID</th>
                <th className="px-5 py-2.5">Device</th>
                <th className="px-5 py-2.5">Status</th>
                <th className="px-5 py-2.5">Action Status</th>
                <th className="px-5 py-2.5">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((r) => {
                const Icon = deviceIcon[r.device] || Activity
                return (
                  <tr key={r.id} className="hover:bg-slate-50/70">
                    <td className="px-5 py-2.5 text-slate-500">#{r.id}</td>
                    <td className="px-5 py-2.5">
                      <span className="inline-flex items-center gap-2 font-medium text-slate-800">
                        <span className="flex h-6 w-6 items-center justify-center rounded-md bg-blue-50 text-blue-600">
                          <Icon size={14} />
                        </span>
                        {r.device}
                      </span>
                    </td>
                    <td className="px-5 py-2.5">
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${
                          statusStyle[r.status] || 'bg-slate-50 text-slate-600 ring-slate-200'
                        }`}
                      >
                        ● {r.status}
                      </span>
                    </td>
                    <td className="px-5 py-2.5">
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${
                          actionStyle[r.action] || 'bg-slate-50 text-slate-600 ring-slate-200'
                        }`}
                      >
                        ● {r.action}
                      </span>
                    </td>
                    <td className="px-5 py-2.5 tabular-nums text-slate-500">{r.time}</td>
                  </tr>
                )
              })}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-5 py-10 text-center text-slate-400">
                    Không tìm thấy bản ghi nào. Thử đổi từ khóa hoặc bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          page={currentPage}
          totalPages={totalPages}
          onChange={setPage}
          pageSize={pageSize}
          pageSizes={PAGE_SIZES}
          onPageSizeChange={(n) => {
            setPageSize(n)
            setPage(1)
          }}
        />
      </div>
    </div>
  )
}
