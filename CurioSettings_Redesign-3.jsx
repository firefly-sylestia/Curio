import React, { useEffect, useMemo, useState } from "react";

/**
 * Curio Settings
 * Single-file React screen.
 *
 * No external UI/icon dependencies are required.
 * Drop this file into a React/Vite project and render <App />.
 */

const SETTINGS = [
  {
    group: "Personalize",
    groupIcon: "✦",
    cards: [
      {
        id: "appearance",
        title: "Appearance",
        subtitle: "Theme, tint, and pastel color",
        icon: "moon",
        tone: "coral",
        visual: "swatches",
      },
      {
        id: "pet",
        title: "Pet designer",
        subtitle: "Draw your own Curie",
        icon: "paw",
        tone: "sage",
        visual: "pet",
      },
    ],
  },
  {
    group: "How it works",
    groupIcon: "✧",
    cards: [
      {
        id: "preferences",
        title: "Preferences",
        subtitle: "Search engine, explore, and pet behavior",
        icon: "sliders",
        tone: "blue",
        visual: "compass",
      },
      {
        id: "recording",
        title: "Recording",
        subtitle: "Voice-note quality, dictation and offline transcripts",
        icon: "mic",
        tone: "lavender",
        visual: "wave",
      },
    ],
  },
  {
    group: "Organize your world",
    groupIcon: "≡",
    cards: [
      {
        id: "categories",
        title: "Manage categories",
        subtitle: "Show, hide, or reorder lanes",
        icon: "menu",
        tone: "yellow",
        visual: "cards",
      },
      {
        id: "history",
        title: "Topic history",
        subtitle: "Revisit what you explored",
        icon: "history",
        tone: "mint",
        visual: "photos",
      },
    ],
  },
  {
    group: "Share & explore",
    groupIcon: "◇",
    cards: [
      {
        id: "share",
        title: "Share hub",
        subtitle: "Browse every design, pick a topic, share a card",
        icon: "share",
        tone: "pink",
        visual: "share",
      },
      {
        id: "experiments",
        title: "Experiments",
        subtitle: "Try features before they ship",
        icon: "sparkles",
        tone: "violet",
        visual: "flask",
      },
    ],
  },
];

const SECONDARY = [
  { id: "backup", title: "Backup & restore", subtitle: "Keep your data safe", icon: "cloud" },
  { id: "support", title: "Help & feedback", subtitle: "Get support or suggest a feature", icon: "life" },
];

const NAV = [
  { id: "all", label: "All Settings", icon: "home" },
  { id: "appearance", label: "Appearance", icon: "palette" },
  { id: "pet", label: "Pet designer", icon: "paw" },
  { id: "preferences", label: "Preferences", icon: "sliders" },
  { id: "recording", label: "Recording", icon: "mic" },
  { id: "categories", label: "Categories", icon: "menu" },
  { id: "history", label: "Topic history", icon: "history" },
  { id: "share", label: "Share hub", icon: "share" },
  { id: "experiments", label: "Experiments", icon: "sparkles" },
  { id: "backup", label: "Backup", icon: "cloud" },
  { id: "support", label: "Support", icon: "life" },
];

const DETAIL_COPY = {
  appearance: {
    title: "Appearance",
    description: "Shape Curio's visual world around you.",
    controls: ["Theme", "Accent tint", "Pastel color", "Reduce motion"],
  },
  pet: {
    title: "Pet designer",
    description: "Make your little Curie feel like yours.",
    controls: ["Body", "Face", "Accessories", "Mood"],
  },
  preferences: {
    title: "Preferences",
    description: "Choose how Curio searches, explores, and behaves.",
    controls: ["Search engine", "Explore behavior", "Pet behavior", "Default results"],
  },
  recording: {
    title: "Recording",
    description: "Tune voice notes, dictation, and offline transcription.",
    controls: ["Voice-note quality", "Dictation", "Offline transcripts", "Auto-save"],
  },
  categories: {
    title: "Manage categories",
    description: "Organize the lanes you use most.",
    controls: ["Visible categories", "Order", "Default category", "Shuffle behavior"],
  },
  history: {
    title: "Topic history",
    description: "Revisit the things that caught your curiosity.",
    controls: ["History", "Recently explored", "Clear history", "Retention"],
  },
  share: {
    title: "Share hub",
    description: "Browse designs and create beautiful share cards.",
    controls: ["Card style", "Default topic", "Export quality", "Sharing"],
  },
  experiments: {
    title: "Experiments",
    description: "Try curious ideas before they become part of Curio.",
    controls: ["Experimental features", "Animations", "Labs access", "Feedback"],
  },
  backup: {
    title: "Backup & restore",
    description: "Keep your Curio world safe.",
    controls: ["Backup", "Restore", "Export", "Automatic backup"],
  },
  support: {
    title: "Help & feedback",
    description: "Get help or send an idea to the Curio team.",
    controls: ["Help center", "Report a problem", "Suggest a feature", "About Curio"],
  },
};

function Icon({ name, size = 22, stroke = 1.9 }) {
  const common = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: stroke,
    strokeLinecap: "round",
    strokeLinejoin: "round",
    "aria-hidden": true,
  };

  const paths = {
    moon: <><path d="M20.5 15.4A8.5 8.5 0 0 1 8.6 3.5 8.5 8.5 0 1 0 20.5 15.4Z"/></>,
    paw: <><path d="M8.2 10.4c-1.5 1.3-3.3 3.1-3.3 5.2 0 1.6 1.2 2.5 2.8 2.5 1.1 0 1.6-.5 2.3-.5s1.2.5 2.3.5c1.6 0 2.8-.9 2.8-2.5 0-2.1-1.8-3.9-3.3-5.2-.8-.7-2.8-.7-3.6 0Z"/><circle cx="6.2" cy="7" r="1.7"/><circle cx="10" cy="5.1" r="1.7"/><circle cx="14" cy="5.1" r="1.7"/><circle cx="17.8" cy="7" r="1.7"/></>,
    sliders: <><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/><circle cx="9" cy="6" r="2"/><circle cx="15" cy="12" r="2"/><circle cx="11" cy="18" r="2"/></>,
    mic: <><rect x="9" y="3" width="6" height="12" rx="3"/><path d="M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3M8 21h8"/></>,
    menu: <><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/></>,
    history: <><path d="M3.5 12a8.5 8.5 0 1 0 2.5-6"/><path d="M3.5 5v5h5"/><path d="M12 7v5l3.2 2"/></>,
    share: <><circle cx="18" cy="5" r="2.5"/><circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="19" r="2.5"/><path d="m8.2 10.8 7.6-4.6M8.2 13.2l7.6 4.6"/></>,
    sparkles: <><path d="m12 3 1.2 4.8L18 9l-4.8 1.2L12 15l-1.2-4.8L6 9l4.8-1.2L12 3Z"/><path d="m19 15 .7 2.3L22 18l-2.3.7L19 21l-.7-2.3L16 18l2.3-.7L19 15Z"/></>,
    cloud: <><path d="M7.2 18.5H18a4 4 0 0 0 .5-8A6.5 6.5 0 0 0 6 9.3a4.6 4.6 0 0 0 1.2 9.2Z"/></>,
    life: <><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.5"/><path d="M5.6 5.6 9.5 9.5M14.5 14.5l3.9 3.9M18.4 5.6l-3.9 3.9M9.5 14.5l-3.9 3.9"/></>,
    home: <><path d="m4 10 8-6 8 6v9a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1Z"/><path d="M9 20v-6h6v6"/></>,
    palette: <><path d="M12 4a8 8 0 0 0 0 16h2.2a1.8 1.8 0 0 0 0-3.6H13a1.6 1.6 0 0 1 0-3.2h2.1A4.9 4.9 0 0 0 12 4Z"/><circle cx="7.8" cy="10" r=".8"/><circle cx="10" cy="7.5" r=".8"/><circle cx="14" cy="7.5" r=".8"/><circle cx="16.2" cy="10" r=".8"/></>,
    folder: <><path d="M3.5 7.5h6l2 2h9v9a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1Z"/><path d="M3.5 7.5V6.2a1.2 1.2 0 0 1 1.2-1.2h4l2 2h7.8a1.2 1.2 0 0 1 1.2 1.2v1.3"/></>,
    shield: <><path d="M12 3.5 19 6v5.3c0 4.3-2.8 7.7-7 9.2-4.2-1.5-7-4.9-7-9.2V6l7-2.5Z"/><path d="m9 12 2 2 4-4"/></>,
    help: <><circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.6 2.6 0 1 1 4.3 2c-1 .8-1.8 1.2-1.8 2.6"/><path d="M12 17h.01"/></>,
    arrow: <><path d="M5 12h13"/><path d="m13 6 6 6-6 6"/></>,
    back: <><path d="m15 6-6 6 6 6"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.5"/><path d="m16 16 4.2 4.2"/></>,
    compass: <><circle cx="12" cy="12" r="8.5"/><path d="m15.8 8.2-2.2 5.4-5.4 2.2 2.2-5.4 5.4-2.2Z"/></>,
    flask: <><path d="M9 3h6M10 3v5l-4.5 8.2A2 2 0 0 0 7.2 19h9.6a2 2 0 0 0 1.7-2.8L14 8V3"/><path d="M8 14h8"/></>,
    check: <><path d="m5 12 4 4L19 6"/></>,
    layers: <><path d="m12 3 8 4-8 4-8-4 8-4Z"/><path d="m4 12 8 4 8-4"/><path d="m4 17 8 4 8-4"/></>,
    drop: <><path d="M12 3.5S6.5 10 6.5 14.1a5.5 5.5 0 0 0 11 0C17.5 10 12 3.5 12 3.5Z"/></>,
    image: <><rect x="3.5" y="4" width="17" height="16" rx="2"/><circle cx="8.5" cy="9" r="1.5"/><path d="m4.5 17 4.8-4.7 3.2 3 2.3-2.1 4.7 4.2"/></>,
    refresh: <><path d="M20 11a8 8 0 0 0-14-5L4 8"/><path d="M4 4v4h4"/><path d="M4 13a8 8 0 0 0 14 5l2-2"/><path d="M20 20v-4h-4"/></>,
  };

  return <svg {...common}>{paths[name] || paths.help}</svg>;
}

