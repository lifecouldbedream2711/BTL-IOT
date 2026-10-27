import { useState, useEffect } from 'react'
import { FileText, ExternalLink, BookOpen } from 'lucide-react'
import PageHeader from '../components/PageHeader'
import { fetchUserProfile } from '../services/api'

// Thông tin tài khoản mặc định
const defaultUser = {
  name: 'Phạm Ngọc Quynh',
  avatar: '/avatar.png',
  fields: [
    { label: 'Full name', value: 'Phạm Ngọc Quynh' },
    { label: 'Email', value: 'phampt987321@gmail.com' },
    { label: 'Student ID', value: 'B23DCCN704' },
    { label: 'University', value: 'PTIT' },
    { label: 'Location', value: 'Hanoi, Vietnam' },
    { label: 'Member since', value: 'September 2023' },
  ],
}

// Logo thương hiệu vẽ bằng SVG để không phụ thuộc phiên bản lucide-react
function GithubLogo() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor" aria-hidden="true">
      <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
    </svg>
  )
}

function FigmaLogo() {
  return (
    <svg viewBox="0 0 38 57" className="h-5" aria-hidden="true">
      <path d="M19 28.5a9.5 9.5 0 1 1 19 0 9.5 9.5 0 0 1-19 0z" fill="#1abcfe" />
      <path d="M0 47.5A9.5 9.5 0 0 1 9.5 38H19v9.5a9.5 9.5 0 1 1-19 0z" fill="#0acf83" />
      <path d="M19 0v19h9.5a9.5 9.5 0 1 0 0-19H19z" fill="#ff7262" />
      <path d="M0 9.5A9.5 9.5 0 0 0 9.5 19H19V0H9.5A9.5 9.5 0 0 0 0 9.5z" fill="#f24e1e" />
      <path d="M0 28.5A9.5 9.5 0 0 0 9.5 38H19V19H9.5A9.5 9.5 0 0 0 0 28.5z" fill="#a259ff" />
    </svg>
  )
}

const resources = [
  {
    title: 'GitHub Repository',
    desc: 'View source code, issues, and pull requests',
    action: 'View Repository',
    href: 'https://github.com/lifecouldbedream2711/BTL-IOT.git',
    logo: <GithubLogo />,
    card: 'border-slate-200',
    button: 'bg-slate-50 text-slate-800 hover:bg-slate-100 ring-slate-200',
  },
  {
    title: 'Figma Design',
    desc: 'UI/UX design files and component library',
    action: 'Open in Figma',
    href: 'https://www.figma.com/make/hgvzIKmbVtEsI5hlIKPDng/Modern-IoT-Monitoring-UI?p=f&t=8fmLKemjXWZFmsEF-0',
    logo: <FigmaLogo />,
    card: 'border-slate-200',
    button: 'bg-purple-50 text-purple-700 hover:bg-purple-100 ring-purple-200',
  },
  {
    title: 'Swagger & API Docs',
    desc: 'OpenAPI Swagger UI documentation and live testing',
    action: 'Open Swagger UI',
    href: 'http://localhost:8080/swagger-ui.html',
    logo: <FileText size={20} className="text-red-500" />,
    card: 'border-red-200',
    button: 'bg-red-50 text-red-600 hover:bg-red-100 ring-red-200',
  },
  {
    title: 'Documentation',
    desc: 'Project report, setup guide and API documents',
    action: 'Open Document',
    href: 'http://localhost:8080/v3/api-docs',
    logo: <BookOpen size={20} className="text-emerald-600" />,
    card: 'border-emerald-200',
    button: 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 ring-emerald-200',
  },
]

export default function Profile() {
  const [user, setUser] = useState(defaultUser)

  useEffect(() => {
    fetchUserProfile().then((data) => {
      if (data) {
        setUser((prev) => ({
          name: data.name || prev.name,
          avatar: data.avatar || prev.avatar,
          fields: (data.fields && data.fields.length > 0) ? data.fields : prev.fields,
        }))
      }
    })
  }, [])

  const initial = user.name.trim().split(' ').pop()?.[0] ?? 'Q'

  return (
    <div className="flex h-full flex-col gap-4 overflow-y-auto">
      <PageHeader title="Profile" subtitle="Account settings and resources" />

      {/* Thẻ thông tin cá nhân */}
      <section className="shrink-0 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-100">
        <div className="flex items-center gap-4">
          {user.avatar ? (
            <img src={user.avatar} alt={user.name} className="h-16 w-16 rounded-full object-cover" />
          ) : (
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-sky-500 text-2xl font-semibold text-white">
              {initial}
            </div>
          )}
          <div>
            <p className="text-lg font-semibold text-slate-900">{user.name}</p>
            <div className="mt-1 flex items-center gap-2">
              <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-600 ring-1 ring-blue-200">
                PTIT Student
              </span>
            </div>
          </div>
        </div>

        {/* Lưới 2 cột; khe 1px màu xám tạo thành đường kẻ giữa các ô */}
        <dl className="mt-4 grid grid-cols-2 gap-px overflow-hidden rounded-xl bg-slate-200 ring-1 ring-slate-200">
          {user.fields.map((f) => (
            <div key={f.label} className="bg-white px-4 py-2.5">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{f.label}</dt>
              <dd className="mt-0.5 text-sm font-medium text-slate-900">{f.value}</dd>
            </div>
          ))}
        </dl>
      </section>

      {/* Tài nguyên dự án */}
      <section className="shrink-0">
        <h2 className="text-base font-semibold text-slate-900">Project Resources</h2>
        <p className="mb-3 text-xs text-slate-500">Quick access to project assets and documentation</p>

        <div className="grid grid-cols-2 gap-4">
          {resources.map((r) => (
            <div key={r.title} className={`rounded-2xl border bg-white p-4 shadow-sm ${r.card}`}>
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-50 ring-1 ring-slate-200">
                {r.logo}
              </div>
              <p className="mt-3 text-sm font-semibold text-slate-900">{r.title}</p>
              <p className="mt-0.5 text-xs text-slate-500">{r.desc}</p>
              <a
                href={r.href}
                target="_blank"
                rel="noopener noreferrer"
                className={`mt-3 flex h-9 items-center justify-center gap-1.5 rounded-lg text-sm font-medium ring-1 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${r.button}`}
              >
                {r.action}
                <ExternalLink size={13} />
              </a>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
