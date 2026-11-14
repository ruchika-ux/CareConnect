import { api } from '../api';
import { useAuth, useLoad } from '../context';
import { Empty, ErrorBox, Loading, PageHeader, Stars } from '../ui';

export default function Reviews() {
  const { user } = useAuth();
  const { data, loading, error } = useLoad(() => api.get('/reviews'));
  const avg = data?.length ? (data.reduce((s, r) => s + r.rating, 0) / data.length).toFixed(1) : null;
  const title = user.role === 'CAREGIVER' ? 'Reviews from families' : user.role === 'ADMIN' ? 'All reviews' : 'Reviews you wrote';

  return (
    <>
      <PageHeader title={title} sub={avg ? `Average rating ${avg} from ${data.length} review${data.length > 1 ? 's' : ''}.` : undefined} />
      {loading ? <Loading /> : error ? <ErrorBox message={error} /> : data.length ? (
        <div className="list">{data.map(r => (
          <article className="review-card" key={r.id}>
            <Stars value={r.rating} />
            <p>{r.comment || 'A family shared a rating after their completed visit.'}</p>
            <small>{user.role === 'PATIENT' ? `For ${r.caregiverName}` : r.patientName} · visit #{r.requestId}</small>
          </article>
        ))}</div>
      ) : <Empty icon="♡" title="Your care is remembered.">Reviews appear here after completed visits.</Empty>}
    </>
  );
}
