import React, { useMemo, useState } from "react";
import { createRoot } from "react-dom/client";

/*
  Curio / Everything — dynamic gallery preview
  - Drag cards to reorder them.
  - Filter chips smoothly reflow the gallery.
  - No fixed grid cells: cards keep their own aspect/size.
  - Pure React + CSS, no external UI libraries required.
*/

const INITIAL_ITEMS = [
  { id: "1975", title: "I Like It When You Sleep...", meta: "The 1975 · Album", type: "Albums", size: "wide", tone: "rose", mark: "01" },
  { id: "brief", title: "A Brief Inquiry Into Online Relationships", meta: "The 1975 · Album", type: "Albums", size: "tall", tone: "cream", mark: "02" },
  { id: "shogun", title: "Shogun", meta: "James Clavell · Book", type: "Books", size: "tall", tone: "sage", mark: "03" },
  { id: "never", title: "Never Let Me Go", meta: "Kazuo Ishiguro · Book", type: "Books", size: "medium", tone: "lavender", mark: "04" },
  { id: "elegant", title: "The Elegant Universe", meta: "Brian Greene · Book", type: "Books", size: "wide", tone: "blue", mark: "05" },
  { id: "thousand", title: "The Thousand and One Nights", meta: "Elie Wiesel · Book", type: "Books", size: "wide", tone: "brown", mark: "06" },
  { id: "2001", title: "2001", meta: "Dr. Dre · Album", type: "Albums", size: "small", tone: "sage", mark: "07" },
  { id: "public", title: "It Takes a Nation of Millions", meta: "Public Enemy · Album", type: "Albums", size: "medium", tone: "cream", mark: "08" },
  { id: "reputation", title: "reputation", meta: "Taylor Swift · Album", type: "Albums", size: "tall", tone: "taupe", mark: "09" },
  { id: "paranoid", title: "Paranoid", meta: "Black Sabbath · Album", type: "Albums", size: "wide", tone: "plum", mark: "10" },
  { id: "capture1", title: "A quieter mind, a kinder you", meta: "Saved capture", type: "Captures", size: "medium", tone: "sage", mark: "11" },
  { id: "capture2", title: "Collect beautiful moments", meta: "Saved capture", type: "Captures", size: "small", tone: "peach", mark: "12" },
  { id: "capture3", title: "Some places stay with you", meta: "Saved capture", type: "Captures", size: "wide", tone: "blue", mark: "13" },
  { id: "capture4", title: "Music for a softer life", meta: "Saved capture", type: "Captures", size: "medium", tone: "cream", mark: "14" },
];

const tabs = ["All", "Books", "Albums", "Captures"];

function Icon({ name, size = 20, stroke = 1.8 }) {
  const common = {
    width: size, height: size, viewBox: "0 0 24 24",
    fill: "none", stroke: "currentColor", strokeWidth: stroke,
    strokeLinecap: "round", strokeLinejoin: "round",
  };
  const paths = {
    home: <><path d="m3 10 9-7 9 7"/><path d="M5 9v11h14V9"/><path d="M9 20v-6h6v6"/></>,
    book: <><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v17H6.5A2.5 2.5 0 0 1 4 17.5z"/><path d="M4 17.5A2.5 2.5 0 0 1 6.5 15H20"/><path d="M8 7h7"/></>,
    disc: <><circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="2"/><path d="m15.5 8.5.01.01"/></>,
    image: <><rect x="3.5" y="4" width="17" height="16" rx="2"/><circle cx="8.5" cy="9" r="1.2"/><path d="m5.5 17 4.5-4 3 2.5 2-2 3.5 3.5"/></>,
    bookmark: <path d="M6 4.5A2.5 2.5 0 0 1 8.5 2h7A2.5 2.5 0 0 1 18 4.5V21l-6-3-6 3z"/>,
    search: <><circle cx="10.8" cy="10.8" r="6.5"/><path d="m16 16 4.5 4.5"/></>,
    sliders: <><path d="M4 6h16"/><path d="M4 12h16"/><path d="M4 18h16"/><circle cx="9" cy="6" r="2"/><circle cx="15" cy="12" r="2"/><circle cx="11" cy="18" r="2"/></>,
    settings: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.7 1.7-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.02 1.56V20h-2.4v-.2a1.7 1.7 0 0 0-1.02-1.56 1.7 1.7 0 0 0-1.88.34l-.06.06-1.7-1.7.06-.06A1.7 1.7 0 0 0 8.4 15a1.7 1.7 0 0 0-1.56-1.02h-.2v-2.4h.2A1.7 1.7 0 0 0 8.4 10a1.7 1.7 0 0 0-.34-1.88L8 8.06l1.7-1.7.06.06A1.7 1.7 0 0 0 11.64 6a1.7 1.7 0 0 0 1.02-1.56v-.2h2.4v.2A1.7 1.7 0 0 0 16.08 6a1.7 1.7 0 0 0 1.88.34l.06-.06 1.7 1.7-.06.06A1.7 1.7 0 0 0 19.4 10a1.7 1.7 0 0 0 1.56 1.02h.2v2.4h-.2A1.7 1.7 0 0 0 19.4 15Z"/></>,
    plus: <><path d="M12 5v14"/><path d="M5 12h14"/></>,
    grip: <><circle cx="8" cy="7" r=".8"/><circle cx="16" cy="7" r=".8"/><circle cx="8" cy="12" r=".8"/><circle cx="16" cy="12" r=".8"/><circle cx="8" cy="17" r=".8"/><circle cx="16" cy="17" r=".8"/></>,
  };
  return <svg {...common}>{paths[name]}</svg>;
}

