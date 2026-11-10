import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { MaterialIcon } from '../components/SharedComponents';

type LegalType = 'privacy' | 'terms';
type Section = { title: string; paragraphs: string[] };

const PRIVACY: Section[] = [
  { title: 'What Curio processes', paragraphs: [
    'Your offline discovery library stays on your device. When Online Mode is enabled, Curio processes account identifiers, username and display name, selected avatar style, bio, friend relationships, public cards, replies, reactions, reports, service timestamps and technical security records needed to run the service.',
    'Direct-message encryption is not currently supported. Direct messages are uploaded as text over the protected Online Mode connection and are not end-to-end encrypted.',
  ]},
  { title: 'Why and how long', paragraphs: [
    'Curio uses this data to create and secure accounts, deliver social features, prevent abuse, respond to reports, comply with law and maintain the service.',
    'Public wall posts, replies and server-side direct messages are designed to expire after 24 hours. A participant’s device may retain messages it received until that person deletes local data or signs out. Security, report and account records may need a separate documented retention period where law permits or requires it.',
  ]},
  { title: 'Sharing and controls', paragraphs: [
    'Curio shares data only with service providers needed to host and authenticate the service, other members according to the feature and privacy settings selected, and authorities when law requires it. Curio does not sell personal information or use Online Mode content for advertising.',
    'Members can disable Online Mode, edit public profile fields, block or report people, delete local app data, and request access, correction, deletion, portability, restriction or objection where applicable through the privacy contact published with the release.',
  ]},
  { title: 'International transfers and children', paragraphs: [
    'Online data may be processed where Curio or its providers operate. The operator must put in place transfer safeguards required for a member’s location before launch.',
    'Curio Online Mode is not intended for children under 13. Where a country sets a higher digital-consent age, the operator must meet that requirement before processing that child’s data.',
  ]},
  { title: 'Security and limits', paragraphs: [
    'Curio uses access controls, Android Keystore-backed local secrets and encryption features described in the Terms. No system is risk-free. Members should use device security and report suspected compromise promptly.',
  ]},
];

const TERMS: Section[] = [
  { title: '1. Accepting these terms', paragraphs: [
    'Online Mode is optional. By creating a Curio account, enabling Online Mode, or posting, replying, messaging, reacting, reporting or using another Online Mode feature, you agree to these terms and the Online Privacy Notice. If you do not agree, do not enable Online Mode.',
    'You must be old enough to consent to online services where you live. Curio Online Mode is not for children under 13 and must not be used where local law requires a higher minimum age without the consent or supervision that law requires. The operator may request age assurance where law requires it.',
  ]},
  { title: '2. Member content and conduct', paragraphs: [
    'You remain responsible for the text, names and profile information you submit. You must have the rights and permissions needed to share it.',
    'Do not post or send unlawful content, child sexual exploitation material, credible threats, terrorism or violent-extremist material, non-consensual intimate content, hateful or harassing content, fraud, impersonation or spam, malware, doxxing or another person’s private information, or material that infringes copyright, trademark or other rights.',
    'Public posts, replies, usernames, display names and bios are moderated by automated rules and may be reviewed following a report. Members can block and report others. Curio may remove content, limit features, suspend or terminate accounts when reasonably necessary for safety, legal compliance, rights protection or enforcement of these terms.',
  ]},
  { title: '3. Reporting, complaints and appeals', paragraphs: [
    'Use the in-app report and block controls for harmful or illegal content. Rights holders and authorities may contact the operator through the contact method published with the release. Reports should identify the content, explain the concern and include enough information to respond.',
    'The operator will review reports, preserve or disclose information only when legally required, and offer an appeal path where required by applicable law.',
  ]},
  { title: '4. Copyright and intellectual property', paragraphs: [
    'Do not upload or publish material you do not have permission to use. By posting content, you give Curio the limited, non-exclusive, worldwide licence needed to host, process, display and distribute that content solely to operate Online Mode until the post expires, is removed or the account is closed. This does not transfer ownership.',
    'Copyright complaints must identify the work, the allegedly infringing material, contact details and a good-faith statement. Counter-notices will be handled where law requires.',
  ]},
  { title: '5. Direct messages and encryption', paragraphs: [
    'Direct-message encryption is not currently supported. Direct messages are sent as plaintext over the protected Online Mode connection and are not end-to-end encrypted. Do not send information you cannot afford to disclose.',
    'Community cards, replies, reactions, reports, usernames and profile text are likewise not covered by end-to-end encryption.',
  ]},
  { title: '6. Service rules and availability', paragraphs: [
    'Online Mode is provided as available, may change and may be restricted where required by law. Curio is not an emergency service. Do not use it to report immediate danger; contact local emergency services instead.',
    'To the extent local law permits, the operator does not promise uninterrupted service and limits liability only as local law allows. Nothing here removes non-waivable consumer rights.',
  ]},
  { title: '7. Changes and contact', paragraphs: [
    'Material changes will be announced in-app or with the release. Continued Online Mode use after the effective date means acceptance where law permits.',
    'The release must publish the operator’s legal identity, postal address and support/privacy contact before public availability.',
  ]},
];

