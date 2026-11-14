import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from './context';

const NAV = {
  PATIENT: [['/', 'Overview'], ['/services', 'Find care'], ['/caregivers', 'Caregivers'], ['/bookings', 'My bookings'], ['/profile', 'My profile']],
  CAREGIVER: [['/', 'Overview'], ['/bookings', 'Care requests'], ['/availability', 'Availability'], ['/profile', 'Profile & services'], ['/reviews', 'Reviews']],
  ADMIN: [['/', 'Platform overview'], ['/admin/caregivers', 'Caregiver verification'], ['/admin/users', 'Users'], ['/bookings', 'Bookings']],
};

export default function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className="shell">
      <aside>
        <div className="brand"><span>✚</span> CareConnect</div>
        <p className="welcome">WELCOME BACK<strong>{user.name}</strong></p>
        <nav>
          {NAV[user.role].map(([to, label]) => (
            <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>
              <span>◦</span>{label}
            </NavLink>
          ))}
        </nav>
        <div className="aside-note">Thoughtful care,<br />right at home.</div>
        <button className="logout" onClick={logout}>↗ &nbsp;Sign out</button>
      </aside>
      <main><Outlet /></main>
    </div>
  );
}
