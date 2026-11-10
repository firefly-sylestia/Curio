import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getQuestSystem } from '../data/QuestSystem';
import { MaterialIcon } from '../components/SharedComponents';

type WebSession = {
  access_token: string;
  refresh_token: string;
  expires_at: number;
  user_id: string;
  email: string;
};
type AuthConfig = {
  configured: boolean;
  problem?: string;
  supabaseUrl: string;
  supabaseAnonKey: string;
  siteUrl?: string;
  deletionEnabled?: boolean;
};
type AuthMode = 'signin' | 'signup';

const SESSION_KEY = 'curio.web.session';
const AccountScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const dark = isDark || isAmoled;
  const ink = getTextColor(isDark);
  const muted = dark ? 'rgba(255,255,255,.58)' : 'rgba(23,27,38,.58)';
  const surface = dark ? '#161A1D' : '#FFFFFF';
  const border = dark ? 'rgba(255,255,255,.10)' : 'rgba(23,27,38,.09)';
  const [displayName, setDisplayName] = useState(() => localStorage.getItem('curio-display-name') || 'Explorer');
  const [stats, setStats] = useState({ saved: 0, spins: 0, streak: 0 });
  const [config, setConfig] = useState<AuthConfig | null>(null);
  const [session, setSession] = useState<WebSession | null>(null);
  const [mode, setMode] = useState<AuthMode>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [repeatPassword, setRepeatPassword] = useState('');
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [status, setStatus] = useState('');
  const [statusKind, setStatusKind] = useState<'success' | 'error' | 'info'>('info');
  const [busy, setBusy] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  const [onlineName, setOnlineName] = useState('');
  const [username, setUsername] = useState('');
  const [memberSince, setMemberSince] = useState('');

  useEffect(() => {
    const state = getQuestSystem().getState();
    setStats({ saved: state.lifetime?.saves || 0, spins: state.lifetime?.spins || 0, streak: state.bestStreak || 0 });
  }, []);

  const saveSession = useCallback((data: any) => {
    if (!data?.access_token) return null;
    const next: WebSession = {
      access_token: data.access_token,
      refresh_token: data.refresh_token || '',
      expires_at: Date.now() + Math.max(0, Number(data.expires_in || 3600) - 30) * 1000,
      user_id: data.user?.id || '',
      email: data.user?.email || '',
    };
    localStorage.setItem(SESSION_KEY, JSON.stringify(next));
    setSession(next);
    return next;
  }, []);

  const authRequest = useCallback(async (path: string, options: {
    method?: string; body?: unknown; token?: string; query?: Record<string, string>;
  } = {}) => {
    if (!config?.configured) throw new Error(config?.problem || 'Online accounts are not configured on this deployment.');
    const query = options.query ? '?' + new URLSearchParams(options.query).toString() : '';
    const response = await fetch(config.supabaseUrl + path + query, {
      method: options.method || 'GET',
      headers: {
        apikey: config.supabaseAnonKey,
        Authorization: 'Bearer ' + (options.token || config.supabaseAnonKey),
        'Content-Type': 'application/json',
      },
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
    const raw = await response.text();
    let data: any = null;
    try { data = raw ? JSON.parse(raw) : null; } catch { data = { message: raw }; }
    if (!response.ok) {
      const message = data?.msg || data?.error_description || data?.error || data?.message || 'The request could not be completed.';
      throw new Error(message);
    }
    return data;
  }, [config]);

  const loadProfile = useCallback(async (current: WebSession) => {
    if (!config?.configured || !current.user_id) return;
    try {
      const query = new URLSearchParams({
        select: 'username,display_name,created_at',
        id: 'eq.' + current.user_id,
        limit: '1',
      });
      const response = await fetch(config.supabaseUrl + '/rest/v1/profiles?' + query.toString(), {
        headers: { apikey: config.supabaseAnonKey, Authorization: 'Bearer ' + current.access_token },
      });
      if (!response.ok) return;
      const rows = await response.json();
      const profile = Array.isArray(rows) ? rows[0] : null;
      if (profile) {
        setOnlineName(profile.display_name || '');
        setUsername(profile.username || '');
        setMemberSince(profile.created_at ? new Date(profile.created_at).toLocaleDateString(undefined, { month: 'long', year: 'numeric' }) : '');
      }
    } catch { /* profile details are optional; auth remains usable */ }
  }, [config]);

  useEffect(() => {
    let active = true;
    Promise.all([
      fetch('/api/config', { cache: 'no-store' }).then(async response => {
        if (!response.ok) throw new Error('Configuration unavailable');
        return response.json() as Promise<AuthConfig>;
      }).catch(() => ({ configured: false, problem: 'Could not reach the account service.', supabaseUrl: '', supabaseAnonKey: '' })),
    ]).then(async ([nextConfig]) => {
      if (!active) return;
      setConfig(nextConfig);
      let stored: WebSession | null = null;
      try {
        const raw = JSON.parse(localStorage.getItem(SESSION_KEY) || 'null') as WebSession | null;
        if (raw?.access_token) stored = raw;
      } catch { /* ignore malformed local session */ }
      if (stored) {
        if (stored.expires_at <= Date.now() && stored.refresh_token && nextConfig.configured) {
          try {
            const response = await fetch(nextConfig.supabaseUrl + '/auth/v1/token?grant_type=refresh_token', {
              method: 'POST',
              headers: { apikey: nextConfig.supabaseAnonKey, Authorization: 'Bearer ' + nextConfig.supabaseAnonKey, 'Content-Type': 'application/json' },
              body: JSON.stringify({ refresh_token: stored.refresh_token }),
            });
            const data = await response.json();
            if (response.ok && data.access_token) {
              stored = {
                access_token: data.access_token,
                refresh_token: data.refresh_token || '',
                expires_at: Date.now() + Math.max(0, Number(data.expires_in || 3600) - 30) * 1000,
                user_id: data.user?.id || '',
                email: data.user?.email || '',
              };
              localStorage.setItem(SESSION_KEY, JSON.stringify(stored));
            } else stored = null;
          } catch { stored = null; }
        } else if (stored.expires_at <= Date.now()) stored = null;
      }
      if (!active) return;
      if (!stored) localStorage.removeItem(SESSION_KEY);
      setSession(stored);
      setAuthLoading(false);
    });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (session && config?.configured) void loadProfile(session);
  }, [session, config, loadProfile]);

  const setNotice = (message: string, kind: 'success' | 'error' | 'info' = 'info') => {
    setStatus(message);
    setStatusKind(kind);
  };

  const handleAuth = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!config?.configured) {
      setNotice(config?.problem || 'Online accounts are not configured yet.', 'error');
      return;
    }
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      setNotice('Enter a valid email address.', 'error');
      return;
    }
    if (mode === 'signup' && (password.length < 6 || password !== repeatPassword || !termsAccepted)) {
      setNotice(password.length < 6 ? 'Use a password of at least 6 characters.' : password !== repeatPassword ? 'The passwords do not match.' : 'Please agree to the Terms and Privacy Notice.', 'error');
      return;
    }
    setBusy(true);
    setNotice('');
    try {
      if (mode === 'signup') {
        const data = await authRequest('/auth/v1/signup', {
          method: 'POST', query: { redirect_to: (config.siteUrl || window.location.origin) + '/confirm' },
          body: { email: email.trim(), password },
        });
        if (data?.access_token) {
          saveSession(data);
          setNotice('Your account is ready. You are signed in.', 'success');
        } else {
          setNotice('Account created. Check your inbox to confirm your email, then sign in here.', 'success');
          setMode('signin');
        }
      } else {
        const data = await authRequest('/auth/v1/token', {
          method: 'POST', query: { grant_type: 'password' }, body: { email: email.trim(), password },
        });
        saveSession(data);
        setNotice('You are signed in to your Curio online account.', 'success');
      }
      setPassword('');
      setRepeatPassword('');
    } catch (error) {
      const message = error instanceof Error ? error.message.toLowerCase() : '';
      setNotice(message.includes('invalid login') || message.includes('invalid_credentials') ? 'That email and password do not match.' :
        message.includes('already registered') || message.includes('user_already_exists') ? 'That email already has an account. Sign in instead.' :
        message.includes('not confirmed') ? 'Confirm your email using the link we sent you first.' :
        message.includes('weak_password') ? 'Choose a stronger password.' :
        error instanceof Error ? error.message : 'Something went wrong. Please try again.', 'error');
    } finally { setBusy(false); }
  };

  const sendMagicLink = async () => {
    if (!config?.configured || !/^\S+@\S+\.\S+$/.test(email.trim())) {
      setNotice(config?.problem || 'Enter a valid email address first.', 'error');
      return;
    }
    setBusy(true);
    setNotice('');
    try {
      await authRequest('/auth/v1/otp', {
        method: 'POST', query: { redirect_to: (config.siteUrl || window.location.origin) + '/link' },
        body: { email: email.trim(), create_user: false },
      });
      setNotice('Sign-in link sent. Open it in this browser to finish signing in.', 'success');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Could not send the sign-in link.', 'error');
    } finally { setBusy(false); }
  };

  const sendRecovery = async () => {
    if (!config?.configured || !/^\S+@\S+\.\S+$/.test(email.trim())) {
      setNotice(config?.problem || 'Enter your email address first.', 'error');
      return;
    }
    setBusy(true);
    setNotice('');
    try {
      await authRequest('/auth/v1/recover', {
        method: 'POST', query: { redirect_to: (config.siteUrl || window.location.origin) + '/reset' },
        body: { email: email.trim() },
      });
      setNotice('Password reset email sent. Follow the link to choose a new password.', 'success');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Could not send a reset email.', 'error');
    } finally { setBusy(false); }
  };

  const signOut = async () => {
    if (!session) return;
    setBusy(true);
    try {
      await authRequest('/auth/v1/logout', { method: 'POST', token: session.access_token, body: {} });
    } catch { /* local session must still end if server revoke fails */ }
    localStorage.removeItem(SESSION_KEY);
    setSession(null);
    setOnlineName('');
    setUsername('');
    setMemberSince('');
    setNotice('Signed out of this browser.', 'success');
    setBusy(false);
  };

  const deleteAccount = async () => {
    if (!session || !config?.deletionEnabled || !window.confirm('Permanently delete your Curio online account? This cannot be undone. Your local discoveries will remain on this device.')) return;
    setBusy(true);
    setNotice('');
    try {
      const response = await fetch('/api/delete-account', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + session.access_token },
        body: JSON.stringify({ confirm: 'DELETE' }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || 'Account deletion failed.');
      localStorage.removeItem(SESSION_KEY);
      setSession(null);
      setNotice('Your online account has been deleted. Local discoveries are untouched.', 'success');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Could not delete the account.', 'error');
    } finally { setBusy(false); }
  };

  const fieldStyle: React.CSSProperties = {
    width: '100%', borderRadius: 12, padding: '13px 14px', fontSize: 14,
    color: ink, background: dark ? 'rgba(255,255,255,.055)' : '#F7F7F9',
    border: '1px solid ' + border, outline: 'none',
  };
  const cardStyle: React.CSSProperties = {
    background: surface, border: '1px solid ' + border, borderRadius: 18,
  };

  return (
    <main className="curio-account-screen min-h-screen px-4 pt-7 pb-32 relative"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled), color: ink }}>
      <div className="max-w-2xl mx-auto">
        <header className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-3xl font-extrabold" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>Account</h1>
            <p className="text-sm mt-1" style={{ color: muted }}>Your profile and online account.</p>
          </div>
          <button onClick={() => navigate('/settings')} className="w-11 h-11 rounded-full flex items-center justify-center"
            style={{ background: surface, border: '1px solid ' + border }} aria-label="Settings"><MaterialIcon name="settings" size={20} /></button>
        </header>

        <section className="rounded-[18px] p-5 mb-4" style={{ ...cardStyle, background: dark ? '#191719' : '#FFF9FA' }}>
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-full flex items-center justify-center text-xl font-bold"
              style={{ color: '#fff', background: 'linear-gradient(145deg,#DA1E37,#8F172B)' }}>
              {displayName.trim().charAt(0).toUpperCase() || 'C'}
            </div>
            <div className="min-w-0 flex-1">
              <p className="font-bold text-lg truncate">{displayName || 'Explorer'}</p>
              <p className="text-xs" style={{ color: muted }}>Local profile · This device</p>
            </div>
          </div>
          <label htmlFor="curio-display-name" className="block text-xs font-semibold mt-5 mb-2" style={{ color: muted }}>Display name</label>
          <input id="curio-display-name" value={displayName} onChange={event => {
            setDisplayName(event.target.value);
            localStorage.setItem('curio-display-name', event.target.value);
          }} style={fieldStyle} maxLength={40} />
        </section>

        <section className="grid grid-cols-3 gap-2.5 mb-5">
          {[{ label: 'Saved', value: stats.saved }, { label: 'Shuffles', value: stats.spins }, { label: 'Best streak', value: stats.streak }].map(item => (
            <div key={item.label} className="rounded-2xl p-3 text-center" style={cardStyle}>
              <p className="text-xl font-extrabold">{item.value}</p>
              <p className="text-[11px] mt-1" style={{ color: muted }}>{item.label}</p>
            </div>
          ))}
        </section>

        <section className="rounded-[18px] p-5 mb-4" style={cardStyle}>
          <div className="flex items-center justify-between gap-3 mb-3">
            <div>
              <h2 className="font-bold text-lg">Online account</h2>
              <p className="text-xs mt-1" style={{ color: muted }}>Optional account access across Curio.</p>
            </div>
            <span className="text-[10px] font-bold px-2.5 py-1 rounded-full"
              style={{ color: session ? '#168653' : muted, background: session ? 'rgba(22,134,83,.10)' : (dark ? 'rgba(255,255,255,.07)' : 'rgba(23,27,38,.05)') }}>
              {authLoading ? 'Checking…' : session ? 'Signed in' : 'Signed out'}
            </span>
          </div>

          {authLoading ? (
            <p className="text-sm py-3" style={{ color: muted }}>Checking your online account…</p>
          ) : session ? (
            <div>
              <div className="rounded-xl p-4 mb-4" style={{ background: dark ? 'rgba(255,255,255,.045)' : '#F7F7F9' }}>
                <p className="text-sm font-semibold break-all">{session.email}</p>
                {(onlineName || username) && <p className="text-xs mt-1" style={{ color: muted }}>{onlineName || '@' + username}</p>}
                {memberSince && <p className="text-xs mt-1" style={{ color: muted }}>Member since {memberSince}</p>}
              </div>
              <p className="text-xs leading-relaxed mb-4" style={{ color: muted }}>
                Your online identity is connected. Saved topics, journals and photos remain local and are not uploaded by signing in.
              </p>
              <div className="flex flex-col gap-2">
                <button onClick={signOut} disabled={busy} className="w-full rounded-xl py-3 text-sm font-semibold disabled:opacity-50"
                  style={{ color: ink, background: dark ? 'rgba(255,255,255,.07)' : '#F1F1F3' }}>
                  {busy ? 'Please wait…' : 'Sign out'}
                </button>
                {config?.deletionEnabled && <button onClick={deleteAccount} disabled={busy}
                  className="w-full rounded-xl py-3 text-sm font-semibold disabled:opacity-50"
                  style={{ color: '#C53045', background: 'rgba(218,30,55,.07)' }}>Delete online account</button>}
              </div>
            </div>
          ) : (
            <div>
              {!config?.configured ? (
                <p className="text-sm leading-relaxed py-2" style={{ color: muted }}>
                  {config?.problem || 'The online account service is not configured on this deployment.'}
                </p>
              ) : (
                <>
                  <div className="flex rounded-xl p-1 mb-4" style={{ background: dark ? 'rgba(255,255,255,.06)' : '#F1F1F3' }}>
                    <button onClick={() => { setMode('signin'); setNotice(''); }} className="flex-1 rounded-lg py-2.5 text-sm font-semibold"
                      style={{ background: mode === 'signin' ? surface : 'transparent', color: ink, boxShadow: mode === 'signin' ? '0 1px 4px rgba(0,0,0,.08)' : 'none' }}>Sign in</button>
                    <button onClick={() => { setMode('signup'); setNotice(''); }} className="flex-1 rounded-lg py-2.5 text-sm font-semibold"
                      style={{ background: mode === 'signup' ? surface : 'transparent', color: ink, boxShadow: mode === 'signup' ? '0 1px 4px rgba(0,0,0,.08)' : 'none' }}>Create account</button>
                  </div>
                  <form onSubmit={handleAuth} className="flex flex-col gap-3">
                    <label className="text-xs font-semibold" style={{ color: muted }}>Email address
                      <input type="email" autoComplete="email" required value={email} onChange={event => setEmail(event.target.value)}
                        placeholder="you@example.com" style={{ ...fieldStyle, marginTop: 7 }} />
                    </label>
                    <label className="text-xs font-semibold" style={{ color: muted }}>Password
                      <input type="password" autoComplete={mode === 'signup' ? 'new-password' : 'current-password'} required minLength={6}
                        value={password} onChange={event => setPassword(event.target.value)} placeholder="At least 6 characters"
                        style={{ ...fieldStyle, marginTop: 7 }} />
                    </label>
                    {mode === 'signup' && (
                      <>
                        <label className="text-xs font-semibold" style={{ color: muted }}>Repeat password
                          <input type="password" autoComplete="new-password" required minLength={6} value={repeatPassword}
                            onChange={event => setRepeatPassword(event.target.value)} placeholder="Enter it again"
                            style={{ ...fieldStyle, marginTop: 7 }} />
                        </label>
                        <label className="flex items-start gap-2 text-xs leading-relaxed" style={{ color: muted }}>
                          <input type="checkbox" checked={termsAccepted} onChange={event => setTermsAccepted(event.target.checked)} className="mt-0.5" />
                          <span>I agree to the <a href="/terms" target="_blank" rel="noreferrer" style={{ color: '#DA1E37' }}>Terms</a> and <a href="/privacy" target="_blank" rel="noreferrer" style={{ color: '#DA1E37' }}>Privacy Notice</a>.</span>
                        </label>
                      </>
                    )}
                    <button type="submit" disabled={busy} className="w-full rounded-xl py-3.5 text-sm font-bold text-white disabled:opacity-50"
                      style={{ background: 'linear-gradient(135deg,#DA1E37,#A9132B)' }}>
                      {busy ? 'Please wait…' : mode === 'signup' ? 'Create my account' : 'Sign in'}
                    </button>
                  </form>
                  {mode === 'signin' && (
                    <div className="flex flex-wrap gap-3 mt-3">
                      <button onClick={sendMagicLink} disabled={busy} className="text-xs font-semibold" style={{ color: '#DA1E37' }}>Email me a sign-in link</button>
                      <button onClick={sendRecovery} disabled={busy} className="text-xs font-semibold" style={{ color: muted }}>Forgot password?</button>
                    </div>
                  )}
                </>
              )}
            </div>
          )}

          {status && <p role="status" aria-live="polite" className="mt-4 rounded-xl px-3.5 py-3 text-xs leading-relaxed"
            style={{ color: statusKind === 'error' ? '#C53045' : statusKind === 'success' ? '#168653' : muted,
              background: statusKind === 'error' ? 'rgba(197,48,69,.08)' : statusKind === 'success' ? 'rgba(22,134,83,.08)' : (dark ? 'rgba(255,255,255,.05)' : '#F7F7F9') }}>
            {status}
          </p>}
        </section>

        <section className="rounded-[18px] overflow-hidden mb-4" style={cardStyle}>
          <button onClick={() => navigate('/settings')} className="w-full flex items-center gap-3 p-4 text-left">
            <span className="flex-1"><strong className="block text-sm">Preferences</strong><small className="text-xs" style={{ color: muted }}>Appearance and experience</small></span>
            <MaterialIcon name="chevron_right" size={18} />
          </button>
          <div style={{ height: 1, background: border }} />
          <a href="/privacy" className="flex items-center gap-3 p-4">
            <span className="flex-1"><strong className="block text-sm">Privacy</strong><small className="text-xs" style={{ color: muted }}>How online account data is handled</small></span>
            <MaterialIcon name="chevron_right" size={18} />
          </a>
          <div style={{ height: 1, background: border }} />
          <button onClick={() => navigate('/support')} className="w-full flex items-center gap-3 p-4 text-left">
            <span className="flex-1"><strong className="block text-sm">Help & support</strong><small className="text-xs" style={{ color: muted }}>Account and sign-in help</small></span>
            <MaterialIcon name="chevron_right" size={18} />
          </button>
        </section>
      </div>
    </main>
  );
};

export default AccountScreen;
