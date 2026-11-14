import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth, useLoad } from '../context';
import { Avatar, ErrorBox, Loading, PageHeader, Stars, hhmm, ratingLabel } from '../ui';

export const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export default function CaregiverDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const { data, loading, error } = useLoad(() => api.get(`/caregivers/${id}`), [id]);
  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;
  const { caregiver: c, availability, reviews } = data;

  return (
    <>
      <PageHeader eyebrow="CAREGIVER PROFILE" title={c.name} />
      <div className="detail-grid">
        <section className="panel">
          <div className="detail-head">
            <Avatar name={c.name} />
            <div>
              <h3>{c.name} {c.verified ? <span className="status completed">VERIFIED</span> : <span className="status pending">PENDING</span>}</h3>
              <p>{c.experienceYears} years of experience · {ratingLabel(c)} · {c.serviceArea || 'Area not set'}</p>
            </div>
          </div>
          {c.bio && <p className="bio">{c.bio}</p>}
          <span className="eyebrow">SERVICES</span>
          <div className="chips">{c.services.length ? c.services.map(s => <span className="chip" key={s}>{s}</span>) : <small>No services selected yet.</small>}</div>
          {user.role === 'PATIENT' && <Link className="primary link-btn" to="/services">Request care <span>→</span></Link>}
        </section>

        <section className="panel">
          <span className="eyebrow">WEEKLY AVAILABILITY</span>
          <div className="week-view">
            {DAYS.map((d, i) => {
              const w = availability.filter(a => a.dayOfWeek === i + 1);
              return <div key={d}><b>{d.slice(0, 3)}</b><span>{w.length ? w.map(x => `${hhmm(x.startTime)}–${hhmm(x.endTime)}`).join(', ') : 'Off'}</span></div>;
            })}
          </div>
          {!c.available && <small>Currently not taking new visits.</small>}
        </section>
      </div>

      <div className="section-title"><div><span className="eyebrow">WHAT FAMILIES SAY</span><h2>Reviews ({reviews.length})</h2></div></div>
      {reviews.length ? <div className="list">{reviews.map(r => (
        <article className="review-card" key={r.id}><Stars value={r.rating} /><p>{r.comment || 'A family shared a rating after their completed visit.'}</p><small>{r.patientName}</small></article>
      ))}</div> : <div className="empty compact">No reviews yet. Reviews appear after completed visits.</div>}
    </>
  );
}
