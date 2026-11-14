import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAction, useAuth, useLoad } from '../context';
import { PageHeader } from '../ui';

export default function Profile() {
  const { user, updateUser } = useAuth();
  const caregiver = user.role === 'CAREGIVER';
  const services = useLoad(() => api.get('/services'));
  const { busy, run } = useAction();
  const [form, setForm] = useState(null);

  // Load the latest saved profile (including chosen services) rather than the cached session copy.
  useEffect(() => {
    api.get('/profile').then(p => setForm({
      name: p.name, phone: p.phone || '', bio: p.bio || '', dateOfBirth: p.dateOfBirth || '', serviceArea: p.serviceArea || '',
      experienceYears: p.experienceYears, available: p.available, serviceIds: p.serviceIds, verified: p.verified,
    }));
  }, []);
  if (!form) return <PageHeader title="Your profile" />;

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const toggle = id => set('serviceIds', form.serviceIds.includes(id) ? form.serviceIds.filter(x => x !== id) : [...form.serviceIds, id]);
  const save = e => {
    e.preventDefault();
    const { verified, ...body } = form;
    if (!body.dateOfBirth) delete body.dateOfBirth;
    if (!caregiver) { delete body.serviceIds; delete body.serviceArea; delete body.experienceYears; delete body.available; }
    run(async () => updateUser(await api.put('/profile', body)));
  };

  return (
    <>
      <PageHeader title={caregiver ? 'Caregiver profile' : 'Your profile'} sub={caregiver ? 'Let families know about your experience and the care you provide.' : 'Keep your contact information up to date.'} />
      <form className="profile-form" onSubmit={save}>
        <span className="eyebrow">YOUR DETAILS</span>
        <h2>{caregiver ? 'About you' : 'Personal details'}</h2>
        <div className="form-row">
          <label>Full name<input value={form.name} onChange={e => set('name', e.target.value)} /></label>
          <label>Phone number<input value={form.phone} onChange={e => set('phone', e.target.value)} /></label>
        </div>
        <div className="form-row">
          <label>Date of birth<input type="date" max={new Date().toLocaleDateString('en-CA')} value={form.dateOfBirth} onChange={e => set('dateOfBirth', e.target.value)} /></label>
          <label>Email<input value={user.email} disabled /></label>
        </div>
        <label>{caregiver ? 'Bio' : 'About'}<textarea value={form.bio} onChange={e => set('bio', e.target.value)} placeholder="A little about you" /></label>
        {caregiver && <>
          <div className="form-row">
            <label>Service area<input value={form.serviceArea} onChange={e => set('serviceArea', e.target.value)} placeholder="City or neighbourhood, e.g. Pune" /></label>
            <label>Years of experience<input type="number" min="0" max="60" value={form.experienceYears} onChange={e => set('experienceYears', Number(e.target.value))} /></label>
          </div>
          <fieldset className="service-picker">
            <legend>Services you provide</legend>
            <div>{(services.data || []).map(s => <label key={s.id}><input type="checkbox" checked={form.serviceIds.includes(s.id)} onChange={() => toggle(s.id)} />{s.name}</label>)}</div>
            <small>Families can only match you for services you select here.</small>
          </fieldset>
          <label className="availability-toggle">
            <input type="checkbox" checked={form.available} onChange={e => set('available', e.target.checked)} />
            <span><b>Available for new visits</b><small>Turn this off when your schedule is full. Set your working hours under Availability.</small></span>
          </label>
          <div className="verification-note">{form.verified ? '✓ Your caregiver profile is verified.' : 'Your profile is waiting for admin verification. You can keep editing while you wait.'}</div>
        </>}
        <button className="primary" disabled={busy}>Save changes <span>→</span></button>
      </form>
    </>
  );
}
