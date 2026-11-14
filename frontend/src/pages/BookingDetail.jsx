import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useLoad } from '../context';
import { BookingActions, ErrorBox, Loading, PageHeader, Stars, StatusBadge, dateLabel, timeRange } from '../ui';

const STEPS = [['REQUESTED', 'Requested'], ['ACCEPTED', 'Accepted'], ['IN_PROGRESS', 'Visit in progress'], ['COMPLETED', 'Completed']];

export default function BookingDetail() {
  const { id } = useParams();
  const { data: r, loading, error, reload } = useLoad(() => api.get(`/requests/${id}`), [id]);
  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;
  const closed = ['CANCELLED', 'REJECTED'].includes(r.status);
  const at = STEPS.findIndex(([k]) => k === r.status);
  const awaitingCaregiver = r.status === 'REQUESTED' && !r.caregiverId;

  return (
    <>
      <PageHeader eyebrow={`BOOKING #${r.id}`} title={r.serviceName} />
      <div className="detail-grid">
        <section className="panel">
          <div className="detail-row"><span>When</span><b>{dateLabel(r.requestedDate)} · {timeRange(r.startTime, r.durationMinutes)}</b></div>
          <div className="detail-row"><span>Where</span><b>{r.location}</b></div>
          <div className="detail-row"><span>Family</span><b>{r.patientName}</b></div>
          <div className="detail-row"><span>Caregiver</span><b>{r.caregiverId ? <Link to={`/caregivers/${r.caregiverId}`}>{r.caregiverName}</Link> : 'Not assigned yet'}</b></div>
          {r.careRequirements && <div className="detail-row"><span>Care needs</span><b>{r.careRequirements}</b></div>}
          {r.matchReason && <div className="detail-row"><span>Why matched</span><b>{r.matchReason}</b></div>}
          {r.visitNotes && <div className="detail-row"><span>Visit notes</span><b>{r.visitNotes}</b></div>}
          {r.reviewed && <div className="detail-row"><span>Review</span><b><Stars value={r.reviewRating} /> {r.reviewComment}</b></div>}
          {awaitingCaregiver && <p className="hint">No caregiver has been selected for this request yet.</p>}
        </section>
        <section className="panel">
          <span className="eyebrow">PROGRESS</span>
          {closed ? <p><StatusBadge status={r.status} /></p> : (
            <ol className="timeline">{STEPS.map(([k, l], i) => <li key={k} className={i <= at ? 'done' : ''}>{l}</li>)}</ol>
          )}
          <div className="detail-actions"><BookingActions r={r} onDone={reload} /></div>
        </section>
      </div>
    </>
  );
}
