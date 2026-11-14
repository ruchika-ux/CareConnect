import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useLoad } from '../context';
import { Avatar, Empty, ErrorBox, Loading, PageHeader, ratingLabel } from '../ui';

export default function Caregivers() {
  const [f, setF] = useState({ serviceId: '', area: '', minRating: '', sort: 'rating' });
  const services = useLoad(() => api.get('/services'));
  const params = Object.fromEntries(Object.entries(f).filter(([, v]) => v !== ''));
  const { data, loading, error } = useLoad(() => api.get('/caregivers', params), [f]);
  const set = (k, v) => setF(x => ({ ...x, [k]: v }));

  return (
    <>
      <PageHeader title="Our caregivers" sub="Every caregiver here has been verified by our team." />
      <div className="filters">
        <label>Service
          <select value={f.serviceId} onChange={e => set('serviceId', e.target.value)}>
            <option value="">All services</option>
            {(services.data || []).map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </label>
        <label>Area<input value={f.area} onChange={e => set('area', e.target.value)} placeholder="e.g. Pune" /></label>
        <label>Min rating
          <select value={f.minRating} onChange={e => set('minRating', e.target.value)}>
            <option value="">Any</option><option value="3">3+</option><option value="4">4+</option><option value="4.5">4.5+</option>
          </select>
        </label>
        <label>Sort by
          <select value={f.sort} onChange={e => set('sort', e.target.value)}>
            <option value="rating">Top rated</option><option value="experience">Most experienced</option><option value="name">Name</option>
          </select>
        </label>
      </div>
      {loading ? <Loading /> : error ? <ErrorBox message={error} /> : data.length ? (
        <div className="cards">
          {data.map(c => (
            <Link to={`/caregivers/${c.id}`} className="caregiver-card" key={c.id}>
              <Avatar name={c.name} />
              <h3>{c.name}</h3>
              <p>{c.experienceYears} years · {ratingLabel(c)}</p>
              <small>{c.serviceArea || 'Area not set'}</small>
              <div className="chips">{c.services.slice(0, 3).map(s => <span className="chip" key={s}>{s}</span>)}</div>
            </Link>
          ))}
        </div>
      ) : <Empty title="No caregivers match those filters.">Try widening your search.</Empty>}
    </>
  );
}
