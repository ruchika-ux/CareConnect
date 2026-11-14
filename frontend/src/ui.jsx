import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from './api';
import { useAction, useAuth } from './context';

export const dateLabel = d =>
  d ? new Date(`${d}T00:00:00`).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }) : 'Date to be scheduled';

export const hhmm = t => (t ? t.slice(0, 5) : '');

export function timeRange(start, minutes) {
  if (!start) return '';
  const [h, m] = start.split(':').map(Number);
  const end = h * 60 + m + minutes;
  const pad = n => String(n).padStart(2, '0');
  return `${hhmm(start)}–${pad(Math.floor(end / 60) % 24)}:${pad(end % 60)}`;
}

export const ratingLabel = c => (c.reviewCount ? `★ ${c.rating.toFixed(1)} (${c.reviewCount})` : 'New');

export const Stars = ({ value }) => <span className="stars">{'★'.repeat(value)}{'☆'.repeat(5 - value)}</span>;
export const StatusBadge = ({ status }) => <span className={`status ${status.toLowerCase()}`}>{status.replace('_', ' ')}</span>;
export const Avatar = ({ name }) => <div className="match-avatar">{name?.[0]}</div>;
export const Loading = () => <div className="empty compact">Loading…</div>;
export const ErrorBox = ({ message }) => <div className="empty compact error-box">{message}</div>;

export function Empty({ icon = '✳', title, children }) {
  return <div className="empty"><span>{icon}</span><h3>{title}</h3>{children && <p>{children}</p>}</div>;
}

export function PageHeader({ eyebrow, title, sub }) {
  const { user } = useAuth();
  return (
    <>
      <header>
        <div><small>{eyebrow || 'CARE AT HOME'} <i>•</i> {user.role}</small><h1>{title}</h1></div>
        <div className="avatar">{user.name?.[0]}</div>
      </header>
      {sub && <p className="sub">{sub}</p>}
    </>
  );
}

export function Modal({ onClose, children }) {
  return (
    <div className="modal"><div className="modal-card">
      <button className="close" onClick={onClose} aria-label="Close">×</button>
      {children}
    </div></div>
  );
}

export function ReviewModal({ request, onClose, onDone }) {
  const { busy, run } = useAction();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const submit = async () => {
    const ok = await run(() => api.post(`/requests/${request.id}/reviews`, { rating, comment }), 'Thanks for your review');
    if (ok) { onClose(); onDone?.(); }
  };
  return (
    <Modal onClose={onClose}>
      <span className="eyebrow">HOW WAS THE VISIT?</span>
      <h2>Review {request.caregiverName}</h2>
      <div className="star-picker">
        {[1, 2, 3, 4, 5].map(n => (
          <button key={n} type="button" className={n <= rating ? 'on' : ''} onClick={() => setRating(n)} aria-label={`${n} stars`}>★</button>
        ))}
      </div>
      <label>Share a short note (optional)
        <textarea value={comment} onChange={e => setComment(e.target.value)} maxLength={2000} placeholder="What went well?" />
      </label>
      <button className="primary full" disabled={busy} onClick={submit}>Submit review <span>→</span></button>
    </Modal>
  );
}

/** Role-aware action buttons for a booking; used on the list card and the detail page. */
export function BookingActions({ r, onDone }) {
  const { user } = useAuth();
  const { busy, run } = useAction();
  const [notes, setNotes] = useState('');
  const [reviewing, setReviewing] = useState(false);
  const post = (path, body, ok) => run(async () => { await api.post(`/requests/${r.id}/${path}`, body); await onDone?.(); }, ok);

  return (
    <>
      {user.role === 'CAREGIVER' && r.status === 'REQUESTED' && (
        <div className="actions">
          <button className="tiny accept" disabled={busy} onClick={() => post('decision', { action: 'ACCEPT' }, 'Booking accepted')}>Accept</button>
          <button className="tiny" disabled={busy} onClick={() => post('decision', { action: 'REJECT' }, 'Booking declined')}>Decline</button>
        </div>
      )}
      {user.role === 'CAREGIVER' && r.status === 'ACCEPTED' &&
        <button className="tiny accept" disabled={busy} onClick={() => post('start', null, 'Visit started')}>Start visit</button>}
      {user.role === 'CAREGIVER' && r.status === 'IN_PROGRESS' && (
        <div className="complete-action">
          <input placeholder="Visit notes / tasks completed" value={notes} onChange={e => setNotes(e.target.value)} />
          <button className="tiny accept" disabled={busy} onClick={() => post('complete', { notes }, 'Visit completed')}>Complete visit</button>
        </div>
      )}
      {user.role !== 'CAREGIVER' && ['REQUESTED', 'ACCEPTED'].includes(r.status) && (user.role === 'ADMIN' || user.id === r.patientId) &&
        <button className="tiny" disabled={busy} onClick={() => post('cancel', null, 'Booking cancelled')}>Cancel booking</button>}
      {user.role === 'PATIENT' && r.status === 'COMPLETED' && !r.reviewed && (
        <>
          <button className="tiny accept" onClick={() => setReviewing(true)}>Leave a review</button>
          {reviewing && <ReviewModal request={r} onClose={() => setReviewing(false)} onDone={onDone} />}
        </>
      )}
    </>
  );
}

export function RequestCard({ r, onChange }) {
  const { user } = useAuth();
  return (
    <article className="request">
      <div className="request-main">
        <div className="service-icon small">{r.status === 'COMPLETED' ? '✓' : '✿'}</div>
        <div className="request-copy">
          <span className="eyebrow">{r.serviceName}</span>
          <h3><Link to={`/bookings/${r.id}`}>{dateLabel(r.requestedDate)}</Link> <span>· {timeRange(r.startTime, r.durationMinutes)}</span></h3>
          <p>{r.location}{r.careRequirements && ` · ${r.careRequirements}`}</p>
          {r.caregiverName && user.role !== 'CAREGIVER' && <small>Caregiver · {r.caregiverName}</small>}
          {user.role !== 'PATIENT' && <small>Family · {r.patientName}</small>}
          {r.visitNotes && <small className="visit-notes">Visit note · {r.visitNotes}</small>}
        </div>
      </div>
      <div className="request-side">
        <StatusBadge status={r.status} />
        <BookingActions r={r} onDone={onChange} />
      </div>
    </article>
  );
}
