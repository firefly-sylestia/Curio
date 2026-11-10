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

  const card: React.CSSProperties = { background: surface, border: '1px solid ' + border, borderRadius: 20 };
  return (
    <main className="min-h-screen px-4 pt-24 pb-12" style={{ background: getBackgroundColor(isDark, isAmoled), color: ink }}>
      <button onClick={() => navigate(-1)} aria-label="Go back"
        className="fixed left-4 top-[max(14px,env(safe-area-inset-top))] z-50 w-11 h-11 rounded-full flex items-center justify-center shadow-lg"
        style={{ background: surface, border: '1px solid ' + border, color: ink }}>
        <MaterialIcon name="arrow_back" size={20} />
      </button>
      <div className="max-w-2xl mx-auto">
        <header className="mb-7">
          <p className="text-[10px] uppercase tracking-[.2em] font-bold mb-2" style={{ color: '#DA1E37' }}>Curio / Support desk</p>
          <h1 className="text-[34px] leading-tight font-extrabold tracking-tight" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>Help & support<span style={{ color: '#DA1E37' }}>.</span></h1>
          <p className="mt-2 text-sm leading-relaxed max-w-md" style={{ color: muted }}>A clear answer, a little guidance, or a way to reach us. Your local discoveries remain yours.</p>
        </header>

        <section className="rounded-[22px] p-5 mb-7" style={{ ...card, background: dark ? '#161A1D' : '#FFFFFF' }}>
          <div className="flex items-center gap-3 mb-3">
            <span className="w-11 h-11 rounded-2xl flex items-center justify-center"
              style={{ background: 'rgba(218,30,55,.11)', color: '#DA1E37' }}>
              <MaterialIcon name="support_agent" size={23} />
            </span>
            <div className="min-w-0">
              <p className="font-bold text-base">Need a hand?</p>
              <p className="text-xs mt-0.5" style={{ color: muted }}>We’ll help you find the right direction.</p>
            </div>
          </div>
          {supportEmail ? (
            <a href={'mailto:' + supportEmail + '?subject=Curio%20support'}
              className="inline-flex items-center gap-2 mt-2 rounded-xl px-4 py-3 text-sm font-bold text-white"
              style={{ background: '#DA1E37' }}><MaterialIcon name="mail" size={17} /> Contact support</a>
          ) : (
            <a href="https://github.com/firefly-sylestia/Curio/issues" target="_blank" rel="noreferrer"
              className="inline-flex items-center gap-2 mt-2 rounded-xl px-4 py-3 text-sm font-bold text-white"
              style={{ background: '#DA1E37' }}><MaterialIcon name="bug_report" size={17} /> Report an issue</a>
          )}
        </section>

        <div className="flex items-end justify-between mb-3">
          <div><p className="text-[10px] uppercase tracking-[.18em] font-bold" style={{ color: '#DA1E37' }}>Help centre</p>
            <h2 className="mt-1 text-xl font-bold">Common questions</h2></div>
          <span className="text-xs pb-1" style={{ color: muted }}>Quick answers</span>
        </div>
        <section className="overflow-hidden mb-7" style={card}>
          {faqs.map((faq, index) => (
            <div key={faq.q} style={{ borderBottom: index === faqs.length - 1 ? 'none' : '1px solid ' + border }}>
              <button onClick={() => setOpen(open === index ? null : index)} aria-expanded={open === index}
                className="w-full flex items-center gap-3 p-4 text-left">
                <span className="flex-1 text-sm font-semibold leading-snug">{faq.q}</span>
                <MaterialIcon name={open === index ? 'remove' : 'add'} size={19} />
              </button>
              {open === index && <p className="px-4 pb-5 pr-8 text-sm leading-[1.75]" style={{ color: muted }}>{faq.a}</p>}
            </div>
          ))}
        </section>

        <div className="mb-3"><p className="text-[10px] uppercase tracking-[.18em] font-bold" style={{ color: '#DA1E37' }}>More information</p>
          <h2 className="mt-1 text-xl font-bold">Your Curio essentials</h2></div>
        <section className="overflow-hidden" style={card}>
          {[
            { label: 'Your account', detail: 'Sign in or manage your account', icon: 'person', action: () => navigate('/profile') },
            { label: 'Preferences', detail: 'Appearance and experience', icon: 'tune', action: () => navigate('/settings') },
            { label: 'Privacy notice', detail: 'How Online Mode handles information', icon: 'shield', action: () => navigate('/privacy') },
            { label: 'Terms of use', detail: 'Rules for using Curio Online Mode', icon: 'description', action: () => navigate('/terms') },
          ].map((item, index) => (
            <button key={item.label} onClick={item.action} className="w-full flex items-center gap-3 p-4 text-left"
              style={{ borderBottom: index === 3 ? 'none' : '1px solid ' + border }}>
              <span className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ background: dark ? 'rgba(255,255,255,.06)' : '#F4F5F7', color: '#DA1E37' }}>
                <MaterialIcon name={item.icon} size={19} />
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
