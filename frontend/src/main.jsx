import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import './style.css';
import { Providers, useAuth } from './context';
import Layout from './Layout';
import Login from './pages/Login';
import Home from './pages/Home';
import Services from './pages/Services';
import Caregivers from './pages/Caregivers';
import CaregiverDetail from './pages/CaregiverDetail';
import Bookings from './pages/Bookings';
import BookingDetail from './pages/BookingDetail';
import Profile from './pages/Profile';
import Availability from './pages/Availability';
import Reviews from './pages/Reviews';
import { AdminCaregivers, AdminUsers } from './pages/Admin';

function Protected({ roles, children }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <Providers>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<Protected><Layout /></Protected>}>
          <Route index element={<Home />} />
          <Route path="services" element={<Protected roles={['PATIENT']}><Services /></Protected>} />
          <Route path="caregivers" element={<Protected roles={['PATIENT', 'ADMIN']}><Caregivers /></Protected>} />
          <Route path="caregivers/:id" element={<CaregiverDetail />} />
          <Route path="bookings" element={<Bookings />} />
          <Route path="bookings/:id" element={<BookingDetail />} />
          <Route path="profile" element={<Profile />} />
          <Route path="availability" element={<Protected roles={['CAREGIVER']}><Availability /></Protected>} />
          <Route path="reviews" element={<Reviews />} />
          <Route path="admin/caregivers" element={<Protected roles={['ADMIN']}><AdminCaregivers /></Protected>} />
          <Route path="admin/users" element={<Protected roles={['ADMIN']}><AdminUsers /></Protected>} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Providers>
  </BrowserRouter>,
);
