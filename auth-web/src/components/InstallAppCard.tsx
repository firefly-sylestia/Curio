import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { MaterialIcon } from './SharedComponents';

interface InstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

const InstallAppCard: React.FC = () => {
  const [promptEvent, setPromptEvent] = useState<InstallPromptEvent | null>(null);
  const [helpOpen, setHelpOpen] = useState(false);
  const [installed, setInstalled] = useState(false);
  const isIOS = typeof navigator !== 'undefined' && /iphone|ipad|ipod/i.test(navigator.userAgent);
  useEffect(() => {
    const onPrompt = (event: Event) => { event.preventDefault(); setPromptEvent(event as InstallPromptEvent); };
    const onInstalled = () => { setInstalled(true); setPromptEvent(null); };
    window.addEventListener('beforeinstallprompt', onPrompt);
    window.addEventListener('appinstalled', onInstalled);
    setInstalled(window.matchMedia('(display-mode: standalone)').matches || (navigator as Navigator & { standalone?: boolean }).standalone === true);
    return () => { window.removeEventListener('beforeinstallprompt', onPrompt); window.removeEventListener('appinstalled', onInstalled); };
  }, []);
  if (installed || !promptEvent) return null;
  const handleInstall = async () => {
    await promptEvent.prompt();
    const choice = await promptEvent.userChoice;
    if (choice.outcome === 'accepted') setInstalled(true);
    setPromptEvent(null);
  };
  return <section className="curio-install-wrap">
    <div className="curio-install-card">
      <div className="curio-install-symbol"><MaterialIcon name="install_mobile" size={22} /></div>
      <div className="curio-install-copy"><strong>Take Curio with you</strong><span>Install it for a focused, app-like experience.</span></div>
      <button type="button" onClick={handleInstall}>{promptEvent ? 'Install' : 'Add app'} <MaterialIcon name="arrow_forward" size={15} /></button>
    </div>
    {helpOpen && createPortal(
      <div className="curio-install-backdrop" onClick={() => setHelpOpen(false)}>
        <section className="curio-install-sheet" role="dialog" aria-modal="true" aria-labelledby="curio-install-title" onClick={event => event.stopPropagation()}>
          <button type="button" className="curio-install-close" onClick={() => setHelpOpen(false)} aria-label="Close"><MaterialIcon name="close" size={20} /></button>
          <div className="curio-install-symbol"><MaterialIcon name="add_to_home_screen" size={24} /></div>
          <h2 id="curio-install-title">Add Curio to your home screen</h2>
          {isIOS ? <ol><li>Open Curio in <strong>Safari</strong>.</li><li>Tap the <strong>Share</strong> button.</li><li>Choose <strong>Add to Home Screen</strong>, then tap Add.</li></ol> : <ol><li>Open your browser menu (<strong>⋮</strong>).</li><li>Choose <strong>Install app</strong> or <strong>Add to Home screen</strong>.</li><li>Confirm the installation.</li></ol>}
          <p>{isIOS ? 'iPhone and iPad browsers use the Share menu for this. The automatic install prompt is not available on iOS.' : 'If your browser shows an install icon in the address bar, you can use that instead.'}</p>
        </section>
      </div>,
      document.body
    )}
  </section>;
};

export default InstallAppCard;