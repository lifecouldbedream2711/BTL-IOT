import { Outlet } from 'react-router-dom'
import Sidebar from '../components/Sidebar'

export default function MainLayout() {
  return (
    <div className="flex h-screen overflow-hidden bg-slate-50">
      <Sidebar />
      <main className="min-w-0 flex-1 overflow-hidden p-3 md:p-4 flex flex-col">
        <Outlet />
      </main>
    </div>
  )
}