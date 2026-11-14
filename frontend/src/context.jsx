import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { api, clearSession, errorMessage, loadSession, saveSession } from './api';

const AuthCtx = createContext(null);
const NoticeCtx = createContext(() => {});
export const useAuth = () => useContext(AuthCtx);
export const useNotice = () => useContext(NoticeCtx);

export function Providers({ children }) {
  const [session, setSession] = useState(loadSession);
  const [notice, setNotice] = useState('');
  const timer = useRef();

  const notify = useCallback(msg => {
    setNotice(msg);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setNotice(''), 4500);
  }, []);

  const login = s => { saveSession(s); setSession(s); };
  const logout = () => { clearSession(); setSession(null); };
  const updateUser = user => { const next = { ...session, user }; saveSession(next); setSession(next); };

  // Refresh the stored user once per page load so profile/verification changes show up.
  useEffect(() => {
    if (!session?.token) return;
    api.get('/auth/me').then(u => { const next = { ...loadSession(), user: u }; saveSession(next); setSession(next); }).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <AuthCtx.Provider value={{ session, user: session?.user, login, logout, updateUser }}>
      <NoticeCtx.Provider value={notify}>
        {children}
        {notice && <button className="notice" onClick={() => setNotice('')}>{notice}　×</button>}
      </NoticeCtx.Provider>
    </AuthCtx.Provider>
  );
}

/** Loads data on mount and whenever `deps` change. */
export function useLoad(fn, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: '' });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const reload = useCallback(() => fn()
    .then(data => setState({ data, loading: false, error: '' }))
    .catch(e => setState({ data: null, loading: false, error: errorMessage(e) })), deps);
  useEffect(() => { reload(); }, [reload]);
  return { ...state, reload };
}

/** Wraps a button action: busy flag + success/error notices. Returns undefined when the action failed. */
export function useAction() {
  const notify = useNotice();
  const [busy, setBusy] = useState(false);
  const run = async (fn, ok = 'Saved') => {
    setBusy(true);
    try {
      const result = await fn();
      if (ok) notify(ok);
      return result === undefined ? true : result;
    } catch (e) {
      notify(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  return { busy, run };
}