function Card({ item, index, onDragStart, onDragOver, onDrop, dragging }) {
  return (
    <article
      className={`gallery-card card-${item.size} tone-${item.tone} ${dragging ? "is-dragging" : ""}`}
      draggable
      onDragStart={(e) => onDragStart(e, item.id)}
      onDragOver={(e) => onDragOver(e)}
      onDrop={(e) => onDrop(e, item.id)}
      style={{ "--order": index }}
      title="Drag to rearrange"
    >
      <div className="card-mark">{item.mark}</div>
      <div className="placeholder">
        <Icon name={item.type === "Books" ? "book" : item.type === "Albums" ? "disc" : "image"} size={34} stroke={1.35} />
      </div>
      <div className="card-copy">
        <h3>{item.title}</h3>
        <p>{item.meta}</p>
      </div>
      <button className="drag-handle" aria-label="Drag card">
        <Icon name="grip" size={17} stroke={1.5} />
      </button>
    </article>
  );
}

export default function App() {
  const [active, setActive] = useState("All");
  const [query, setQuery] = useState("");
  const [items, setItems] = useState(INITIAL_ITEMS);
  const [dragged, setDragged] = useState(null);
  const [sort, setSort] = useState("Recent");

  const visible = useMemo(() => {
    let result = items.filter((x) => active === "All" || x.type === active);
    if (query.trim()) {
      const q = query.toLowerCase();
      result = result.filter((x) => `${x.title} ${x.meta}`.toLowerCase().includes(q));
    }
    if (sort === "A–Z") result = [...result].sort((a, b) => a.title.localeCompare(b.title));
    return result;
  }, [items, active, query, sort]);

  function dragStart(e, id) {
    setDragged(id);
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", id);
  }

  function dragOver(e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
  }

  function drop(e, targetId) {
    e.preventDefault();
    const sourceId = dragged || e.dataTransfer.getData("text/plain");
    if (!sourceId || sourceId === targetId) return;

    setItems((current) => {
      const copy = [...current];
      const from = copy.findIndex((x) => x.id === sourceId);
      const to = copy.findIndex((x) => x.id === targetId);
      if (from < 0 || to < 0) return current;
      const [moved] = copy.splice(from, 1);
      copy.splice(to, 0, moved);
      return copy;
    });
    setDragged(null);
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand-mini">C</div>
        <nav>
          <button className="nav-item active"><Icon name="home" /><span>Everything</span></button>
          <button className="nav-item"><Icon name="book" /><span>Books</span></button>
          <button className="nav-item"><Icon name="disc" /><span>Albums</span></button>
          <button className="nav-item"><Icon name="image" /><span>Captures</span></button>
          <button className="nav-item"><Icon name="bookmark" /><span>Saved</span></button>
        </nav>
        <button className="nav-item settings"><Icon name="settings" /><span>Settings</span></button>
      </aside>

      <main>
        <header className="header">
          <div>
            <div className="eyebrow">CABINET / LIBRARY</div>
            <h1>Everything</h1>
            <p>Books · albums · saved captures</p>
          </div>
          <div className="header-actions">
            <label className="search">
              <Icon name="search" size={19} />
              <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search your library..." />
            </label>
            <button className="icon-button"><Icon name="sliders" /></button>
          </div>
        </header>

        <section className="toolbar">
          <div className="tabs">
            {tabs.map((tab) => (
              <button key={tab} className={active === tab ? "tab selected" : "tab"} onClick={() => setActive(tab)}>
                {tab}
              </button>
            ))}
          </div>
          <label className="sort">
            <span>Sort</span>
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option>Recent</option>
              <option>A–Z</option>
            </select>
          </label>
        </section>

        <div className="hint">
          <span><Icon name="grip" size={16} /> Drag anything anywhere</span>
          <span>{visible.length} items</span>
        </div>

        <section className="gallery" aria-live="polite">
          {visible.map((item, index) => (
            <Card
              key={item.id}
              item={item}
              index={index}
              dragging={dragged === item.id}
              onDragStart={dragStart}
              onDragOver={dragOver}
              onDrop={drop}
            />
          ))}
        </section>

        {!visible.length && <div className="empty">Nothing here yet.</div>}
      </main>

      <button className="fab" aria-label="Add item"><Icon name="plus" size={25} /></button>
    </div>
  );
}

