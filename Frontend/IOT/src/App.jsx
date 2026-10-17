import { Routes, Route } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import Dashboard from './pages/Dashboard'
import DataSensor from './pages/DataSensor'
import ActivateHistory from './pages/ActivateHistory'
import Profile from './pages/Profile'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/data-sensor" element={<DataSensor />} />
        <Route path="/history" element={<ActivateHistory />} />
        <Route path="/profile" element={<Profile />} />
      </Route>
    </Routes>
  )
}