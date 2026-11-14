import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAction } from '../context';
import { Loading, PageHeader, hhmm } from '../ui';
import { DAYS } from './CaregiverDetail';

export default function Availability() {
  const [windows, setWindows] = useState(null);
  const { busy, run } = useAction();

  useEffect(() => {
    api.get('/availability').then(list => setWindows(list.map(w => ({ dayOfWeek: w.dayOfWeek, startTime: hhmm(w.startTime), endTime: hhmm(w.endTime) }))));
  }, []);
  if (!windows) return <><PageHeader title="Availability" /><Loading /></>;

  const add = day => setWindows(w => [...w, { dayOfWeek: day, startTime: '09:00', endTime: '17:00' }]);
  const change = (idx, k, v) => setWindows(w => w.map((x, i) => (i === idx ? { ...x, [k]: v } : x)));
  const remove = idx => setWindows(w => w.filter((_, i) => i !== idx));
  const preset = (days, s, e) => setWindows(Array.from({ length: days }, (_, i) => ({ dayOfWeek: i + 1, startTime: s, endTime: e })));
  const invalid = windows.some(w => !w.startTime || !w.endTime || w.endTime <= w.startTime);
  const save = () => run(async () => setWindows((await api.put('/availability', windows)).map(w => ({ dayOfWeek: w.dayOfWeek, startTime: hhmm(w.startTime), endTime: hhmm(w.endTime) }))), 'Availability saved');

  return (
    <>
      <PageHeader title="Working hours" sub="Families are only matched with you for visits that fit entirely inside these hours." />
      {!windows.length && <p className="hint">You have no working hours yet, so you won't appear in any matches. Add some below.</p>}
      <div className="presets">
        <button onClick={() => preset(5, '09:00', '17:00')}>Weekdays 9–5</button>
        <button onClick={() => preset(7, '08:00', '20:00')}>Every day 8–8</button>
        <button onClick={() => setWindows([])}>Clear all</button>
      </div>
      <div className="week-edit">
        {DAYS.map((d, i) => (
          <div className="day-row" key={d}>
            <b>{d}</b>
            <div className="day-windows">
              {windows.map((w, idx) => w.dayOfWeek === i + 1 && (
                <span className="window" key={idx}>
                  <input type="time" value={w.startTime} onChange={e => change(idx, 'startTime', e.target.value)} />–
                  <input type="time" value={w.endTime} onChange={e => change(idx, 'endTime', e.target.value)} />
                  <button className="x" onClick={() => remove(idx)} aria-label="Remove">×</button>
                </span>
              ))}
              {!windows.some(w => w.dayOfWeek === i + 1) && <small>Off</small>}
              <button className="tiny" onClick={() => add(i + 1)}>+ Add hours</button>
            </div>
          </div>
        ))}
      </div>
      {invalid && <div className="error">Each window must end after it starts.</div>}
      <button className="primary" disabled={busy || invalid} onClick={save}>Save availability <span>→</span></button>
    </>
  );
}