const LegalScreen: React.FC<{ type: LegalType }> = ({ type }) => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const dark = isDark || isAmoled;
  const ink = getTextColor(isDark);
  const muted = dark ? 'rgba(255,255,255,.62)' : 'rgba(23,27,38,.62)';
  const border = dark ? 'rgba(255,255,255,.09)' : 'rgba(23,27,38,.08)';
  const sections = type === 'privacy' ? PRIVACY : TERMS;
  const title = type === 'privacy' ? 'Privacy notice' : 'Terms of use';
  return (
    <main className="min-h-screen px-4 pt-24 pb-14" style={{ background: getBackgroundColor(isDark, isAmoled), color: ink }}>
      <button onClick={() => navigate(-1)} aria-label="Go back"
        className="fixed left-4 top-[max(14px,env(safe-area-inset-top))] z-50 w-11 h-11 rounded-full flex items-center justify-center shadow-lg"
        style={{ background: dark ? '#161A1D' : '#FFFFFF', border: '1px solid ' + border }}>
        <MaterialIcon name="arrow_back" size={20} />
      </button>
      <article className="max-w-2xl mx-auto">
        <header className="pb-6 mb-6" style={{ borderBottom: '1px solid ' + border }}>
          <p className="text-[10px] uppercase tracking-[.2em] font-bold mb-2" style={{ color: '#DA1E37' }}>Curio / Legal</p>
          <h1 className="text-[34px] leading-tight font-extrabold tracking-tight" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{title}<span style={{ color: '#DA1E37' }}>.</span></h1>
          <p className="mt-3 text-sm" style={{ color: muted }}>Effective 13 September 2026 · Applies to Curio Online Mode</p>
          <p className="mt-4 text-sm leading-relaxed" style={{ color: muted }}>
            {type === 'privacy'
              ? 'This notice explains what information Curio processes when you choose to use Online Mode. Your offline discovery library remains on your device.'
              : 'These terms explain the rules for Curio Online Mode, including profiles, community posts, replies and direct messages.'}
          </p>
        </header>
        {type === 'terms' && (
          <aside className="rounded-2xl p-4 mb-6 text-sm leading-relaxed"
            style={{ background: dark ? 'rgba(218,30,55,.10)' : 'rgba(218,30,55,.055)', border: '1px solid ' + border, color: muted }}>
            This is an operational product policy, not legal advice. Before public launch, the operator must obtain legal review in each launch market and publish its legal identity, contact address, governing law and dispute process where required.
          </aside>
        )}
        <div className="space-y-6">
          {sections.map(section => (
            <section key={section.title} className="pb-5" style={{ borderBottom: '1px solid ' + border }}>
              <h2 className="text-base font-bold mb-3">{section.title}</h2>
              {section.paragraphs.map((paragraph, index) => (
                <p key={index} className="text-sm leading-[1.8] mb-3 last:mb-0" style={{ color: muted }}>{paragraph}</p>
              ))}
            </section>
          ))}
        </div>
        <button onClick={() => navigate('/support')} className="mt-7 text-sm font-semibold" style={{ color: '#DA1E37' }}>
          Need help? Visit Help & support <span aria-hidden="true">→</span>
        </button>
      </article>
    </main>
  );
};
export default LegalScreen;