function DecorativeVisual({ type }) {
  if (type === "swatches") {
    return (
      <div className="visual swatches">
        <span className="swatch s1" />
        <span className="swatch s2" />
        <span className="swatch s3" />
        <span className="swatch s4" />
      </div>
    );
  }
  if (type === "pet") {
    return (
      <div className="visual petVisual">
        <div className="ear left" />
        <div className="ear right" />
        <div className="petHead"><span /><span /></div>
        <div className="petBody" />
        <div className="petSpark">♡</div>
      </div>
    );
  }
  if (type === "compass") {
    return (
      <div className="visual compassVisual">
        <div className="mountain m1" />
        <div className="mountain m2" />
        <div className="compassRing"><Icon name="compass" size={68} stroke={1.35} /></div>
      </div>
    );
  }
  if (type === "wave") {
    return (
      <div className="visual waveVisual">
        {[18, 30, 52, 38, 65, 29, 48, 22, 57, 34, 45, 24].map((h, i) => <i key={i} style={{height: h}} />)}
      </div>
    );
  }
  if (type === "cards") {
    return (
      <div className="visual cardStack">
        <span /><span /><span /><span />
      </div>
    );
  }
  if (type === "photos") {
    return (
      <div className="visual photoStack">
        <span className="photo p1">☾</span><span className="photo p2">✦</span><span className="photo p3">⌁</span>
      </div>
    );
  }
  if (type === "share") {
    return (
      <div className="visual shareVisual">
        <div className="miniCard">CURIO<br /><b>stay curious</b></div>
        <div className="shareBubble"><Icon name="share" size={27} /></div>
      </div>
    );
  }
  return (
    <div className="visual flaskVisual">
      <div className="flaskShape"><Icon name="flask" size={62} stroke={1.4} /></div>
      <span>✦</span><b>✧</b>
    </div>
  );
}

function SettingCard({ item, onOpen }) {
  return (
    <button className={`settingCard ${item.tone}`} onClick={() => onOpen(item.id)}>
      <div className="cardTexture" aria-hidden="true"><i></i><i></i><i></i><i></i></div>
      <div className="cardTop">
        <div className="cardIcon"><Icon name={item.icon} size={25} /></div>
        <span className="roundArrow"><Icon name="arrow" size={18} /></span>
      </div>
      <div className="cardText">
        <h3>{item.title}</h3>
        <p>{item.subtitle}</p>
      </div>
      <DecorativeVisual type={item.visual} />
    </button>
  );
}

function AppearanceArt({ type }) {
  return (
    <div className={`appearanceArt ${type}`} aria-hidden="true">
      {type === "theme" && <><span className="artSun">☼</span><span className="artMoon">☾</span><span className="artSpark a1">✦</span><span className="artSpark a2">✧</span><div className="artWindow"><b>☀</b></div><div className="artLeaf l1"/><div className="artLeaf l2"/></>}
      {type === "tint" && <><div className="paintCard c1"/><div className="paintCard c2"/><div className="paintCard c3"/><div className="paintCard c4"/><span className="artStar">✦</span><div className="artLeaf l1"/><div className="artLeaf l2"/></>}
      {type === "pastel" && <><span className="pastelBlob b1"/><span className="pastelBlob b2"/><span className="pastelBlob b3"/><span className="pastelBlob b4"/><span className="artSpark a1">✧</span><span className="artSpark a2">✦</span></>}
      {type === "material" && <><div className="materialStack"><i/><i/><i/></div><div className="artLeaf l1"/><div className="artLeaf l2"/></>}
      {type === "tears" && <><div className="tornPhoto"><span>⌁</span><b>AZURE</b></div><div className="tearLine"/><span className="tinyDrop">◇</span></>}
      {type === "hero" && <><div className="heroPhoto hp1">☁</div><div className="heroPhoto hp2">☾</div><div className="heroPhoto hp3">✦</div><div className="artLeaf l1"/><span className="artNote">different moods<br/>same you ♡</span></>}
      {type === "adaptive" && <><div className="compassArt"><Icon name="compass" size={58} stroke={1.25}/></div><div className="artLeaf l1"/><div className="artLeaf l2"/><span className="artNote">flow with<br/>your curiosity ♡</span></>}
      {type === "curie" && <><div className="curieArt"><div className="curieEar e1"/><div className="curieEar e2"/><div className="curieFace"><i/><i/><b>•</b></div></div><div className="artLeaf l1"/><div className="artLeaf l2"/><span className="artNote">a little happier,<br/>every day</span></>}
    </div>
  );
}

function AppearanceCard({ className = "", children, art }) {
  return <div className={`appearanceCard ${className}`}>{children}{art && <AppearanceArt type={art} />}</div>;
}

