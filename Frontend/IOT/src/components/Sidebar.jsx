import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Database, History, User, Activity } from 'lucide-react'

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/data-sensor', label: 'Data Sensor', icon: Database },
  { to: '/history', label: 'Active History', icon: History },
  { to: '/profile', label: 'Profile', icon: User },
]

export default function Sidebar() {
  return (
    <aside className="flex w-60 shrink-0 flex-col bg-slate-900 text-slate-300">
      {/* Logo */}
      <div className="flex items-center gap-3 border-b border-slate-800 px-5 py-5">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500 text-white">
          <Activity size={20} />
        </div>
        <div>
          <p className="text-sm font-semibold text-white">IoT Monitor</p>
          <p className="text-xs text-slate-500">System Dashboard</p>
        </div>
      </div>

      {/* Menu */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        <p className="px-3 pb-2 text-xs text-slate-500">Navigation</p>
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors ${
                isActive
                  ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/30'
                  : 'hover:bg-slate-800 hover:text-white'
              }`
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Trạng thái hệ thống */}
      <div className="flex items-center gap-2 border-t border-slate-800 px-5 py-4 text-xs text-slate-400">
        <span className="h-2 w-2 rounded-full bg-emerald-400" />
        System Online
      </div>
    </aside>
  )
}
