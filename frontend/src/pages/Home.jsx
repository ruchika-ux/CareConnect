import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth, useLoad } from '../context';
import { Empty, Loading, PageHeader, RequestCard } from '../ui';
import { AdminDashboard } from './Admin';

export default function Home() {
  const { user } = useAuth();
  return user.role === 'ADMIN' ? <AdminDashboard /> : <MemberHome user={user} />;
}

function MemberHome({ user }) {
  const patient = user.role === 'PATIENT';
  const { data, loading, reload } = useLoad(() => api.get('/requests'));
  const live = (data || []).filter(r => !['COMPLETED', 'REJECTED', 'CANCELLED'].includes(r.status));
  const awaiting = (data || []).filter(r => r.status === 'REQUESTED' && r.caregiverId).length;

  return (
    <>
      <PageHeader title="Your care, coordinated." />
      <section className="hero">
        <div>
          <span className="eyebrow">{patient ? 'YOUR CARE JOURNEY' : 'YOUR CARE COMMUNITY'}</span>
          <h2>{patient ? 'The right support, in the comfort of home.' : 'Make every visit a moment of care.'}</h2>
          <p>{patient ? 'From daily companionship to recovery support, find care that fits your family.'
            : awaiting ? `You have ${awaiting} request${awaiting > 1 ? 's' : ''} waiting for your response.` : 'See the families who need support, manage your availability, and keep care moving.'}</p>
          <Link className="primary link-btn" to={patient ? '/services' : '/bookings'}>{patient ? 'Explore care' : 'Review requests'} <span>→</span></Link>
        </div>
        <div className="hero-art"><span className="art-sun">✳</span><div className="art-home">⌂</div><small>CARE<br />STARTS HERE</small></div>
      </section>
      <div className="section-title">
        <div><span className="eyebrow">{patient ? 'A LITTLE PEACE OF MIND' : 'ON YOUR RADAR'}</span><h2>{patient ? 'Your upcoming care' : 'Your active visits'}</h2></div>
        <Link className="text-link" to="/bookings">View all →</Link>
      </div>
      {loading ? <Loading /> : live.length
        ? <div className="list">{live.slice(0, 3).map(r => <RequestCard key={r.id} r={r} onChange={reload} />)}</div>
        : <Empty title="Your next step starts here.">{patient ? 'When you request care, your booking details will show up here.' : 'New care requests will appear here as families book you.'}</Empty>}
    </>
  );
}
