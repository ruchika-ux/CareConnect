import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth, useLoad } from '../context';
import { Empty, ErrorBox, Loading, PageHeader, RequestCard } from '../ui';

const TABS = [['ALL', 'All'], ['REQUESTED', 'Awaiting'], ['ACCEPTED', 'Accepted'], ['IN_PROGRESS', 'In progress'], ['COMPLETED', 'Completed'], ['CLOSED', 'Cancelled / declined']];
const SUBS = {
  PATIENT: 'Every step of care, together in one place.',
  CAREGIVER: 'Accept visits that fit your schedule and support your community.',
  ADMIN: 'Review platform bookings and their current progress.',
};

export default function Bookings() {
  const { user } = useAuth();
  const [tab, setTab] = useState('ALL');
  const { data, loading, error, reload } = useLoad(() => api.get('/requests'));
  const match = r => tab === 'ALL' || (tab === 'CLOSED' ? ['CANCELLED', 'REJECTED'].includes(r.status) : r.status === tab);
  const rows = (data || []).filter(match);
  const count = key => (data || []).filter(r => key === 'ALL' || (key === 'CLOSED' ? ['CANCELLED', 'REJECTED'].includes(r.status) : r.status === key)).length;

  return (
    <>
      <PageHeader title={user.role === 'CAREGIVER' ? 'Care requests' : user.role === 'ADMIN' ? 'All bookings' : 'Your bookings'} sub={SUBS[user.role]} />
      <div className="tabs">{TABS.map(([k, l]) => <button key={k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)}>{l} <i>{count(k)}</i></button>)}</div>
      {loading ? <Loading /> : error ? <ErrorBox message={error} /> : rows.length
        ? <div className="list">{rows.map(r => <RequestCard key={r.id} r={r} onChange={reload} />)}</div>
        : <Empty title="No bookings to show yet.">{user.role === 'PATIENT' ? <>Ready when you are. <Link className="text-link" to="/services">Find care →</Link></> : 'Your care updates will appear here.'}</Empty>}
    </>
  );
}
