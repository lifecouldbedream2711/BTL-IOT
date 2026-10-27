import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { api } from '../services/api'

export default function ProtectedRoute() {
  const location = useLocation()
  const isAuth = api.isAuthenticated()

  if (!isAuth) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