function SettingPage({ item, onBack, theme, setTheme }) {
  const [categoryTint, setCategoryTint] = useState(true);
  const [pastelColors, setPastelColors] = useState(true);
  const [materialTheme, setMaterialTheme] = useState(false);
  const [materialHeroTears, setMaterialHeroTears] = useState(false);
  const [hero, setHero] = useState("Azure hero");
  const [adaptiveHero, setAdaptiveHero] = useState(true);
  const [curie, setCurie] = useState(true);
  const isAppearance = item.id === "appearance";

  return (
    <section className="settingPage">
      <div className="settingPageHeader">
        <button className="pageBack" onClick={onBack} aria-label="Back to settings"><Icon name="back" size={24} /></button>
        <div className="settingHeaderCopy">
          <div className="eyebrow">Curio / Personalize</div>
          <h1>{item.title}</h1>
          <p>{item.description}</p>
        </div>
        <div className="headerDoodles" aria-hidden="true"><span>✦</span><span>⌁</span><span>☼</span></div>
      </div>

      <div className="pageSectionLabel"><span>{isAppearance ? "Appearance" : item.title}</span><i /></div>

      {isAppearance ? (
        <div className="appearanceGrid">
          <AppearanceCard className="themeCard" art="theme">
            <div className="appearanceCardCopy">
              <div className="miniIcon"><Icon name="moon" size={24}/></div>
              <h2>Theme</h2>
              <p>Choose how Curio looks</p>
            </div>
            <Segmented value={theme === "dark" ? "Dark" : theme === "system" ? "System" : "Light"} options={["Light", "Dark", "System"]} onChange={v => setTheme(v === "Dark" ? "dark" : v === "System" ? "system" : "light")} />
            <span className="handNote">A cozy Curio,<br/>your way ♡</span>
          </AppearanceCard>

          <AppearanceCard className="halfCard tintCard" art="tint">
            <div className="appearanceCardTop"><div className="miniIcon"><Icon name="palette" size={23}/></div><ToggleSwitch value={categoryTint} onChange={setCategoryTint}/></div>
            <h2>Category tint</h2><p>Colorful page backgrounds</p>
            <span className="bottomNote">Let each category bring<br/>its own color to the app.</span>
          </AppearanceCard>

          <AppearanceCard className="halfCard pastelCard" art="pastel">
            <div className="appearanceCardTop"><div className="miniIcon"><Icon name="sparkles" size={23}/></div><ToggleSwitch value={pastelColors} onChange={setPastelColors}/></div>
            <h2>Pastel colors</h2><p>Soft category accents<br/>and page tints</p>
            <span className="bottomNote">Soft colors for a<br/>calmer little Curio.</span>
          </AppearanceCard>

          <AppearanceCard className="halfCard materialCard" art="material">
            <div className="appearanceCardTop"><div className="miniIcon"><Icon name="layers" size={23}/></div><ToggleSwitch value={materialTheme} onChange={setMaterialTheme} neutral/></div>
            <h2>Material theme</h2><p>Proper Material 3 colors: one primary, neutral surfaces, muted category families.</p>
            <span className="smallCaption">Structured color system</span>
          </AppearanceCard>

          <AppearanceCard className="halfCard tearsCard" art="tears">
            <div className="appearanceCardTop"><div className="miniIcon"><Icon name="drop" size={23}/></div><ToggleSwitch value={materialHeroTears} onChange={setMaterialHeroTears}/></div>
            <h2>Material hero tears</h2><p>Torn heroes wear the theme's container color, not rose.</p>
            <span className="smallCaption">Use the page container color</span>
          </AppearanceCard>

          <AppearanceCard className="heroCard" art="hero">
            <div className="appearanceCardTop"><div><div className="miniIcon"><Icon name="image" size={23}/></div></div><div><h2>Hero</h2><p>Choose your hero style</p></div></div>
            <Segmented value={hero} options={["Rose hero", "Azure hero"]} onChange={setHero}/>
            <span className="heroHandNote">Different moods,<br/>same you ♡</span>
          </AppearanceCard>

          <AppearanceCard className="halfCard adaptiveCard" art="adaptive">
            <div className="appearanceCardTop"><div className="miniIcon"><Icon name="refresh" size={23}/></div><ToggleSwitch value={adaptiveHero} onChange={setAdaptiveHero}/></div>
            <h2>Adaptive Hero</h2><p>Shared hero and page take the category you last picked on Spin.</p>
          </AppearanceCard>

          <AppearanceCard className="halfCard curieCard" art="curie">
            <div className="appearanceCardTop"><div className="miniIcon"><Icon name="paw" size={23}/></div><ToggleSwitch value={curie} onChange={setCurie}/></div>
            <h2>Curie</h2><p>Pixel companion that grows with your XP.</p>
          </AppearanceCard>
        </div>
      ) : (
        <div className="controlCard genericControls">
          <div className="genericIntro">
            <div className={`genericIllustration ${item.id}`}><Icon name={item.icon || "sparkles"} size={42} stroke={1.45}/></div>
            <div><h2>Make it yours</h2><p>These controls belong to the same visual world as the rest of Curio.</p></div>
          </div>
          {item.controls.map((control, index) => <button className="genericRow" key={control}><span className="rowIndex">0{index+1}</span><span className="genericRowText"><strong>{control}</strong><small>Customize your {control.toLowerCase()}.</small></span><Icon name="arrow" size={18}/></button>)}
        </div>
      )}

      <div className="pageTip"><span>✦</span><div><strong>A little Curio note</strong><p>Your choices are designed to keep the app feeling personal, calm, and a little playful.</p></div></div>
    </section>
  );
}

function ToggleSwitch({ value, onChange, neutral = false }) {
  return <button type="button" className={`switch ${value ? "on" : ""} ${neutral ? "neutralSwitch" : ""}`} onClick={() => onChange(!value)} aria-pressed={value}><span/></button>;
}

function Segmented({ value, options, onChange }) {
  return (
    <div className="segmented" role="tablist">
      {options.map(option => (
        <button key={option} className={value === option ? "selected" : ""} onClick={() => onChange(option)}>
          {value === option && <Icon name="check" size={16} stroke={2.2} />}
          <span>{option}</span>
        </button>
      ))}
    </div>
  );
}

function ToggleRow({ title, subtitle, value, onChange, neutral = false }) {
  return (
    <div className="toggleRow">
      <div className="toggleCopy">
        <h3>{title}</h3>
        <p>{subtitle}</p>
      </div>
      <button className={`switch ${value ? "on" : ""} ${neutral ? "neutralSwitch" : ""}`} onClick={() => onChange(!value)} aria-pressed={value} aria-label={title}>
        <span />
      </button>
    </div>
  );
}

