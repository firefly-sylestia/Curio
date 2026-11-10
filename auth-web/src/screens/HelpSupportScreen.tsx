import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { MaterialIcon } from '../components/SharedComponents';

const HelpSupportScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const dark = isDark || isAmoled;
  const ink = getTextColor(isDark);
  const muted = dark ? 'rgba(255,255,255,.58)' : 'rgba(23,27,38,.58)';
  const surface = dark ? '#161A1D' : '#FFFFFF';
  const border = dark ? 'rgba(255,255,255,.10)' : 'rgba(23,27,38,.09)';
  const [supportEmail, setSupportEmail] = useState('');
  const [open, setOpen] = useState<number | null>(0);

  useEffect(() => {
    fetch('/api/config', { cache: 'no-store' }).then(response => response.ok ? response.json() : null)
      .then(data => { if (data?.supportEmail) setSupportEmail(data.supportEmail); }).catch(() => {});
  }, []);

  const faqs = [
    { q: 'Are my journals and saved discoveries uploaded?', a: 'No. Your discoveries, journal entries and imported photos stay in this browser on this device. Signing in to an online account does not back them up or sync them.' },
    { q: 'What does my online account do?', a: 'It gives you an online identity for Curio account features. It is separate from your local discovery library.' },
    { q: 'I cannot sign in. What should I try?', a: 'Check the email address and password, then use Forgot password from Account to request a reset email. If you have not confirmed your email yet, open the confirmation link first.' },
    { q: 'I did not receive an email.', a: 'Check your spam or promotions folder and make sure the address is correct. Email links expire and can only be used once, so request a fresh link if needed.' },
    { q: 'What happens if I delete my online account?', a: 'Your online profile and server-side social data are deleted. Your local journals, photos and saved discoveries on this device are not deleted.' },
  ];

  const card: React.CSSProperties = { background: surface, border: '1px solid ' + border, borderRadius: 18 };
  return (
    <main className="min-h-screen px-4 pt-7 pb-32" style={{ background: getBackgroundColor(isDark, isAmoled), color: ink }}>
      <div className="max-w-2xl mx-auto">
        <header className="flex items-center gap-3 mb-7">
          <button onClick={() => navigate(-1)} aria-label="Go back" className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: surface, border: '1px solid ' + border }}><MaterialIcon name="arrow_back" size={19} /></button>
          <div>
            <p className="text-[10px] uppercase tracking-[.16em] font-bold" style={{ color: '#DA1E37' }}>Curio care</p>
            <h1 className="text-2xl font-extrabold tracking-tight" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>Help & support</h1>
          </div>
        </header>
        <section className="relative overflow-hidden rounded-[20px] p-5 mb-5"
          style={{ background: dark ? 'linear-gradient(135deg,#26151B,#171A1D 72%)' : 'linear-gradient(135deg,#FBE9ED,#FFFFFF 78%)', border: '1px solid ' + border }}>
          <div className="absolute -right-8 -top-10 opacity-[.12] pointer-events-none" style={{ color: '#DA1E37' }} aria-hidden="true">
            <MaterialIcon name="auto_awesome" size={150} />
          </div>
          <div className="relative z-10">
            <span className="w-10 h-10 rounded-xl flex items-center justify-center mb-4"
              style={{ background: 'rgba(218,30,55,.12)', color: '#DA1E37' }}><MaterialIcon name="support_agent" size={22} /></span>
            <h2 className="text-xl font-bold">A little help goes a long way.</h2>
            <p className="text-sm leading-relaxed mt-2 max-w-md" style={{ color: muted }}>
              Find answers about your Curio account, privacy and sign-in. Your local discoveries remain yours.
            </p>
            {supportEmail ? (
              <a href={'mailto:' + supportEmail + '?subject=Curio%20support'} className="inline-flex items-center gap-2 mt-4 rounded-xl px-4 py-2.5 text-sm font-bold text-white"
                style={{ background: '#DA1E37' }}><MaterialIcon name="mail" size={16} /> Contact support</a>
            ) : (
              <a href="https://github.com/firefly-sylestia/Curio/issues" target="_blank" rel="noreferrer"
                className="inline-flex items-center gap-2 mt-4 rounded-xl px-4 py-2.5 text-sm font-bold text-white"
                style={{ background: '#DA1E37' }}><MaterialIcon name="github" size={16} /> Report an issue</a>
            )}
          </div>
        </section>
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-bold text-lg">Common questions</h2>
          <span className="text-xs" style={{ color: muted }}>Quick answers</span>
        </div>
        <section className="overflow-hidden mb-5" style={card}>
          {faqs.map((faq, index) => (
            <div key={faq.q} style={{ borderBottom: index === faqs.length - 1 ? 'none' : '1px solid ' + border }}>
              <button onClick={() => setOpen(open === index ? null : index)} className="w-full flex items-center gap-3 p-4 text-left">
                <span className="flex-1 text-sm font-semibold">{faq.q}</span>
                <MaterialIcon name={open === index ? 'expand_less' : 'expand_more'} size={19} />
              </button>
              {open === index && <p className="px-4 pb-4 text-sm leading-relaxed" style={{ color: muted }}>{faq.a}</p>}
            </div>
          ))}
        </section>
        <section className="rounded-[18px] p-4" style={card}>
          <h2 className="font-bold text-sm mb-3">Useful places</h2>
          {[
            { label: 'Your account', detail: 'Sign in or manage your account', icon: 'person', action: () => navigate('/profile') },
            { label: 'Preferences', detail: 'Appearance and experience', icon: 'tune', action: () => navigate('/settings') },
            { label: 'Privacy notice', detail: 'Understand how your data is handled', icon: 'shield', action: () => window.open('/privacy', '_self') },
            { label: 'Terms', detail: 'The rules for using Curio', icon: 'description', action: () => window.open('/terms', '_self') },
          ].map(item => (
            <button key={item.label} onClick={item.action} className="w-full flex items-center gap-3 py-3 text-left">
              <span className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: dark ? 'rgba(255,255,255,.06)' : '#F5F5F7', color: '#DA1E37' }}>
                <MaterialIcon name={item.icon} size={18} />
              </span>
              <span className="flex-1"><strong className="block text-sm">{item.label}</strong><small className="text-xs" style={{ color: muted }}>{item.detail}</small></span>
              <MaterialIcon name="chevron_right" size={18} />
            </button>
          ))}
        </section>
      </div>
    </main>
  );
};
export default HelpSupportScreen;