const css = `
:root {
  font-family: Georgia, "Times New Roman", serif;
  color: #382825;
  background: #f5eee4;
  font-synthesis: none;
}
* { box-sizing: border-box; }
body { margin: 0; min-width: 320px; background: #f5eee4; }
button, input, select { font: inherit; color: inherit; }
button { border: 0; cursor: pointer; }

.app {
  min-height: 100vh;
  display: flex;
  background:
    radial-gradient(circle at 80% 0%, rgba(224,184,177,.18), transparent 32rem),
    #f5eee4;
}

.sidebar {
  width: 112px;
  flex: 0 0 112px;
  border-right: 1px solid rgba(75,53,48,.1);
  padding: 24px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
}
.brand-mini {
  width: 42px; height: 42px; border-radius: 14px;
  display: grid; place-items: center;
  background: #dfb4b1; font-size: 25px; margin-bottom: 35px;
}
.sidebar nav { width: 100%; display: grid; gap: 8px; }
.nav-item {
  background: transparent; border-radius: 16px; min-height: 64px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 5px; color: #806d67; transition: .2s ease;
}
.nav-item span { font-size: 12px; }
.nav-item.active { background: #ead0cd; color: #3f2927; }
.nav-item:hover { background: rgba(223,180,177,.28); color: #3f2927; }
.settings { margin-top: auto; }

main { width: min(1400px, 100%); margin: 0 auto; padding: 44px 46px 100px; }

.header {
  display: flex; justify-content: space-between; align-items: end; gap: 30px;
}
.eyebrow { font: 600 10px/1.2 Arial, sans-serif; letter-spacing: .18em; color: #987a73; margin-bottom: 8px; }
h1 { margin: 0; font-size: clamp(42px, 5vw, 66px); line-height: .95; font-weight: 500; letter-spacing: -.045em; }
.header p { margin: 10px 0 0; color: #8d7971; font-size: 17px; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.search {
  width: min(360px, 35vw); height: 52px; border-radius: 27px;
  background: rgba(255,250,244,.72); border: 1px solid rgba(80,57,50,.08);
  display: flex; align-items: center; gap: 11px; padding: 0 17px; color: #765e58;
}
.search input { width: 100%; border: 0; outline: 0; background: transparent; font: 15px Arial, sans-serif; }
.search input::placeholder { color: #a6938c; }
.icon-button {
  width: 52px; height: 52px; border-radius: 50%; background: #e7c6c1;
  display: grid; place-items: center;
}
.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin: 38px 0 16px; gap: 20px;
}
.tabs { display: flex; gap: 8px; flex-wrap: wrap; }
.tab {
  padding: 11px 22px; border-radius: 22px; background: #eee3d9; color: #6e5b55;
  transition: .25s ease;
}
.tab.selected { background: #9d716d; color: #fffaf4; }
.tab:hover { transform: translateY(-1px); }

.sort { display: flex; align-items: center; gap: 8px; color: #907c75; font-size: 14px; }
.sort select { border: 0; background: transparent; outline: 0; cursor: pointer; font-size: 15px; }

.hint {
  display: flex; justify-content: space-between; align-items: center;
  color: #a18e86; font: 12px Arial, sans-serif; margin: 0 3px 14px;
}
.hint span:first-child { display: flex; gap: 6px; align-items: center; }

.gallery {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  grid-auto-rows: 74px;
  grid-auto-flow: dense;
  gap: 10px;
  transition: height .35s ease;
}

.gallery-card {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(75,53,48,.08);
  padding: 17px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-width: 0;
  box-shadow: 0 5px 18px rgba(79,56,48,.055);
  cursor: grab;
  transition: transform .25s ease, opacity .25s ease, box-shadow .25s ease;
}
.gallery-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(79,56,48,.10);
}
.gallery-card:active { cursor: grabbing; }
.gallery-card.is-dragging { opacity: .35; transform: scale(.97); }
.card-small { grid-column: span 3; grid-row: span 2; }
.card-medium { grid-column: span 4; grid-row: span 4; }
.card-wide { grid-column: span 6; grid-row: span 3; }
.card-tall { grid-column: span 3; grid-row: span 5; }

.tone-rose { background: #dfb9b4; }
.tone-cream { background: #eee5d7; }
.tone-sage { background: #b9c0ad; }
.tone-lavender { background: #c8c2d4; }
.tone-blue { background: #b7c2d2; }
.tone-brown { background: #80665a; color: #fff7ed; }
.tone-taupe { background: #d3c4b6; }
.tone-plum { background: #8f899e; color: #fff7ed; }
.tone-peach { background: #e5b6ad; }

.placeholder {
  position: absolute; inset: 14px 14px auto auto;
  width: 46px; height: 46px; border-radius: 50%;
  display: grid; place-items: center;
  background: rgba(255,255,255,.22); color: currentColor; opacity: .62;
}
.card-copy { position: relative; z-index: 1; max-width: 86%; }
.card-copy h3 { margin: 0 0 5px; font-size: clamp(17px, 1.55vw, 24px); line-height: 1.06; font-weight: 500; }
.card-copy p { margin: 0; opacity: .67; font: 12px/1.3 Arial, sans-serif; }
.card-mark { position: absolute; top: 17px; left: 17px; font: 10px Arial, sans-serif; opacity: .42; }
.drag-handle {
  position: absolute; right: 12px; bottom: 12px; background: rgba(255,255,255,.22);
  width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center; opacity: .0;
  transition: opacity .2s ease;
}
.gallery-card:hover .drag-handle { opacity: .75; }

.empty {
  padding: 80px 20px; text-align: center; color: #907c75;
  font-size: 20px;
}

.fab {
  position: fixed; right: 28px; bottom: 28px; width: 58px; height: 58px;
  border-radius: 50%; background: #dfb4b1; display: grid; place-items: center;
  box-shadow: 0 12px 30px rgba(76,47,42,.18);
}

@media (max-width: 850px) {
  .sidebar { width: 78px; flex-basis: 78px; }
  .sidebar .nav-item span { display: none; }
  main { padding: 30px 22px 90px; }
  .header { align-items: flex-start; flex-direction: column; }
  .header-actions, .search { width: 100%; }
  .search { flex: 1; }
  .toolbar { align-items: flex-start; flex-direction: column; }
  .sort { align-self: flex-end; }
  .gallery { grid-template-columns: repeat(6, minmax(0, 1fr)); }
  .card-small { grid-column: span 2; }
  .card-medium { grid-column: span 3; }
  .card-wide { grid-column: span 6; }
  .card-tall { grid-column: span 3; }
}

@media (max-width: 560px) {
  .app { display: block; }
  .sidebar {
    position: fixed; z-index: 20; left: 12px; right: 12px; bottom: 12px;
    width: auto; height: 64px; min-height: 0; padding: 6px;
    border: 1px solid rgba(75,53,48,.08); border-radius: 24px;
    background: rgba(250,243,235,.92); backdrop-filter: blur(18px);
    flex-direction: row; justify-content: center; box-shadow: 0 12px 35px rgba(65,43,39,.12);
  }
  .brand-mini, .settings { display: none; }
  .sidebar nav { display: flex; justify-content: space-around; gap: 2px; }
  .nav-item { min-height: 52px; min-width: 55px; border-radius: 17px; }
  .sidebar nav .nav-item:first-child span { display: block; font-size: 9px; }
  .sidebar nav .nav-item span { display: none; }
  main { padding: 28px 15px 105px; }
  h1 { font-size: 48px; }
  .header p { font-size: 15px; }
  .gallery {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    grid-auto-rows: 58px;
    gap: 7px;
  }
  .card-small { grid-column: span 2; grid-row: span 2; }
  .card-medium { grid-column: span 3; grid-row: span 3; }
  .card-wide { grid-column: span 6; grid-row: span 3; }
  .card-tall { grid-column: span 3; grid-row: span 4; }
  .gallery-card { border-radius: 14px; padding: 12px; }
  .card-copy h3 { font-size: 16px; }
  .card-copy p { font-size: 10px; }
  .placeholder { width: 34px; height: 34px; inset: 9px 9px auto auto; }
  .placeholder svg { width: 22px; height: 22px; }
  .card-mark { top: 10px; left: 11px; font-size: 8px; }
  .drag-handle { display: none; }
  .fab { right: 19px; bottom: 91px; width: 52px; height: 52px; }
}
`;

function Style() {
  return <style dangerouslySetInnerHTML={{ __html: css }} />;
}

function Root() {
  return <>
    <Style />
    <App />
  </>;
}

createRoot(document.getElementById("root")).render(<Root />);
