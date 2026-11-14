import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAction, useLoad } from '../context';
import { Empty, ErrorBox, Loading, PageHeader, StatusBadge } from '../ui';

const STAT_CARDS = [
  ['users', 'USERS', 'People in the community'], ['patients', 'FAMILIES', 'Patients and family members'],
  ['caregivers', 'CAREGIVERS', 'Care professionals'], ['pendingVerification', 'TO VERIFY', 'Caregivers awaiting review'],
  ['requests', 'BOOKINGS', 'Total care requests'], ['awaitingResponse', 'AWAITING', 'Waiting on a caregiver'],
  ['active', 'ACTIVE', 'Accepted or in progress'], ['completed', 'COMPLETED', 'Visits completed'],
];

function useVerify(reload) {
  const { busy, run } = useAction();
  const toggle = c => run(async () => { await api.put(`/admin/caregivers/${c.id}/verification`, null, { verified: !c.verified }); await reload(); }, c.verified ? 'Verification revoked' : 'Caregiver verified');
  return { busy, toggle };
}

export function AdminDashboard() {
  const stats = useLoad(() => api.get('/admin/stats'));
  const cg = useLoad(() => api.get('/admin/caregivers'));
  const reload = () => Promise.all([stats.reload(), cg.reload()]);
  const { busy, toggle } = useVerify(reload);
  const pending = (cg.data || []).filter(c => !c.verified);

  return (
    <>
      <PageHeader title="Platform overview" sub="A clear view of the care activity across your community." />
      {stats.error ? <ErrorBox message={stats.error} /> : (
        <div className="stats">{STAT_CARDS.map(([k, label, desc]) => <div key={k}><span className="eyebrow">{label}</span><b>{stats.data?.[k] ?? '—'}</b><small>{desc}</small></div>)}</div>
      )}
      <div className="section-title"><div><span className="eyebrow">CARE COMMUNITY</span><h2>Verification queue</h2></div><Link className="text-link" to="/admin/caregivers">Manage caregivers →</Link></div>
      {cg.loading ? <Loading /> : pending.length ? <div className="list">{pending.map(c => (
        <div className="request" key={c.id}>
          <div><h3><Link to={`/caregivers/${c.id}`}>{c.name}</Link></h3><p>{c.experienceYears} years · {c.serviceArea || 'No area set'} · {c.services.join(', ') || 'No services selected'}</p></div>
          <button className="tiny accept" disabled={busy} onClick={() => toggle(c)}>Verify profile</button>
        </div>))}</div>
        : <div className="empty compact">All caregiver profiles have been reviewed.</div>}
    </>
  );
}

export function AdminCaregivers() {
  const { data, loading, error, reload } = useLoad(() => api.get('/admin/caregivers'));
  const { busy, toggle } = useVerify(reload);
  return (
    <>
      <PageHeader title="Caregiver verification" sub="Review caregiver profiles before they can be matched with families." />
      {loading ? <Loading /> : error ? <ErrorBox message={error} /> : data.length ? (
        <div className="list">{data.map(c => (
          <article className="request" key={c.id}>
            <div className="request-main">
              <div className="match-avatar">{c.name[0]}</div>
              <div>
                <h3><Link to={`/caregivers/${c.id}`}>{c.name}</Link></h3>
                <p>{c.experienceYears} years · {c.serviceArea || 'No service area'} · {c.email}</p>
                <small>{c.services.join(', ') || 'No services selected yet'}</small>
              </div>
            </div>
            <div className="request-side">
              <span className={`status ${c.verified ? 'completed' : 'pending'}`}>{c.verified ? 'VERIFIED' : 'PENDING'}</span>
              <button className="tiny accept" disabled={busy} onClick={() => toggle(c)}>{c.verified ? 'Revoke' : 'Verify profile'}</button>
            </div>
          </article>))}</div>
      ) : <Empty title="No caregivers have registered yet." />}
    </>
  );
}

export function AdminUsers() {
  const { data, loading, error, reload } = useLoad(() => api.get('/admin/users'));
  const { busy, run } = useAction();
  const setEnabled = u => run(async () => { await api.put(`/admin/users/${u.id}/enabled`, null, { enabled: !u.enabled }); await reload(); }, u.enabled ? 'Account disabled' : 'Account enabled');
  return (
    <>
      <PageHeader title="People on CareConnect" sub="People using CareConnect to coordinate home care." />
      {loading ? <Loading /> : error ? <ErrorBox message={error} /> : (
        <div className="list">{data.map(u => (
          <article className="request" key={u.id}>
            <div><h3>{u.name}</h3><p>{u.email} · {u.phone || 'No phone added'}</p></div>
            <div className="request-side">
              <span className="status">{u.role}</span>
              {!u.enabled && <StatusBadge status="CANCELLED" />}
              {u.role !== 'ADMIN' && <button className="tiny" disabled={busy} onClick={() => setEnabled(u)}>{u.enabled ? 'Disable' : 'Enable'}</button>}
            </div>
          </article>))}</div>
      )}
    </>
  );
}