function App() {
  const [activeNav, setActiveNav] = useState("all");
  const [query, setQuery] = useState("");
  const [opened, setOpened] = useState(null);
  const [theme, setTheme] = useState("light");
  const [systemDark, setSystemDark] = useState(false);

  useEffect(() => {
    const media = window.matchMedia?.("(prefers-color-scheme: dark)");
    if (!media) return;
    const sync = () => setSystemDark(media.matches);
    sync();
    media.addEventListener?.("change", sync);
    return () => media.removeEventListener?.("change", sync);
  }, []);

  const filteredGroups = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return SETTINGS;
    return SETTINGS
      .map(group => ({
        ...group,
        cards: group.cards.filter(c =>
          `${c.title} ${c.subtitle} ${group.group}`.toLowerCase().includes(q)
        ),
      }))
      .filter(group => group.cards.length);
  }, [query]);

  const allSecondary = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return SECONDARY;
    return SECONDARY.filter(x =>
      `${x.title} ${x.subtitle}`.toLowerCase().includes(q)
    );
  }, [query]);

  const openSetting = id => {
    setOpened({ ...(DETAIL_COPY[id] || {}), id });
    setActiveNav(id);
    window.scrollTo?.({ top: 0, behavior: "smooth" });
  };
  const goHome = () => { setOpened(null); setActiveNav("all"); window.scrollTo?.({ top: 0, behavior: "smooth" }); };
  const effectiveDark = theme === "dark" || (theme === "system" && systemDark);

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:wght@500;600&display=swap');

        :root {
          --ink: #38252a;
          --muted: #795f64;
          --line: rgba(73, 45, 49, .13);
          --paper: #fbf7f0;
          --paper2: #f6efe5;
          --cream: #f2e6d4;
          --brown: #815947;
          --shadow: 0 16px 42px rgba(77, 53, 45, .09);
        }

        * { box-sizing: border-box; }
        body {
          margin: 0;
          background: #f1ebe4;
          color: var(--ink);
          font-family: "DM Sans", system-ui, sans-serif;
        }
        button, input { font: inherit; }
        button { -webkit-tap-highlight-color: transparent; }

        .settingsShell {
          min-height: 100vh;
          background:
            radial-gradient(circle at 9% 8%, rgba(235, 199, 181, .32), transparent 23%),
            radial-gradient(circle at 92% 28%, rgba(202, 218, 199, .28), transparent 26%),
            var(--paper);
          overflow-x: hidden;
        }

        .topBar {
          height: 76px;
          display: flex;
          align-items: center;
          gap: 16px;
          padding: 18px 32px;
          border-bottom: 1px solid var(--line);
          background: rgba(251,247,240,.86);
          backdrop-filter: blur(18px);
          position: sticky;
          top: 0;
          z-index: 20;
        }

        .backButton {
          width: 43px;
          height: 43px;
          border: 0;
          border-radius: 50%;
          background: #fffaf3;
          color: #5c3e36;
          display: grid;
          place-items: center;
          box-shadow: 0 5px 18px rgba(71, 45, 35, .10);
          cursor: pointer;
        }

        .brandMini {
          display: flex;
          flex-direction: column;
          line-height: 1;
        }
        .brandMini strong {
          font-family: "Playfair Display", serif;
          font-size: 21px;
          font-weight: 600;
        }
        .brandMini span {
          color: var(--muted);
          font-size: 11px;
          margin-top: 5px;
        }

        .topSpacer { flex: 1; }
        .topStatus {
          font-size: 12px;
          color: #866e70;
          padding: 9px 13px;
          border: 1px solid var(--line);
          border-radius: 999px;
          background: rgba(255,255,255,.52);
        }

        .layout {
          width: min(1320px, calc(100% - 48px));
          margin: 0 auto;
          display: grid;
          grid-template-columns: 132px minmax(0, 1fr);
          gap: 34px;
          padding: 32px 0 70px;
        }

        .sideNav {
          position: sticky;
          top: 100px;
          height: max-content;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
        }

        .navItem {
          width: 118px;
          min-height: 88px;
          padding: 13px 8px;
          border: 0;
          background: transparent;
          color: #71595c;
          border-radius: 22px;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          transition: .2s ease;
        }
        .navItem:hover { background: rgba(255,255,255,.72); transform: translateY(-1px); }
        .navItem.active {
          background: #815947;
          color: #fff9f1;
          box-shadow: 0 12px 28px rgba(102, 67, 51, .18);
        }
        .navIcon { opacity: .92; }
        .navLabel {
          font-size: 12px;
          line-height: 1.15;
          text-align: center;
          font-weight: 600;
        }

        .content { min-width: 0; }

        .pageIntro {
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: 24px;
          margin: 4px 0 26px;
        }

        .eyebrow {
          color: #9a7368;
          text-transform: uppercase;
          letter-spacing: .16em;
          font-size: 10px;
          font-weight: 700;
          margin-bottom: 8px;
        }

        .pageIntro h1 {
          margin: 0;
          font-family: "Playfair Display", Georgia, serif;
          font-size: clamp(42px, 5vw, 68px);
          line-height: .95;
          letter-spacing: -.035em;
          font-weight: 600;
        }

        .pageIntro p {
          margin: 10px 0 0;
          color: var(--muted);
          font-size: 15px;
        }

        .quickButton {
          border: 1px solid rgba(131, 90, 62, .14);
          background: #fff9ee;
          color: #684632;
          padding: 13px 17px;
          border-radius: 18px;
          display: flex;
          align-items: center;
          gap: 9px;
          cursor: pointer;
          box-shadow: 0 8px 22px rgba(93, 61, 43, .08);
          font-weight: 700;
          white-space: nowrap;
        }

        .searchRow {
          display: flex;
          gap: 12px;
          margin-bottom: 32px;
        }

        .searchBox {
          height: 60px;
          flex: 1;
          border-radius: 22px;
          background: rgba(255,255,255,.68);
          border: 1px solid var(--line);
          display: flex;
          align-items: center;
          padding: 0 20px;
          gap: 13px;
          box-shadow: inset 0 1px 0 rgba(255,255,255,.7);
        }
        .searchBox svg { color: #806b6e; flex: 0 0 auto; }
        .searchBox input {
          width: 100%;
          border: 0;
          outline: 0;
          background: transparent;
          color: var(--ink);
          font-size: 16px;
        }
        .searchBox input::placeholder { color: #9c878a; }

        .group {
          margin-bottom: 31px;
          scroll-margin-top: 110px;
        }

        .groupHeading {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: 20px;
          margin: 0 4px 14px;
        }
        .groupHeading h2 {
          margin: 0;
          font-family: "Playfair Display", serif;
          font-size: 25px;
          font-weight: 600;
          letter-spacing: -.015em;
        }
        .groupHeading h2::after {
          content: "";
          display: inline-block;
          width: 38px;
          height: 1px;
          background: #9d7b72;
          vertical-align: middle;
          margin: 0 0 5px 11px;
          opacity: .7;
        }
        .groupLink {
          color: #886d6d;
          font-size: 12px;
          font-weight: 600;
          text-decoration: none;
        }

        .cardGrid {
          display: grid;
          grid-template-columns: repeat(2, minmax(0,1fr));
          gap: 16px;
        }

        .settingCard {
          min-height: 220px;
          position: relative;
          overflow: hidden;
          isolation: isolate;
          border: 1px solid rgba(65,45,43,.08);
          border-radius: 27px;
          padding: 24px;
          text-align: left;
          cursor: pointer;
          color: var(--ink);
          box-shadow: 0 10px 30px rgba(72, 51, 45, .07), inset 0 1px 0 rgba(255,255,255,.5);
          transition: transform .2s ease, box-shadow .2s ease;
          background-size: auto, auto, auto, auto;
        }
        .settingCard::before {
          content: "";
          position: absolute;
          width: 190px;
          height: 125px;
          border-radius: 48% 52% 58% 42%;
          background: rgba(255,255,255,.22);
          top: -47px;
          right: -38px;
          transform: rotate(-18deg);
          z-index: -1;
        }
        .settingCard::after {
          content: "";
          position: absolute;
          width: 135px;
          height: 155px;
          border-radius: 52% 48% 42% 58%;
          background: rgba(255,255,255,.14);
          left: -72px;
          bottom: -84px;
          transform: rotate(35deg);
          z-index: -1;
        }
        .cardTexture {
          position:absolute; inset:0; pointer-events:none; overflow:hidden; z-index:0; opacity:.68;
          background:
            radial-gradient(circle at 13% 83%, rgba(255,255,255,.18) 0 2px, transparent 3px),
            radial-gradient(circle at 18% 76%, rgba(95,67,61,.08) 0 1.5px, transparent 2.5px),
            linear-gradient(118deg, transparent 0 46%, rgba(255,255,255,.11) 47% 48%, transparent 49% 100%);
          background-size: 32px 32px, 42px 42px, 100% 100%;
        }
        .cardTexture i { position:absolute; display:block; border:1px solid rgba(255,255,255,.28); opacity:.8; }
        .cardTexture i:nth-child(1) { width:72px; height:72px; border-radius:50%; right:-24px; top:54px; }
        .cardTexture i:nth-child(2) { width:48px; height:48px; border-radius:13px; right:31px; bottom:-20px; transform:rotate(18deg); }
        .cardTexture i:nth-child(3) { width:8px; height:8px; border:0; border-radius:50%; left:42%; bottom:21px; background:rgba(255,255,255,.35); box-shadow:20px -9px rgba(255,255,255,.22), 35px 3px rgba(255,255,255,.25), -17px -12px rgba(255,255,255,.22); }
        .cardTexture i:nth-child(4) { width:115px; height:38px; border-radius:50%; left:24%; top:-27px; transform:rotate(-11deg); }
        .settingCard:hover {
          transform: translateY(-4px);
          box-shadow: 0 18px 38px rgba(72, 51, 45, .12);
        }
        .settingCard:active { transform: translateY(-1px) scale(.995); }

        .coral { background: radial-gradient(circle at 84% 13%, rgba(255,255,255,.25) 0 9%, transparent 10%), radial-gradient(circle at 16% 102%, rgba(166,91,79,.11) 0 22%, transparent 23%), linear-gradient(135deg, #f4b6a8 0%, #eea391 52%, #e7a08f 100%); }
        .sage { background: radial-gradient(circle at 89% 16%, rgba(255,255,255,.27) 0 11%, transparent 12%), radial-gradient(circle at 6% 92%, rgba(81,112,77,.10) 0 23%, transparent 24%), linear-gradient(135deg, #d0e1c9 0%, #c2d8bb 55%, #b7d0b4 100%); }
        .blue { background: radial-gradient(circle at 91% 20%, rgba(255,255,255,.28) 0 12%, transparent 13%), radial-gradient(circle at 4% 94%, rgba(65,101,125,.12) 0 25%, transparent 26%), linear-gradient(135deg, #c0deeb 0%, #b1d5e5 55%, #a5cade 100%); }
        .lavender { background: radial-gradient(circle at 91% 14%, rgba(255,255,255,.27) 0 12%, transparent 13%), radial-gradient(circle at 8% 96%, rgba(88,73,126,.10) 0 23%, transparent 24%), linear-gradient(135deg, #d6cfeb 0%, #c9bee4 55%, #bcaed9 100%); }
        .yellow { background: radial-gradient(circle at 88% 15%, rgba(255,255,255,.28) 0 11%, transparent 12%), radial-gradient(circle at 7% 97%, rgba(147,103,43,.11) 0 24%, transparent 25%), linear-gradient(135deg, #f9dfa6 0%, #f6d48e 55%, #f1c875 100%); }
        .mint { background: radial-gradient(circle at 89% 15%, rgba(255,255,255,.27) 0 11%, transparent 12%), radial-gradient(circle at 7% 96%, rgba(67,111,82,.10) 0 24%, transparent 25%), linear-gradient(135deg, #cfe4d5 0%, #c2dccb 55%, #b5d3c0 100%); }
        .pink { background: radial-gradient(circle at 89% 15%, rgba(255,255,255,.28) 0 11%, transparent 12%), radial-gradient(circle at 7% 96%, rgba(138,72,88,.10) 0 24%, transparent 25%), linear-gradient(135deg, #f2c2c8 0%, #ecb1ba 55%, #e5a6b1 100%); }
        .violet { background: radial-gradient(circle at 89% 15%, rgba(255,255,255,.27) 0 11%, transparent 12%), radial-gradient(circle at 7% 96%, rgba(89,67,126,.11) 0 24%, transparent 25%), linear-gradient(135deg, #d3c4e7 0%, #c8b7e0 55%, #b9a5d5 100%); }

        .cardTop {
          position: relative;
          z-index: 2;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .cardIcon {
          width: 43px;
          height: 43px;
          border-radius: 15px;
          display: grid;
          place-items: center;
          background: rgba(255,255,255,.33);
          backdrop-filter: blur(8px);
        }
        .roundArrow {
          width: 38px;
          height: 38px;
          border-radius: 50%;
          display: grid;
          place-items: center;
          background: rgba(99, 66, 58, .20);
          color: #fff9f1;
          transition: transform .2s ease;
        }
        .settingCard:hover .roundArrow { transform: translateX(2px); }

        .cardText {
          position: relative;
          z-index: 2;
          max-width: 72%;
          margin-top: 20px;
        }
        .cardText h3 {
          margin: 0 0 6px;
          font-size: 19px;
          letter-spacing: -.02em;
          font-weight: 700;
        }
        .cardText p {
          margin: 0;
          font-size: 13px;
          line-height: 1.42;
          color: rgba(50, 36, 38, .78);
          max-width: 330px;
        }

        .visual {
          position: absolute;
          right: 20px;
          bottom: 9px;
          width: 150px;
          height: 86px;
          z-index: 1;
          pointer-events: none;
        }

        .swatches {
          transform: rotate(-5deg);
          opacity: .9;
        }
        .swatch {
          position: absolute;
          width: 58px;
          height: 75px;
          border: 6px solid rgba(255,249,243,.82);
          border-radius: 7px;
          box-shadow: 0 6px 14px rgba(74,46,39,.12);
        }
        .s1 { left: 18px; top: 8px; background: #e8b0a0; transform: rotate(-11deg); }
        .s2 { left: 47px; top: 1px; background: #b8c9b0; transform: rotate(1deg); }
        .s3 { left: 78px; top: 10px; background: #b7a9cf; transform: rotate(12deg); }
        .s4 { left: 42px; top: 25px; background: #d6b1c1; transform: rotate(-2deg); }

        .petVisual { transform: translateY(5px); }
        .petHead {
          position: absolute;
          left: 52px; top: 26px;
          width: 72px; height: 58px;
          border-radius: 50% 50% 43% 43%;
          background: #f4eee3;
          box-shadow: inset 0 -6px 0 rgba(116,87,72,.08), 0 8px 18px rgba(62,74,54,.11);
        }
        .petHead::before {
          content: "";
          position: absolute; left: 21px; top: 29px;
          width: 7px; height: 5px; border-radius: 50%; background: #4b3a35;
          box-shadow: 25px 0 #4b3a35;
        }
        .petHead::after {
          content: "•";
          position: absolute; left: 33px; top: 29px; color: #a36f65; font-size: 15px;
        }
        .ear {
          position: absolute; top: 11px; width: 34px; height: 39px;
          background: #eee5d8; transform: rotate(25deg);
          border-radius: 8px 24px 8px 20px;
        }
        .ear.left { left: 55px; }
        .ear.right { left: 89px; transform: scaleX(-1) rotate(25deg); }
        .petBody {
          position: absolute; left: 43px; top: 66px; width: 91px; height: 45px;
          background: #9c735f; border-radius: 45px 45px 18px 18px; opacity: .85;
        }
        .petSpark { position:absolute; right:3px; top:7px; font-size:29px; color:rgba(78,91,67,.65); }

        .compassVisual { opacity: .78; }
        .mountain { position:absolute; bottom:0; border-style:solid; border-width:0 45px 58px; border-color:transparent transparent rgba(70,101,108,.23); }
        .m1 { left: 15px; transform: scale(.85); }
        .m2 { left: 66px; border-width:0 58px 72px; border-color:transparent transparent rgba(73,94,116,.16); }
        .compassRing { position:absolute; right:5px; bottom:5px; width:75px; height:75px; border-radius:50%; display:grid; place-items:center; background:rgba(255,255,255,.38); }

        .waveVisual { display:flex; align-items:center; justify-content:flex-end; gap:5px; padding-top:24px; opacity:.55; }
        .waveVisual i { width:6px; border-radius:99px; background:#6e628e; }

        .cardStack span { position:absolute; width:84px; height:56px; border-radius:9px; border:3px solid rgba(255,255,255,.72); box-shadow:0 7px 14px rgba(110,76,37,.12); }
        .cardStack span:nth-child(1){ right:6px; bottom:4px; background:#8da993; transform:rotate(8deg); }
        .cardStack span:nth-child(2){ right:37px; bottom:13px; background:#d2a87b; transform:rotate(-3deg); }
        .cardStack span:nth-child(3){ right:66px; bottom:25px; background:#9eb8c0; transform:rotate(-13deg); }
        .cardStack span:nth-child(4){ right:92px; bottom:36px; background:#f4eadc; transform:rotate(-21deg); }

        .photoStack .photo { position:absolute; width:68px; height:52px; border:4px solid #f8f0e8; border-radius:5px; display:grid; place-items:center; font-size:23px; box-shadow:0 6px 14px rgba(62,83,66,.12); }
        .p1 { right:3px; bottom:3px; background:#7e9c86; transform:rotate(10deg); }
        .p2 { right:42px; bottom:15px; background:#c79f86; transform:rotate(-4deg); }
        .p3 { right:80px; bottom:30px; background:#9bb5a1; transform:rotate(-15deg); }

        .shareVisual .miniCard { position:absolute; right:30px; bottom:4px; width:95px; height:67px; border-radius:10px; background:#f6e9d5; box-shadow:0 7px 15px rgba(78,52,47,.13); padding:11px; font-size:7px; letter-spacing:.15em; transform:rotate(-7deg); }
        .miniCard b { font-size:12px; letter-spacing:0; line-height:1.1; }
        .shareBubble { position:absolute; right:4px; top:2px; width:43px; height:43px; border-radius:50%; background:rgba(255,255,255,.42); display:grid; place-items:center; }

        .flaskVisual .flaskShape { position:absolute; right:20px; bottom:-2px; color:rgba(255,255,255,.72); }
        .flaskVisual > span { position:absolute; right:94px; top:2px; font-size:25px; color:rgba(255,255,255,.75); }
        .flaskVisual > b { position:absolute; right:75px; top:34px; font-size:19px; color:rgba(255,255,255,.72); }

        .secondaryGrid {
          display:grid;
          grid-template-columns:repeat(2,minmax(0,1fr));
          gap:16px;
          margin-top: -12px;
        }
        .secondaryCard {
          min-height:86px;
          border:1px solid var(--line);
          background:rgba(255,255,255,.68);
          border-radius:21px;
          padding:16px 19px;
          display:flex;
          align-items:center;
          gap:14px;
          cursor:pointer;
          text-align:left;
          color:var(--ink);
          transition:.2s ease;
        }
        .secondaryCard:hover { transform:translateY(-2px); box-shadow:var(--shadow); }
        .secondaryIcon {
          width:42px; height:42px; border-radius:14px; background:#f2e8dc;
          display:grid; place-items:center; color:#755647; flex:0 0 auto;
        }
        .secondaryText { flex:1; }
        .secondaryText strong { display:block; font-size:14px; margin-bottom:3px; }
        .secondaryText span { color:#866f72; font-size:11px; }
        .secondaryArrow { color:#947e80; }

        .footerNote {
          margin-top: 38px;
          padding: 32px 22px;
          border-radius: 27px;
          background: #e9dfd4;
          text-align:center;
          position:relative;
          overflow:hidden;
        }
        .footerNote::before, .footerNote::after {
          content:"";
          position:absolute;
          width:180px; height:90px; border-radius:50%;
          border:1px solid rgba(117,86,68,.12);
        }
        .footerNote::before { left:-70px; bottom:-50px; }
        .footerNote::after { right:-70px; top:-50px; }
        .footerNote .tinyStars { color:#8b695c; font-size:18px; letter-spacing:8px; }
        .footerNote h3 { font-family:"Playfair Display",serif; margin:8px 0 3px; font-size:25px; font-weight:500; }
        .footerNote p { margin:0; color:#866f72; font-size:12px; }

        .emptyState {
          padding:60px 20px; text-align:center; color:#806b6e;
          border:1px dashed var(--line); border-radius:24px; background:rgba(255,255,255,.4);
        }

        @keyframes pageIn { from{opacity:0; transform:translateY(7px)} to{opacity:1; transform:none} }

        .settingsShell {
          --control-bg:#f3e9d4;
          --control-border:rgba(102,72,58,.06);
          --control-line:rgba(91,65,55,.48);
          --control-shadow:rgba(76,53,42,.07);
          --selected-bg:#f7e8b8;
          --selected-border:rgba(154,119,67,.2);
          --tip-bg:#eee3d7;
        }
        .darkMode {
          --paper:#171619;
          --paper2:#1e1d20;
          --cream:#28252a;
          --ink:#f5eee8;
          --muted:#b9aaac;
          --line:rgba(255,255,255,.10);
          --control-bg:#29272a;
          --control-border:rgba(255,255,255,.06);
          --control-line:rgba(255,255,255,.20);
          --control-shadow:rgba(0,0,0,.30);
          --selected-bg:#443a2d;
          --selected-border:rgba(244,211,147,.22);
          --tip-bg:#252326;
          background:radial-gradient(circle at 8% 8%,rgba(126,76,67,.15),transparent 25%),radial-gradient(circle at 92% 28%,rgba(78,105,88,.12),transparent 25%),#171619;
        }
        .darkMode .topBar { background:rgba(23,22,25,.88); }
        .darkMode .backButton, .darkMode .pageBack { background:#252326;color:#f0ddd0;box-shadow:0 7px 20px rgba(0,0,0,.22); }
        .darkMode .navItem { color:#b9abad; }
        .darkMode .navItem:hover { background:#252326; }
        .darkMode .navItem.active { background:#8d6654;color:#fff6ee; }
        .darkMode .searchBox, .darkMode .secondaryCard { background:rgba(37,35,38,.78); }
        .darkMode .quickButton { background:#2c2825;color:#f0d7b5;border-color:rgba(255,255,255,.07); }
        .darkMode .groupHeading, .darkMode .pageSectionLabel { color:#d5b9ae; }
        .darkMode .footerNote { background:#252326; }
        .darkMode .footerNote h3 { color:#f1e6df; }
        .darkMode .settingPageHeader p, .darkMode .pageTip p { color:#b8aaad; }
        .darkMode .pageSectionLabel i { background:#b48f83; }
        .darkMode .cardIcon { background:rgba(255,255,255,.14); }
        .darkMode .secondaryIcon { background:#322d2d;color:#d7b8a9; }
        .darkMode .secondaryText span { color:#a99b9e; }
        .darkMode .detailRow { background:#242225; }
        .darkMode .emptyState { background:rgba(37,35,38,.6); }
        .darkMode .switch { background:#3a3638; box-shadow:inset 0 0 0 3px rgba(255,255,255,.07); }
        .darkMode .switch span { background:#e8ded8; }
        .darkMode .switch.on { background:#d96d88; box-shadow:none; }
        .darkMode .neutralSwitch.on { background:#75624e; }
        .darkMode .neutralSwitch.on span { background:#bca68f; }
        .darkMode .coral { background:linear-gradient(135deg,#743f42,#693a42); }
        .darkMode .sage { background:linear-gradient(135deg,#3c5140,#34483a); }
        .darkMode .blue { background:linear-gradient(135deg,#345363,#314b59); }
        .darkMode .lavender { background:linear-gradient(135deg,#4a4164,#40385b); }
        .darkMode .yellow { background:linear-gradient(135deg,#62502f,#57452a); }
        .darkMode .mint { background:linear-gradient(135deg,#385345,#324a3e); }
        .darkMode .pink { background:linear-gradient(135deg,#693f4d,#603946); }
        .darkMode .violet { background:linear-gradient(135deg,#50416b,#45385e); }


        /* --- Illustrated setting interiors --- */
        .settingPage { animation:pageIn .28s ease; }
        .settingPageHeader { min-height:188px; display:flex; align-items:flex-end; gap:18px; position:relative; overflow:hidden; padding:24px 6px 28px; margin-bottom:24px; }
        .settingPageHeader::before { content:""; position:absolute; width:310px; height:160px; right:-75px; top:-58px; border-radius:50%; background:rgba(226,190,170,.22); transform:rotate(-12deg); }
        .settingPageHeader::after { content:""; position:absolute; width:210px; height:100px; left:34%; bottom:-66px; border:1px solid rgba(132,100,87,.14); border-radius:50%; }
        .pageBack { flex:0 0 auto; width:48px; height:48px; border:0; border-radius:17px; background:#fff9f1; color:#62463c; display:grid; place-items:center; cursor:pointer; box-shadow:0 8px 22px rgba(77,53,45,.10); position:relative; z-index:2; }
        .settingHeaderCopy { position:relative; z-index:2; }
        .settingPageHeader h1 { margin:0; font-family:"Playfair Display",serif; font-size:clamp(42px,5vw,65px); line-height:.96; letter-spacing:-.035em; }
        .settingPageHeader p { margin:9px 0 0; color:#795f64; font-size:15px; }
        .headerDoodles { position:absolute; right:18px; top:26px; color:#b99380; opacity:.42; display:flex; gap:30px; font-size:28px; transform:rotate(-8deg); }
        .pageSectionLabel { display:flex; align-items:center; gap:12px; color:#775b62; font-family:"Playfair Display",serif; font-size:22px; margin:0 4px 13px; }
        .pageSectionLabel i { width:42px; height:1px; background:#a6857b; display:block; }
        .appearanceGrid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }
        .appearanceCard { position:relative; overflow:hidden; isolation:isolate; min-height:204px; border-radius:25px; border:1px solid rgba(83,57,51,.08); padding:22px; color:#3b242a; box-shadow:0 13px 34px rgba(75,54,48,.075), inset 0 1px rgba(255,255,255,.58); }
        .appearanceCard::before { content:""; position:absolute; inset:0; z-index:-3; background:radial-gradient(circle at 91% 14%,rgba(255,255,255,.48),transparent 18%),radial-gradient(circle at 6% 95%,rgba(90,65,60,.07),transparent 28%); }
        .appearanceCard::after { content:""; position:absolute; width:170px;height:92px;right:-55px;top:-34px;border-radius:50%;background:rgba(255,255,255,.22);transform:rotate(-16deg);z-index:-2; }
        .themeCard { grid-column:1/-1; min-height:205px; background:linear-gradient(135deg,#e9edf2 0%,#dfe9f1 48%,#d5e4ee 100%); }
        .tintCard { background:linear-gradient(135deg,#f5c5c7,#efb4bd 58%,#e8aeb8); }
        .pastelCard { background:linear-gradient(135deg,#d9d0ec,#cec3e7 55%,#c1b4df); }
        .materialCard { background:linear-gradient(135deg,#c8dfeb,#bad6e4 55%,#b0cddc); }
        .tearsCard { background:linear-gradient(135deg,#eee5dc,#e6d9cf 55%,#dbc8b7); }
        .heroCard { grid-column:1/-1; min-height:220px; background:linear-gradient(135deg,#f3c8d1,#efbbc8 55%,#e8b0be); }
        .adaptiveCard { background:linear-gradient(135deg,#d7def2,#cdd3ea 55%,#c2c8e2); }
        .curieCard { background:linear-gradient(135deg,#d4e2d5,#c9ddcd 55%,#bdd5c4); }
        .appearanceCardCopy,.appearanceCardTop { position:relative; z-index:4; }
        .appearanceCardTop { display:flex; justify-content:space-between; align-items:flex-start; }
        .miniIcon { width:43px;height:43px;border-radius:14px;background:rgba(255,255,255,.37);display:grid;place-items:center;box-shadow:inset 0 1px rgba(255,255,255,.55); }
        .appearanceCard h2 { position:relative; z-index:4; margin:13px 0 4px; font-size:20px; line-height:1.1; letter-spacing:-.025em; }
        .appearanceCard p { position:relative; z-index:4; margin:0; max-width:420px; color:rgba(59,42,46,.72); font-size:13px; line-height:1.43; }
        .themeCard h2 { margin-top:0; margin-left:57px; margin-top:-40px; }
        .themeCard .appearanceCardCopy > p { margin-left:57px; }
        .themeCard .segmented { margin-top:27px; max-width:620px; }
        .handNote { position:absolute; right:26px; top:30px; z-index:4; color:#7084a2; font-family:"Playfair Display",serif; font-size:13px; transform:rotate(-5deg); line-height:1.3; }
        .bottomNote { position:absolute; left:22px; bottom:19px; z-index:4; font-family:"Playfair Display",serif; font-size:12px; line-height:1.35; color:rgba(76,49,55,.58); }
        .smallCaption { position:absolute; left:22px; bottom:17px; z-index:4; color:rgba(59,42,46,.48); font-size:10px; letter-spacing:.04em; }
        .heroCard .appearanceCardTop > div:last-child { margin-left:12px; flex:1; }
        .heroCard .appearanceCardTop h2 { margin:0 0 4px; }
        .heroCard .appearanceCardTop p { margin:0; }
        .heroCard .segmented { width:min(570px,68%); margin-top:30px; }
        .heroHandNote { position:absolute; right:28px; top:48px; z-index:4; color:#9b6678; font-family:"Playfair Display",serif; font-size:12px; transform:rotate(6deg); line-height:1.35; }
        .segmented { min-height:57px; display:grid; grid-template-columns:repeat(var(--segments,3),1fr); overflow:hidden; border:1px solid rgba(95,73,69,.18); border-radius:999px; background:rgba(255,255,255,.22); position:relative; z-index:5; }
        .segmented button { border:0; border-right:1px solid rgba(95,73,69,.18); background:transparent; color:#523b43; display:flex;align-items:center;justify-content:center;gap:8px;cursor:pointer;font-size:14px; }
        .segmented button:last-child { border-right:0; }
        .segmented button.selected { background:#f9e5b0; box-shadow:inset 0 1px rgba(255,255,255,.55); }
        .segmented button.selected svg { color:#6d4b43; }
        .switch { width:62px;height:38px;border:0;border-radius:999px;padding:4px;background:#e6ddd2;box-shadow:inset 0 0 0 2px rgba(95,72,61,.08);cursor:pointer;position:relative;z-index:7;flex:0 0 auto; }
        .switch span { display:block;width:30px;height:30px;border-radius:50%;background:#aa9b91;box-shadow:0 2px 5px rgba(69,48,43,.15);transition:.2s; }
        .switch.on { background:#f37f9b; box-shadow:none; }
        .switch.on span { transform:translateX(24px); background:#fffaf6; }
        .neutralSwitch.on { background:#d9c8ab; }
        .neutralSwitch.on span { background:#b9a68f; transform:translateX(24px); }
        .appearanceArt { position:absolute; inset:0; z-index:1; pointer-events:none; opacity:.95; }
        .appearanceArt .artLeaf { position:absolute; width:42px;height:18px;border-radius:100% 0 100% 0;background:rgba(92,126,82,.55);transform:rotate(-28deg);bottom:12px;right:54px; }
        .appearanceArt .artLeaf.l2 { width:35px;height:15px;transform:rotate(31deg);right:25px;bottom:18px;background:rgba(103,135,86,.43); }
        .themeCard .artWindow { position:absolute;right:56px;bottom:-7px;width:105px;height:120px;border-radius:55px 55px 0 0;border:7px solid rgba(104,127,160,.22);background:linear-gradient(#b9d6ec,#edf0dc);box-shadow:inset 0 0 0 4px rgba(255,255,255,.23); }
        .themeCard .artWindow b { position:absolute;right:20px;top:16px;font-size:27px;color:#d6a75f; }
        .themeCard .artSun,.themeCard .artMoon { position:absolute;z-index:3;font-size:28px;color:rgba(82,105,135,.55); }
        .themeCard .artSun { right:157px;bottom:54px;}.themeCard .artMoon{right:122px;bottom:78px;}
        .artSpark { position:absolute;color:rgba(105,93,155,.5);font-size:23px; }.artSpark.a1{right:184px;top:35px}.artSpark.a2{right:142px;top:65px;font-size:17px}
        .paintCard { position:absolute;width:60px;height:79px;border:6px solid rgba(255,248,240,.72);border-radius:8px;box-shadow:0 8px 16px rgba(74,47,49,.12);bottom:8px;right:44px;transform:rotate(-7deg);background:#d67e93; }.paintCard.c2{right:5px;bottom:12px;transform:rotate(13deg);background:#d7b7a4}.paintCard.c3{right:75px;bottom:-1px;transform:rotate(7deg);background:#c28aaf}.paintCard.c4{right:34px;bottom:0;transform:rotate(-1deg);background:#efa6a2}.artStar{position:absolute;right:23px;bottom:70px;font-size:22px;color:rgba(255,255,255,.68)}
        .pastelBlob{position:absolute;border-radius:50%;filter:blur(.2px);opacity:.5}.pastelBlob.b1{width:75px;height:75px;right:17px;bottom:8px;background:#f3c8a6}.pastelBlob.b2{width:62px;height:62px;right:67px;bottom:22px;background:#a7c8df}.pastelBlob.b3{width:48px;height:48px;right:102px;bottom:4px;background:#c9a8d5}.pastelBlob.b4{width:28px;height:28px;right:33px;bottom:76px;background:#f1a7c0}.materialStack{position:absolute;right:36px;bottom:3px;width:112px;height:100px}.materialStack i{position:absolute;width:77px;height:47px;border-radius:7px;border:4px solid rgba(255,255,255,.6);transform:rotate(-15deg);background:#d6e5ee;box-shadow:0 7px 12px rgba(55,80,91,.1)}.materialStack i:nth-child(1){right:4px;bottom:2px;background:#9bb3a8}.materialStack i:nth-child(2){right:24px;bottom:25px;background:#f1dfca;transform:rotate(8deg)}.materialStack i:nth-child(3){right:48px;bottom:42px;background:#9db7dc;transform:rotate(-9deg)}
        .tornPhoto{position:absolute;right:18px;bottom:6px;width:104px;height:78px;background:linear-gradient(135deg,#9fb8d1,#dce7e8);border:5px solid rgba(255,249,240,.72);box-shadow:0 8px 17px rgba(76,55,45,.12);transform:rotate(8deg);padding:10px;color:#6d7180}.tornPhoto b{position:absolute;bottom:8px;left:9px;font-size:9px;letter-spacing:.15em}.tornPhoto span{font-size:29px;position:absolute;right:10px;top:9px}.tearLine{position:absolute;right:98px;bottom:-12px;width:18px;height:120px;background:#efe4da;transform:rotate(23deg);border-radius:50%}
        .tinyDrop{position:absolute;right:119px;bottom:30px;font-size:29px;color:rgba(125,102,98,.36)}
        .heroPhoto{position:absolute;width:92px;height:74px;border:6px solid rgba(255,249,241,.85);background:#9ab7d1;box-shadow:0 8px 15px rgba(78,52,54,.12);display:grid;place-items:center;font-size:27px;color:#fff;bottom:8px}.heroPhoto.hp1{right:32px;transform:rotate(11deg);background:#9ebed3}.heroPhoto.hp2{right:83px;bottom:23px;transform:rotate(-3deg);background:#8ba5b7}.heroPhoto.hp3{right:128px;bottom:8px;transform:rotate(-14deg);background:#d19ba7}.heroCard .artLeaf.l1{right:178px}.heroCard .artLeaf.l2{right:150px}.artNote{position:absolute;right:21px;top:17px;color:rgba(117,76,91,.62);font-family:"Playfair Display",serif;font-size:11px;line-height:1.3;transform:rotate(4deg)}
        .compassArt{position:absolute;right:34px;bottom:2px;width:91px;height:91px;border-radius:50%;display:grid;place-items:center;background:rgba(255,255,255,.33);color:#687a91;box-shadow:inset 0 0 0 5px rgba(255,255,255,.15)}.adaptiveCard .artLeaf.l1{right:110px}.adaptiveCard .artLeaf.l2{right:77px}.adaptiveCard .artNote{right:19px;top:auto;bottom:27px;}
        .curieArt{position:absolute;right:34px;bottom:-6px;width:120px;height:103px}.curieFace{position:absolute;left:20px;top:28px;width:82px;height:69px;border-radius:50% 50% 45% 45%;background:#f4e8d8;box-shadow:0 8px 15px rgba(62,77,61,.13);}.curieFace i{position:absolute;top:32px;width:7px;height:7px;border-radius:50%;background:#4a3734}.curieFace i:first-child{left:23px}.curieFace i:nth-child(2){right:23px}.curieFace b{position:absolute;left:38px;top:32px;color:#b87875;font-size:15px}.curieEar{position:absolute;width:39px;height:44px;background:#e8dac9;border-radius:9px 27px 8px 22px;top:9px}.curieEar.e1{left:18px;transform:rotate(-22deg)}.curieEar.e2{right:17px;transform:scaleX(-1) rotate(-22deg)}.curieCard .artLeaf.l1{right:140px}.curieCard .artLeaf.l2{right:119px}.curieCard .artNote{top:13px;right:18px}
        .darkMode .appearanceCard { color:#f6eee9; border-color:rgba(255,255,255,.07); box-shadow:0 15px 34px rgba(0,0,0,.24),inset 0 1px rgba(255,255,255,.06); }
        .darkMode .themeCard { background:linear-gradient(135deg,#26303a,#293947 55%,#30434e); }.darkMode .tintCard{background:linear-gradient(135deg,#603b48,#6a3f4c 55%,#583541)}.darkMode .pastelCard{background:linear-gradient(135deg,#433b5c,#4a4064 55%,#3e3757)}.darkMode .materialCard{background:linear-gradient(135deg,#2f4a57,#345666 55%,#2d4b58)}.darkMode .tearsCard{background:linear-gradient(135deg,#4b403b,#56483f 55%,#443a36)}.darkMode .heroCard{background:linear-gradient(135deg,#5d3c4b,#68404f 55%,#563745)}.darkMode .adaptiveCard{background:linear-gradient(135deg,#38445f,#414c69 55%,#343f5a)}.darkMode .curieCard{background:linear-gradient(135deg,#334a3a,#3a5140 55%,#304438)}
        .darkMode .appearanceCard p { color:rgba(246,238,233,.72); }.darkMode .miniIcon{background:rgba(255,255,255,.11)}.darkMode .segmented{background:rgba(0,0,0,.12);border-color:rgba(255,255,255,.13)}.darkMode .segmented button{color:#eee4de;border-color:rgba(255,255,255,.12)}.darkMode .segmented button.selected{background:#4b4031}.darkMode .handNote,.darkMode .artNote{color:#d5b8aa}.darkMode .bottomNote,.darkMode .smallCaption{color:rgba(246,238,233,.56)}

        @media (max-width: 900px) {
          .layout { grid-template-columns:1fr; width:min(760px,calc(100% - 30px)); padding-top:22px; }
          .sideNav {
            position:static; flex-direction:row; overflow-x:auto; justify-content:flex-start;
            padding:2px 0 8px; gap:7px; scrollbar-width:none;
          }
          .sideNav::-webkit-scrollbar { display:none; }
          .navItem { min-width:88px; min-height:67px; width:88px; border-radius:18px; }
          .navLabel { font-size:10px; }
          .navIcon svg { width:19px;height:19px; }
          .pageIntro h1 { font-size:50px; }
          .settingPageHeader { min-height:170px; }
          .appearanceGrid { gap:13px; }
          .appearanceCard { min-height:205px; }
          .themeCard, .heroCard { grid-column:1/-1; }
        }

        @media (max-width: 620px) {
          .settingPageHeader { gap:11px; margin-bottom:27px; }
          .pageBack { width:44px;height:44px;border-radius:15px; }
          .settingPageHeader h1 { font-size:39px; }
          .settingPageHeader p { font-size:12px;line-height:1.4;max-width:300px; }
          .appearanceControls { padding:13px 18px 8px; border-radius:23px; }
          .controlBlock { padding:11px 0 14px; }
          .controlBlock h3, .toggleCopy h3 { font-size:16px; }
          .segmented { min-height:60px;margin-top:11px; }
          .segmented button { font-size:13px; }
          .toggleRow { min-height:82px;padding:16px 0;gap:10px; }
          .toggleCopy p { font-size:11px;line-height:1.4;padding-right:3px; }
          .switch { width:67px;height:42px;padding:4px; }
          .switch span { width:34px;height:34px; }
          .switch.on span { transform:translateX(25px); }
          .genericControls { padding:15px; border-radius:23px; }
          .genericIntro { padding:10px 5px 19px; }
          .genericIllustration { width:60px;height:60px;border-radius:19px; }
          .genericIntro h2 { font-size:23px; }
          .genericRow { min-height:69px; }
          .pageTip { border-radius:18px;padding:15px; }
          .appearanceGrid { grid-template-columns:1fr; gap:12px; }
          .appearanceCard, .themeCard, .heroCard { grid-column:1; min-height:222px; border-radius:23px; padding:20px; }
          .themeCard { min-height:236px; }
          .themeCard .segmented { margin-top:30px; max-width:none; }
          .themeCard .artWindow { right:15px; width:92px; height:105px; }
          .themeCard .artSun { right:118px; }.themeCard .artMoon { right:88px; }
          .handNote { right:17px; top:25px; font-size:11px; }
          .heroCard { min-height:235px; }
          .heroCard .segmented { width:100%; max-width:none; margin-top:31px; }
          .heroHandNote { top:19px; right:17px; font-size:10px; }
          .paintCard { right:27px; }.pastelBlob.b1{right:9px}.materialStack{right:20px}.tornPhoto{right:12px}.compassArt{right:20px}.curieArt{right:13px}
        }
      `}</style>

      <div className={`settingsShell ${effectiveDark ? "darkMode" : ""}`}>
        <header className="topBar">
          <button className="backButton" aria-label="Go back" onClick={goHome}>
            <Icon name="back" size={24} />
          </button>
          <div className="brandMini">
            <strong>Curio</strong>
            <span>Your little corner of curiosity</span>
          </div>
          <div className="topSpacer" />
          <div className="topStatus">Settings · Personal space</div>
        </header>

        <div className="layout">
          <nav className="sideNav" aria-label="Settings categories">
            {NAV.map(item => (
              <button
                key={item.id}
                className={`navItem ${activeNav === item.id ? "active" : ""}`}
                onClick={() => item.id === "all" ? goHome() : openSetting(item.id)}
              >
                <span className="navIcon"><Icon name={item.icon} size={21} /></span>
                <span className="navLabel">{item.label}</span>
              </button>
            ))}
          </nav>

          <main className="content">
            {opened ? (
              <SettingPage item={opened} onBack={goHome} theme={theme} setTheme={setTheme} />
            ) : (
              <>
                <section className="pageIntro">
                  <div>
                    <div className="eyebrow">Make it feel like you</div>
                    <h1>Settings</h1>
                    <p>Tune Curio your way. Nothing complicated, just the things that matter.</p>
                  </div>
                  <button className="quickButton" onClick={() => setQuery("")}>
                    <Icon name="sparkles" size={18} />
                    <span>Quick actions</span>
                  </button>
                </section>

                <div className="searchRow">
                  <label className="searchBox">
                    <Icon name="search" size={22} />
                    <input
                      value={query}
                      onChange={e => setQuery(e.target.value)}
                      placeholder="Search settings..."
                      aria-label="Search settings"
                    />
                  </label>
                </div>

                {filteredGroups.length === 0 && allSecondary.length === 0 ? (
                  <div className="emptyState">
                    <div style={{fontSize:30, marginBottom:8}}>✦</div>
                    Nothing here yet. Try a different setting name.
                  </div>
                ) : (
                  <>
                    {filteredGroups.map(group => (
                      <section className="group" key={group.group}>
                        <div className="groupHeading">
                          <h2>{group.group}</h2>
                          <span className="groupLink">Explore ›</span>
                        </div>
                        <div className="cardGrid">
                          {group.cards.map(item => (
                            <SettingCard key={item.id} item={item} onOpen={openSetting} />
                          ))}
                        </div>
                      </section>
                    ))}

                    {allSecondary.length > 0 && (
                      <div className="secondaryGrid">
                        {allSecondary.map(item => (
                          <button className="secondaryCard" key={item.id} onClick={() => openSetting(item.id)}>
                            <span className="secondaryIcon"><Icon name={item.icon} size={21} /></span>
                            <span className="secondaryText">
                              <strong>{item.title}</strong>
                              <span>{item.subtitle}</span>
                            </span>
                            <span className="secondaryArrow"><Icon name="arrow" size={17} /></span>
                          </button>
                        ))}
                      </div>
                    )}
                  </>
                )}

                <footer className="footerNote">
                  <div className="tinyStars">✦  ✧  ✦</div>
                  <h3>Same curiosity, new horizons.</h3>
                  <p>Curio is a little better when it feels like yours.</p>
                </footer>
              </>
            )}
          </main>
        </div>

      </div>
    </>
  );
}

export default App;
