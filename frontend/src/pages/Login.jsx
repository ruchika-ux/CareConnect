import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { api, errorMessage } from '../api';
import { useAuth } from '../context';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ name: '', role: 'PATIENT', email: 'patient@careconnect.test', password: 'demo' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  if (user) return <Navigate to="/" replace />;

  const submit = async e => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      if (mode === 'register') await api.post('/auth/register', form);
      login(await api.post('/auth/login', { email: form.email, password: form.password }));
      navigate('/');
    } catch (err) { setError(errorMessage(err)); } finally { setLoading(false); }
  };

  const register = mode === 'register';
  return (
    <div className="login">
      <div className="login-visual">
        <div className="brand"><span>✚</span> CareConnect</div>
        <div>
          <span className="eyebrow">CARE THAT COMES HOME</span>
          <h1>Support for the<br />moments that matter.</h1>
          <p>Find dependable care, thoughtfully matched to your family and your day.</p>
        </div>
        <div className="visual-stamp">✳<small>HERE FOR<br />EVERY STEP</small></div>
      </div>
      <div className="login-panel">
        <div className="login-card">
          <span className="eyebrow">{register ? 'GET STARTED' : 'WELCOME BACK'}</span>
          <h2>{register ? 'Create your account.' : 'Good to have you here.'}</h2>
          <p>{register ? 'Join families and caregivers building better care at home.' : 'Sign in to coordinate care for the people you love.'}</p>
          <form onSubmit={submit}>
            {register && <>
              <label>Full name<input required value={form.name} onChange={e => set('name', e.target.value)} placeholder="Your name" /></label>
              <label>I am a
                <select value={form.role} onChange={e => set('role', e.target.value)}>
                  <option value="PATIENT">Patient or family member</option>
                  <option value="CAREGIVER">Caregiver</option>
                </select>
              </label>
            </>}
            <label>Email address<input required type="email" value={form.email} onChange={e => set('email', e.target.value)} placeholder="you@example.com" /></label>
            <label>Password<input required type="password" minLength={register ? 8 : 1} value={form.password} onChange={e => set('password', e.target.value)} placeholder={register ? 'At least 8 characters' : 'Enter your password'} /></label>
            {register && form.role === 'CAREGIVER' && <p className="hint">New caregiver profiles are reviewed by an admin before families can book you.</p>}
            {error && <div className="error">{error}</div>}
            <button className="primary" disabled={loading}>{loading ? 'Please wait…' : register ? 'Create account' : 'Sign in'} <span>→</span></button>
          </form>
          {!register && (
            <div className="demo"><b>Try the demo</b>
              <div>{[['Patient', 'patient@careconnect.test'], ['Caregiver', 'caregiver@careconnect.test'], ['Admin', 'admin@careconnect.test']].map(([label, mail]) =>
                <button key={mail} type="button" onClick={() => setForm(f => ({ ...f, email: mail, password: 'demo' }))}>{label}</button>)}</div>
              <small>Password: demo</small>
            </div>
          )}
          <div className="switch-mode">
            {register ? <>Already have an account? <button onClick={() => { setMode('login'); setError(''); }}>Sign in</button></>
              : <>New to CareConnect? <button onClick={() => { setMode('register'); setError(''); setForm(f => ({ ...f, email: '', password: '' })); }}>Create an account</button></>}
          </div>
        </div>
        <small className="login-foot">CARE, COORDINATED WITH HEART</small>
      </div>
    </div>
  );
}
