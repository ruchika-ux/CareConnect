import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAction, useLoad } from '../context';
import { Avatar, Empty, ErrorBox, Loading, Modal, PageHeader, ratingLabel } from '../ui';

const DURATIONS = [[30, '30 minutes'], [60, '1 hour'], [120, '2 hours'], [240, '4 hours'], [480, '8 hours'], [720, '12 hours']];
const MAX = { Service: 35, Availability: 30, Area: 15, Experience: 10, Rating: 10 };
const ICONS = ['✿', '✚', '☼', '♡', '✳'];
const todayISO = () => new Date().toLocaleDateString('en-CA');

export default function Services() {
  const { data, loading, error } = useLoad(() => api.get('/services'));
  const [selected, setSelected] = useState(null);
  return (
    <>
      <PageHeader title="Find the right care" sub="A little help can make a big difference. Choose the support your family needs." />
      {loading ? <Loading /> : error ? <ErrorBox message={error} /> : (
        <div className="service-grid">
          {data.map((s, i) => (
            <article className="service" key={s.id}>
              <div className="service-top"><span className="service-icon">{ICONS[i % 5]}</span><span className="service-index">0{i + 1}</span></div>
              <span className="eyebrow">CARE AT HOME</span>
              <h3>{s.name}</h3><p>{s.description}</p>
              <button className="outline" onClick={() => setSelected(s)}>Request this care <span>→</span></button>
            </article>
          ))}
        </div>
      )}
      {selected && <RequestModal service={selected} onClose={() => setSelected(null)} />}
    </>
  );
}

function RequestModal({ service, onClose }) {
  const navigate = useNavigate();
  const { busy, run } = useAction();
  const [form, setForm] = useState({ requestedDate: '', startTime: '09:00', durationMinutes: 120, location: '', careRequirements: '' });
  const [requestId, setRequestId] = useState(null);
  const [matches, setMatches] = useState(null);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const find = () => run(async () => {
    const r = await api.post('/requests', { ...form, serviceId: service.id, durationMinutes: Number(form.durationMinutes) });
    setRequestId(r.id);
    setMatches(await api.get(`/requests/${r.id}/matches`));
  }, '');

  const book = caregiverId => run(async () => {
    try { await api.post(`/requests/${requestId}/book`, null, { caregiverId }); }
    catch (e) { setMatches(await api.get(`/requests/${requestId}/matches`)); throw e; }
    navigate(`/bookings/${requestId}`);
  }, 'Booking requested. The caregiver will confirm shortly.');

  // The request is created before matching, so abandoning the match list withdraws it.
  const withdraw = async () => { if (requestId) { try { await api.post(`/requests/${requestId}/cancel`); } catch { /* already gone */ } } };
  const changeDetails = () => run(async () => { await withdraw(); setMatches(null); setRequestId(null); }, '');
  const close = async () => { if (matches) await withdraw(); onClose(); };

  return (
    <Modal onClose={close}>
      {!matches ? (
        <>
          <span className="eyebrow">LET'S FIND YOUR MATCH</span>
          <h2>Plan {service.name.toLowerCase()}</h2>
          <p>Tell us when and where care would help most.</p>
          <label>Date<input type="date" min={todayISO()} value={form.requestedDate} onChange={e => set('requestedDate', e.target.value)} /></label>
          <div className="form-row">
            <label>Start time<input type="time" value={form.startTime} onChange={e => set('startTime', e.target.value)} /></label>
            <label>Duration<select value={form.durationMinutes} onChange={e => set('durationMinutes', e.target.value)}>{DURATIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></label>
          </div>
          <label>Care location<input value={form.location} onChange={e => set('location', e.target.value)} placeholder="Area or neighborhood, e.g. Kothrud, Pune" /></label>
          <label>What support would help?<textarea value={form.careRequirements} maxLength={2000} onChange={e => set('careRequirements', e.target.value)} placeholder="Care requirements only. Please don't include diagnoses or medical records." /></label>
          <button className="primary full" disabled={busy || !form.requestedDate || !form.startTime || !form.location.trim()} onClick={find}>Find my caregivers <span>→</span></button>
        </>
      ) : (
        <>
          <span className="eyebrow">YOUR MATCHES</span>
          <h2>Caregivers for your visit</h2>
          <p>Verified professionals who offer {service.name} and are free at your chosen time. Here is why each one matched.</p>
          <div className="match-list">
            {matches.length ? matches.map(m => (
              <article className="match-card stacked" key={m.caregiver.id}>
                <div className="match-top">
                  <Avatar name={m.caregiver.name} />
                  <div className="match-info">
                    <h3>{m.caregiver.name}<span>{m.score}% match</span></h3>
                    <p>{m.caregiver.experienceYears} yrs · {ratingLabel(m.caregiver)} · {m.caregiver.serviceArea || 'Area not set'}</p>
                  </div>
                  <button className="tiny accept" disabled={busy} onClick={() => book(m.caregiver.id)}>Book</button>
                </div>
                <div className="chips">{Object.entries(m.breakdown).map(([k, v]) => <span key={k} className={`chip ${v === 0 ? 'zero' : ''}`}>{k} {v}/{MAX[k]}</span>)}</div>
                <ul className="reasons">{m.reasons.map(r => <li key={r}>{r}</li>)}</ul>
                <Link className="text-link" to={`/caregivers/${m.caregiver.id}`} target="_blank">View full profile ↗</Link>
              </article>
            )) : <Empty title="No caregivers available for that slot yet.">Try another date or time and we'll look again.</Empty>}
          </div>
          <button className="text-link" disabled={busy} onClick={changeDetails}>← Change visit details</button>
        </>
      )}
    </Modal>
  );
}
