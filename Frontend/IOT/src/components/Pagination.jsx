import { ChevronLeft, ChevronRight } from 'lucide-react'

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

export default function Pagination({
  page,
  totalPages,
  onChange,
  pageSize,
  pageSizes = [5, 10, 20, 50],
  onPageSizeChange,
}) {
 return (
    <div className="flex shrink-0 items-center justify-between border-t border-slate-100 px-5 py-3">
      <div className="flex items-center gap-4">
        <p className="text-xs text-slate-500">Page {page} / {totalPages}</p>

        {onPageSizeChange && (
          <label className="flex items-center gap-1.5 text-xs text-slate-500">
            Page size
            <select
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="h-8 rounded-lg border border-slate-200 bg-slate-50 px-2 text-xs text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {pageSizes.map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </label>
        )}
      </div>

      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => onChange(page - 1)}
          disabled={page === 1}
          aria-label="Trang trước"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40"
        >
          <ChevronLeft size={16} />
        </button>

        {getPages(page, totalPages).map((p, i) =>
          p === '…' ? (
            <span key={`dots-${i}`} className="px-1 text-slate-400">…</span>
          ) : (
            <button
              key={p}
              type="button"
              onClick={() => onChange(p)}
              aria-current={p === page ? 'page' : undefined}
              className={`h-8 min-w-8 rounded-lg px-2 text-sm ${
                p === page ? 'bg-blue-600 font-medium text-white' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              {p}
            </button>
          )
        )}

        <button
          type="button"
          onClick={() => onChange(page + 1)}
          disabled={page === totalPages}
          aria-label="Trang sau"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40"
        >
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  )
}