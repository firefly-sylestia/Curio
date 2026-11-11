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

  const fieldStyle: React.CSSProperties = { width: '100%', borderRadius: 14, padding: '14px 15px', fontSize: 14, color: ink, background: dark ? 'rgba(255,255,255,.055)' : 'rgba(20,27,42,.045)', border: 0, outline: 'none' };
  const cardStyle: React.CSSProperties = { background: surface, border: 0, borderRadius: 24 };

  return (
    <main className="curio-account-screen min-h-screen px-4 pt-7 pb-32 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled), color: ink }}>
      <div className="curio-account-content mx-auto">
        <header className="curio-account-heading">
          <div>
            <p className="curio-account-eyebrow">CURIO / YOUR SPACE</p>
            <h1>Your account<span>.</span></h1>
            <p className="curio-account-subtitle">A little record of what keeps you curious.</p>
          </div>
          <button onClick={() => navigate('/settings')} className="curio-account-settings" aria-label="Settings"><MaterialIcon name="settings" size={20} /></button>
        </header>

        <section className="curio-account-profile" style={cardStyle}>
          <div className="curio-account-profile-glow" />
          <div className="curio-account-profile-top">
            <div className="curio-account-avatar">{displayName.trim().charAt(0).toUpperCase() || 'C'}</div>
            <div className="curio-account-profile-copy">
              <span className="curio-account-label">YOUR LOCAL PROFILE</span>
              <h2>{displayName.trim() || 'Explorer'}</h2>
              <p><MaterialIcon name="lock" size={13} /> Private to this device</p>
            </div>
            <span className="curio-account-local-mark"><i /> LOCAL</span>
          </div>
          <label htmlFor="curio-display-name" className="curio-account-field-label">What should we call you?</label>
          <input id="curio-display-name" className="curio-account-input" value={displayName} onChange={event => {
            setDisplayName(event.target.value);
            localStorage.setItem('curio-display-name', event.target.value);
          }} maxLength={40} placeholder="Your name" />
        </section>

        <section className="curio-account-progress">
          <div className="curio-account-section-heading"><div><span className="curio-account-label">YOUR JOURNEY</span><h2>A curious life, counted.</h2></div><MaterialIcon name="auto_awesome" size={19} /></div>
          <div className="curio-account-stats">
            {[{ label: 'Discoveries saved', value: stats.saved, icon: 'bookmark' }, { label: 'Curiosity draws', value: stats.spins, icon: 'shuffle' }, { label: 'Best streak', value: stats.streak, icon: 'local_fire_department' }].map((item, index) => (
              <div key={item.label} className="curio-account-stat" style={{ '--stat-index': index } as React.CSSProperties}>
                <span className="curio-account-stat-icon"><MaterialIcon name={item.icon} size={18} /></span>
                <strong>{item.value.toLocaleString()}</strong>
                <small>{item.label}</small>
              </div>
            ))}
          </div>
        </section>

        <section className="curio-account-online" style={cardStyle}>
          <div className="curio-account-online-heading">
            <div className="curio-account-online-symbol"><MaterialIcon name="cloud" size={21} /></div>
            <div className="curio-account-online-copy"><span className="curio-account-label">OPTIONAL CONNECTION</span><h2>Curio online account</h2><p>Your identity beyond this device.</p></div>
            <span className={"curio-account-status " + (session ? 'is-connected' : '')}>{authLoading ? 'Checking' : session ? 'Connected' : 'Offline'}</span>
          </div>

          {authLoading ? (
            <div className="curio-account-loading"><span className="curio-account-spinner" />Checking your account…</div>
          ) : session ? (
            <div className="curio-account-connected">
              <div className="curio-account-identity">
                <strong>{session.email}</strong>
                {(onlineName || username) && <span>{onlineName || '@' + username}</span>}
                {memberSince && <small>Member since {memberSince}</small>}
              </div>
              <p className="curio-account-privacy-note"><MaterialIcon name="shield_lock" size={16} /> Your journals, saved topics and photos remain on this device. Signing in does not upload them.</p>
              <button onClick={signOut} disabled={busy} className="curio-account-secondary-action">{busy ? 'Please wait…' : 'Sign out'} <MaterialIcon name="logout" size={17} /></button>
              {config?.deletionEnabled && <button onClick={deleteAccount} disabled={busy} className="curio-account-danger-action">Delete online account</button>}
            </div>
          ) : (
            <div className="curio-account-auth">
              {!config?.configured ? (
                <p className="curio-account-unavailable">{config?.problem || 'The online account service is not configured on this deployment.'}</p>
              ) : (
                <>
                  <div className="curio-account-auth-switch">
                    <button onClick={() => { setMode('signin'); setNotice(''); }} className={mode === 'signin' ? 'active' : ''}>Sign in</button>
                    <button onClick={() => { setMode('signup'); setNotice(''); }} className={mode === 'signup' ? 'active' : ''}>Create account</button>
                  </div>
                  <form onSubmit={handleAuth} className="curio-account-auth-form">
                    <label>Email address<input type="email" autoComplete="email" required value={email} onChange={event => setEmail(event.target.value)} placeholder="you@example.com" style={fieldStyle} /></label>
                    <label>Password<input type="password" autoComplete={mode === 'signup' ? 'new-password' : 'current-password'} required minLength={6} value={password} onChange={event => setPassword(event.target.value)} placeholder="At least 6 characters" style={fieldStyle} /></label>
                    {mode === 'signup' && <>
                      <label>Repeat password<input type="password" autoComplete="new-password" required minLength={6} value={repeatPassword} onChange={event => setRepeatPassword(event.target.value)} placeholder="Enter it again" style={fieldStyle} /></label>
                      <label className="curio-account-terms"><input type="checkbox" checked={termsAccepted} onChange={event => setTermsAccepted(event.target.checked)} /><span>I agree to the <a href="/terms" target="_blank" rel="noreferrer">Terms</a> and <a href="/privacy" target="_blank" rel="noreferrer">Privacy Notice</a>.</span></label>
                    </>}
                    <button type="submit" disabled={busy} className="curio-account-primary-action">{busy ? 'Please wait…' : mode === 'signup' ? 'Create my account' : 'Sign in'} <MaterialIcon name="arrow_forward" size={18} /></button>
                  </form>
                  {mode === 'signin' && <div className="curio-account-auth-links"><button onClick={sendMagicLink} disabled={busy}>Email me a sign-in link</button><button onClick={sendRecovery} disabled={busy}>Forgot password?</button></div>}
                </>
              )}
            </div>
          )}
          {status && <p role="status" aria-live="polite" className={"curio-account-notice is-" + statusKind}>{status}</p>}
        </section>

        <section className="curio-account-preferences">
          <div className="curio-account-section-heading"><div><span className="curio-account-label">THE DETAILS</span><h2>Make Curio yours.</h2></div></div>
          <div className="curio-account-menu">
            <button onClick={() => navigate('/settings')}><span className="curio-account-menu-icon"><MaterialIcon name="tune" size={19} /></span><span><strong>Preferences</strong><small>Appearance and experience</small></span><MaterialIcon name="chevron_right" size={19} /></button>
            <a href="/privacy"><span className="curio-account-menu-icon"><MaterialIcon name="shield" size={19} /></span><span><strong>Privacy</strong><small>How your account data is handled</small></span><MaterialIcon name="chevron_right" size={19} /></a>
            <button onClick={() => navigate('/support')}><span className="curio-account-menu-icon"><MaterialIcon name="help_outline" size={19} /></span><span><strong>Help & support</strong><small>Answers when you need them</small></span><MaterialIcon name="chevron_right" size={19} /></button>
          </div>
        </section>
        <p className="curio-account-footer">CURIO · MADE FOR THE CURIOUS</p>
      </div>
    </main>
  );
};

export default AccountScreen;
