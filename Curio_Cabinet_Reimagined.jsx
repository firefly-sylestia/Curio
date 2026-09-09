import React, { useMemo, useState } from "react";

const books = [
  { title: "Animal Farm", author: "George Orwell", tone: "rose", cover: "Animal\nFarm" },
  { title: "Never Let Me Go", author: "Kazuo Ishiguro", tone: "cream", cover: "Never\nLet Me\nGo" },
  { title: "Shogun", author: "James Clavell", tone: "sage", cover: "SHOGUN" },
  { title: "The Elegant Universe", author: "Brian Greene", tone: "blue", cover: "The\nElegant\nUniverse" },
];

const collections = [
  { title: "Favorites", count: 8, icon: "star", tone: "lavender", art: "starArt" },
  { title: "Currently Reading", count: 5, icon: "book", tone: "sage", art: "readingArt" },
  { title: "Want to Read", count: 12, icon: "bookmark", tone: "pink", art: "booksArt" },
  { title: "Completed", count: 20, icon: "check", tone: "mint", art: "mountainArt" },
  { title: "Notes", count: 14, icon: "note", tone: "blush", art: "notesArt" },
  { title: "Personal", count: 6, icon: "person", tone: "peach", art: "roomArt" },
];

const everythingItems = [
  { title: "The Midnight Library", meta: "Matt Haig", type: "Book", icon: "book", art: "midnight" },
  { title: "Notes on a Brighter Day", meta: "Personal note", type: "Note", icon: "note", art: "brighter" },
  { title: "A Moment in Kyoto", meta: "Image", type: "Image", icon: "image", art: "kyoto" },
  { title: "lofi ideas.mp3", meta: "Audio", type: "Audio", icon: "audio", art: "lofi" },
  { title: "Curio Tour", meta: "Video", type: "Video", icon: "video", art: "tour" },
  { title: "Dune", meta: "Frank Herbert", type: "Book", icon: "book", art: "dune" },
  { title: "My thoughts today", meta: "Personal note", type: "Note", icon: "note", art: "thoughts" },
  { title: "Forest.jpg", meta: "Image", type: "Image", icon: "image", art: "forest" },
  { title: "Rain ambience", meta: "Audio", type: "Audio", icon: "audio", art: "rain" },
  { title: "About Time (2013)", meta: "Video", type: "Video", icon: "video", art: "time" },
  { title: "Recipes", meta: "Saved link", type: "Link", icon: "link", art: "recipes" },
  { title: "The Comfort Book", meta: "Matt Haig", type: "Book", icon: "book", art: "comfort" },
];

const filters = [
  ["all", "All", "book"],
  ["Book", "Books", "book"],
  ["Note", "Notes", "note"],
  ["Image", "Images", "image"],
  ["Link", "Links", "link"],
  ["Audio", "Audio", "audio"],
  ["Video", "Videos", "video"],
  ["Other", "Others", "grid"],
];

function Icon({ name, size = 24 }) {
  const common = {
    width: size, height: size, viewBox: "0 0 24 24", fill: "none",
    stroke: "currentColor", strokeWidth: 1.8, strokeLinecap: "round", strokeLinejoin: "round",
    "aria-hidden": true,
  };
  const p = {
    home: <><path d="m4 10 8-6 8 6v9a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1Z"/><path d="M9 20v-6h6v6"/></>,
    sparkles: <><path d="m12 3 1.2 4.8L18 9l-4.8 1.2L12 15l-1.2-4.8L6 9l4.8-1.2L12 3Z"/><path d="m19 15 .7 2.3L22 18l-2.3.7L19 21l-.7-2.3L16 18l2.3-.7L19 15Z"/></>,
    cabinet: <><rect x="4" y="6" width="16" height="14" rx="2"/><path d="M3 6h18V4H3v2ZM8 10h8M9 15h6"/></>,
    search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4 4"/></>,
    trash: <><path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5"/></>,
    arrow: <><path d="M5 12h13"/><path d="m13 6 6 6-6 6"/></>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z"/>,
    book: <><path d="M5 4.5h6a2 2 0 0 1 2 2V20a2 2 0 0 0-2-2H5a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z"/><path d="M19 4.5h-6v13.2a2 2 0 0 1 2-2h4a2 2 0 0 0 2-2V6.5a2 2 0 0 0-2-2Z"/></>,
    bookmark: <path d="M6 4.5A1.5 1.5 0 0 1 7.5 3h9A1.5 1.5 0 0 1 18 4.5V21l-6-3.5L6 21Z"/>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    note: <><path d="M6 3.5h9l4 4V20H6Z"/><path d="M15 3.5V8h4M9 12h6M9 16h5"/></>,
    person: <><circle cx="12" cy="8" r="3"/><path d="M5 20a7 7 0 0 1 14 0"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    more: <><circle cx="6" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="18" cy="12" r="1"/></>,
    filter: <><path d="M4 6h16M7 12h10M10 18h4"/></>,
    sort: <><path d="M7 5v14M4 8l3-3 3 3M17 19V5M14 16l3 3 3-3"/></>,
    image: <><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9" r="1.5"/><path d="m4 17 5-5 3 3 3-4 5 6"/></>,
    link: <><path d="M9.5 14.5 14.5 9.5"/><path d="M7 17a4 4 0 0 1 0-5.7l2.2-2.2a4 4 0 0 1 5.7 0M17 7a4 4 0 0 1 0 5.7l-2.2 2.2a4 4 0 0 1-5.7 0"/></>,
    audio: <><path d="M4 12h3l2.5-6 4 12 2.5-6H20"/></>,
    video: <><rect x="3" y="6" width="14" height="12" rx="2"/><path d="m17 10 4-2v8l-4-2"/></>,
    grid: <><rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><rect x="14" y="14" width="6" height="6" rx="1"/></>,
  };
  return <svg {...common}>{p[name] || p.note}</svg>;
}

function Header() {
  return (
    <header className="hero">
      <div className="heroDoodles">
        <span className="doodle book">▱</span><span className="doodle star">☆</span>
        <span className="doodle leaf">♧</span><span className="doodle rocket">⌁</span>
        <span className="doodle compass">◉</span>
      </div>
      <div className="status"><span>12:21</span><span>Sep 8</span><b>•</b><span className="statusRight">LTE+ ▴▴ 27⌁</span></div>
      <div className="heroActions">
        <button aria-label="Search"><Icon name="search" size={23}/></button>
        <button aria-label="Delete"><Icon name="trash" size={23}/></button>
      </div>
      <div className="heroText">
        <h1>The Cabinet</h1>
        <p>Collections · Everything · your keepsakes</p>
        <small>A home for your stories ♡</small>
      </div>
      <div className="heroBooks">
        <div className="plant">♧</div>
        <div className="shelfBooks"><i/><i/><i/><i/><i/></div>
        <div className="cat">◡ᴥ◡</div>
        <div className="note">Small<br/>things<br/><b>big memories</b><br/>♡</div>
      </div>
    </header>
  );
}

function EverythingCard({ onOpen }) {
  return (
    <button className="everything" onClick={onOpen}>
      <div className="everythingTop">
        <div className="collectionIcon"><Icon name="book" size={25}/></div>
        <div><h2>Everything</h2><p>All your books, notes, media and more</p></div>
        <span className="circleArrow"><Icon name="arrow" size={17}/></span>
      </div>
      <div className="mediaRail">
        <div className="emptyMedia">+</div>
        {books.map((book, i) => (
          <div className={`mediaBook ${book.tone}`} key={i}>
            <div className="bookCover"><span>{book.cover}</span><small>{book.author}</small></div>
          </div>
        ))}
      </div>
      <div className="everythingBottom"><strong>11 items</strong><span>›</span></div>
      <div className="cloudDoodle">All in<br/>one place<br/>♡</div>
    </button>
  );
}

function CollectionCard({ item }) {
  return (
    <button className={`collectionCard ${item.tone}`}>
      <div className="collectionHead"><Icon name={item.icon} size={25}/><Icon name="more" size={21}/></div>
      <h3>{item.title}</h3><p>{item.count} items</p>
      <div className={`collectionArt ${item.art}`}>
        {item.art === "starArt" && <><div className="mountainBack"/><div className="bigStar">★</div></>}
        {item.art === "readingArt" && <><div className="openBook">⌁</div><div className="cup">☕</div><div className="leafArt">♧</div></>}
        {item.art === "booksArt" && <><div className="stackedBooks"/><span className="paperNote">Someday<br/>♡</span></>}
        {item.art === "mountainArt" && <><div className="hills"/><div className="flowers">✿　✿</div></>}
        {item.art === "notesArt" && <><div className="paperStack"/><span className="ideaNote">Ideas<br/>Quotes<br/>Thoughts<br/>♡</span></>}
        {item.art === "roomArt" && <><div className="window"/><div className="plantPot">♧</div></>}
      </div>
    </button>
  );
}

function EverythingScreen({ onBack }) {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("all");
  const [sortOpen, setSortOpen] = useState(false);
  const [sort, setSort] = useState("Recent");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return everythingItems
      .filter(item => filter === "all" || item.type === filter)
      .filter(item => !q || `${item.title} ${item.meta} ${item.type}`.toLowerCase().includes(q))
      .sort((a, b) => sort === "A–Z" ? a.title.localeCompare(b.title) : 0);
  }, [query, filter, sort]);

  return (
    <div className="everythingPage">
      <header className="everythingHeader">
        <button className="pageBack" onClick={onBack}><Icon name="arrow" size={22}/></button>
        <div>
          <span className="eyebrow">THE CABINET · LIBRARY</span>
          <h1>Everything</h1>
          <p>All your books, notes, media and more</p>
        </div>
        <div className="everythingHeaderArt"><span>✦</span><b>♡</b><i>▱</i></div>
      </header>

      <main className="everythingContent">
        <div className="everythingToolbar">
          <label className="everythingSearch">
            <Icon name="search" size={21}/>
            <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search in everything..."/>
          </label>
          <button className="toolButton"><Icon name="filter" size={19}/><span>Filter</span></button>
          <button className={`toolButton ${sortOpen ? "pressed" : ""}`} onClick={() => setSortOpen(!sortOpen)}>
            <Icon name="sort" size={19}/><span>Sort</span>
          </button>
          <div className="viewToggle"><button className="active"><Icon name="grid" size={18}/></button><button>☷</button></div>
          {sortOpen && (
            <div className="sortMenu">
              {["Recent", "A–Z"].map(value => (
                <button key={value} className={sort === value ? "selected" : ""} onClick={() => {setSort(value);setSortOpen(false);}}>
                  {value}{sort === value && " ✓"}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="filterRail">
          {filters.map(([id, label, icon]) => (
            <button key={id} className={filter === id ? "active" : ""} onClick={() => setFilter(id)}>
              <Icon name={icon} size={17}/>{label}
            </button>
          ))}
        </div>

        <section className="everythingSection recentSection">
          <div className="sectionTitle">
            <div><h2>Recent</h2><p>Your latest additions</p></div>
            <button>See all <Icon name="arrow" size={15}/></button>
          </div>
          <div className="recentRail">
            {filtered.slice(0, 5).map(item => <EverythingItem key={item.title} item={item} recent/>)}
          </div>
        </section>

        <section className="everythingSection">
          <div className="sectionTitle">
            <div><h2>All Items</h2><p>A mix of everything you keep</p></div>
            <strong>{filtered.length ? `${filtered.length} shown` : "0 items"}</strong>
          </div>
          {filtered.length ? (
            <div className="everythingGrid">
              {filtered.map(item => <EverythingItem key={item.title} item={item}/>)}
            </div>
          ) : (
            <div className="emptyEverything"><span>✦</span><h3>Nothing found</h3><p>Try another word or choose a different category.</p></div>
          )}
        </section>

        <button className="addSomething">
          <span className="addCircle"><Icon name="plus" size={24}/></span>
          <span><strong>Add something new</strong><small>Save a book, note, image, link or anything...</small></span>
          <em>Curate<br/>your little universe ♡</em>
        </button>
      </main>
    </div>
  );
}

function EverythingItem({ item, recent = false }) {
  return (
    <button className={`everythingItem ${recent ? "recent" : ""}`}>
      <div className={`itemArtwork ${item.art}`}>
        {item.art === "midnight" && <><b>The</b><strong>Midnight<br/>Library</strong><small>Matt Haig</small></>}
        {item.art === "brighter" && <><span>Notes on a<br/>Brighter Day</span><i>☁</i></>}
        {item.art === "kyoto" && <><div className="kyotoRoof"/> <span>京都</span></>}
        {item.art === "lofi" && <><span>LOFI</span><b>▶</b></>}
        {item.art === "tour" && <><span>CURIO</span><small>A SMALL APP<br/>FOR BIG CURIOSITY</small><b>▶</b></>}
        {item.art === "dune" && <><b>DUNE</b><span>FRANK<br/>HERBERT</span></>}
        {item.art === "thoughts" && <><span>My thoughts<br/>today ♡</span></>}
        {item.art === "forest" && <><div className="forestSun"/><div className="forestTrees"/></>}
        {item.art === "rain" && <><span>RAIN</span><b>▶</b></>}
        {item.art === "time" && <><span>ABOUT<br/>TIME</span><small>2013</small></>}
        {item.art === "recipes" && <><Icon name="link" size={42}/><span>Recipes</span></>}
        {item.art === "comfort" && <><span>The Comfort<br/>Book</span><small>Matt Haig</small></>}
        <span className="itemMore">⋮</span>
      </div>
      <div className="itemInfo">
        <strong>{item.title}</strong><small>{item.meta}</small>
        <span className="itemType"><Icon name={item.icon} size={14}/>{item.type}</span>
      </div>
    </button>
  );
}

function BottomNav() {
  return (
    <nav className="bottomNav">
      <button><Icon name="home" size={27}/><span>Home</span></button>
      <button><Icon name="sparkles" size={27}/><span>Explore</span></button>
      <button className="selected"><Icon name="cabinet" size={27}/><strong>Cabinet</strong></button>
    </nav>
  );
}

export default function App() {
  const [showEverything, setShowEverything] = useState(false);

  if (showEverything) return (
    <>
      <style>{styles}</style>
      <div className="app"><EverythingScreen onBack={() => setShowEverything(false)}/><BottomNav/></div>
    </>
  );

  return (
    <>
      <style>{styles}</style>
      <div className="app">
        <Header/>
        <main className="content">
          <EverythingCard onOpen={() => setShowEverything(true)}/>

          <div className="sectionTitle">
            <div><h2>Collections</h2><p>Your personal shelves</p></div>
            <div className="viewControls"><button className="active">✦ Recent</button><button>▦</button><button>☷</button></div>
          </div>

          <div className="collectionGrid">
            {collections.map(item => <CollectionCard key={item.title} item={item}/>)}
          </div>

          <button className="newCollection">
            <span className="newPlus"><Icon name="plus" size={23}/></span>
            <span><strong>New collection</strong><span>Create a new shelf for your stories</span></span>
            <span className="newDoodle">New stories<br/>await ♡</span>
          </button>
        </main>
        <BottomNav/>
      </div>
    </>
  );
}

const styles = `
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:wght@500;600&display=swap');

:root{--ink:#482d2f;--muted:#765b5d;--paper:#faf2e7;--cream:#eee2c8;--brown:#7d4f3c}
*{box-sizing:border-box}
body{margin:0;background:#eee7df;color:var(--ink);font-family:"DM Sans",system-ui,sans-serif}
button{font:inherit;color:inherit;border:0;cursor:pointer}
.app{min-height:100vh;background:radial-gradient(circle at 15% 30%,rgba(255,255,255,.55),transparent 30%),radial-gradient(circle at 90% 70%,rgba(234,208,184,.2),transparent 30%),var(--paper);padding-bottom:125px;overflow:hidden}

.hero{height:306px;position:relative;overflow:hidden;background:radial-gradient(circle at 65% 120%,rgba(255,245,228,.75),transparent 28%),linear-gradient(120deg,#efcdb4,#e8c1a3);border-bottom:1px solid rgba(92,61,49,.16)}
.hero:after{content:"";position:absolute;left:-3%;right:-3%;bottom:-16px;height:30px;background:var(--paper);border-radius:50% 50% 0 0/75% 75% 0 0}
.status{position:absolute;top:17px;left:28px;right:28px;display:flex;gap:11px;align-items:center;font-size:14px;color:#593e38;z-index:4}.status b{font-size:15px}.statusRight{margin-left:auto;font-size:12px}
.heroActions{position:absolute;right:30px;top:76px;display:flex;gap:10px;z-index:5}.heroActions button{width:54px;height:54px;border-radius:50%;background:#fbf0e4;display:grid;place-items:center;box-shadow:0 8px 18px rgba(86,53,42,.13)}
.heroText{position:absolute;left:34px;top:167px;z-index:3}.heroText h1{font-family:"Playfair Display",serif;font-size:39px;line-height:1;margin:0 0 6px;letter-spacing:-.03em}.heroText p{margin:0;font-size:15px;color:#684c47}.heroText small{display:block;margin-top:29px;font-family:cursive;font-size:15px;transform:rotate(-3deg);color:#87675d}
.heroDoodles .doodle{position:absolute;color:rgba(125,78,57,.19);font-size:62px;z-index:1;font-family:serif}.doodle.book{left:45px;top:42px;transform:rotate(-12deg)}.doodle.star{left:39px;top:107px;font-size:70px}.doodle.leaf{right:145px;top:137px}.doodle.rocket{right:45px;top:183px;font-size:80px;transform:rotate(-30deg)}.doodle.compass{left:156px;top:70px;font-size:52px}
.heroBooks{position:absolute;right:0;bottom:0;width:48%;height:205px;z-index:2}.shelfBooks{position:absolute;right:100px;bottom:16px;width:205px;height:105px;display:flex;flex-direction:column;justify-content:flex-end;gap:3px}.shelfBooks i{display:block;height:17px;border-radius:4px 8px 3px 3px;background:#80604c;transform:rotate(-2deg);box-shadow:inset 8px 0 rgba(255,255,255,.12)}.shelfBooks i:nth-child(2){width:190px;background:#9a7c68}.shelfBooks i:nth-child(3){width:207px;background:#6f5a52}.shelfBooks i:nth-child(4){width:176px;background:#b08b72}.shelfBooks i:nth-child(5){width:195px;background:#73534a}.cat{position:absolute;right:90px;bottom:43px;font-size:43px;color:#715247;background:#f1e2d2;border-radius:50%;padding:2px 10px;transform:rotate(-3deg)}.plant{position:absolute;right:295px;top:0;font-size:105px;color:#5d775a;opacity:.75}.note{position:absolute;right:8px;top:42px;background:#f5dfbd;padding:9px 10px;font-family:cursive;line-height:1.1;font-size:12px;transform:rotate(3deg);box-shadow:0 4px 8px rgba(90,57,44,.08)}

.content{width:min(1160px,calc(100% - 54px));margin:0 auto;padding-top:55px}
.everything{position:relative;width:100%;display:block;text-align:left;border-radius:28px;background:#e8dcc0;padding:19px 20px 16px;overflow:hidden;box-shadow:0 8px 20px rgba(91,67,51,.06)}
.everything:before{content:"";position:absolute;inset:0;background:radial-gradient(circle at 78% 65%,rgba(255,255,255,.38),transparent 26%),linear-gradient(90deg,transparent,rgba(255,255,255,.12));pointer-events:none}
.everythingTop{display:flex;align-items:center;gap:12px;position:relative;z-index:2}.collectionIcon{width:44px;height:44px;border-radius:15px;background:rgba(255,255,255,.36);display:grid;place-items:center}.everything h2{margin:0;font-family:"Playfair Display",serif;font-size:24px}.everything p{margin:3px 0 0;font-size:12px;color:var(--muted)}.circleArrow{margin-left:auto;width:39px;height:39px;border-radius:50%;background:rgba(112,75,61,.18);display:grid;place-items:center}
.mediaRail{display:grid;grid-template-columns:repeat(5,1fr);gap:9px;margin-top:16px;position:relative;z-index:2}.mediaBook,.emptyMedia{height:122px;border-radius:13px;background:linear-gradient(135deg,#d2c3b3,#b29d8d);display:grid;place-items:center;overflow:hidden}.emptyMedia{background:rgba(255,255,255,.32);font-size:34px;color:#aa8d78}.bookCover{height:104px;width:66px;background:#1e1a18;color:white;padding:8px 4px;text-align:center;box-shadow:0 6px 12px rgba(50,35,30,.18);display:flex;flex-direction:column;justify-content:space-between}.bookCover span{font-family:Georgia,serif;font-size:13px;line-height:.95}.bookCover small{font-size:7px;opacity:.75}.mediaBook.rose .bookCover{background:linear-gradient(#222,#8c4662)}.mediaBook.cream .bookCover{background:#f5eee1;color:#4a3c38}.mediaBook.sage .bookCover{background:#cad39d;color:#6d3024}.mediaBook.blue .bookCover{background:linear-gradient(#193f71,#101f40)}
.everythingBottom{position:relative;z-index:2;display:flex;align-items:center;margin-top:10px;padding:0 3px}.everythingBottom strong{font-family:"Playfair Display",serif;font-size:17px}.everythingBottom span{margin-left:auto;font-size:26px;color:#81635a}.cloudDoodle{position:absolute;right:88px;top:48px;font-family:cursive;color:rgba(116,84,72,.55);font-size:14px;transform:rotate(3deg);line-height:1.1}

.sectionTitle{display:flex;align-items:flex-end;justify-content:space-between;margin:29px 0 16px}.sectionTitle h2{margin:0;font-family:"Playfair Display",serif;font-size:28px}.sectionTitle p{margin:2px 0 0;color:#876d69;font-size:13px}.viewControls{display:flex;gap:8px}.viewControls button{padding:9px 13px;border-radius:18px;background:transparent;border:1px solid rgba(98,68,57,.11)}.viewControls button.active{background:#805740;color:white;border-color:#805740}
.collectionGrid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.collectionCard{min-height:248px;position:relative;overflow:hidden;border-radius:25px;padding:20px;text-align:left;background:#eee;box-shadow:0 9px 25px rgba(80,61,52,.07);transition:transform .18s ease,box-shadow .18s ease}.collectionCard:hover{transform:translateY(-3px);box-shadow:0 15px 30px rgba(80,61,52,.12)}.collectionCard:before{content:"";position:absolute;width:150px;height:120px;border-radius:50%;right:-55px;top:-45px;background:rgba(255,255,255,.25)}.collectionCard.lavender{background:#d8d1ee}.collectionCard.sage{background:#d0dfc7}.collectionCard.pink{background:#f1c6ca}.collectionCard.mint{background:#cfe4d5}.collectionCard.blush{background:#efd7d4}.collectionCard.peach{background:#ecd5b8}.collectionHead{display:flex;justify-content:space-between;position:relative;z-index:3}.collectionHead svg:last-child{opacity:.75}.collectionCard h3{font-size:18px;margin:22px 0 4px;position:relative;z-index:3}.collectionCard p{font-size:12px;color:#725b5e;margin:0;position:relative;z-index:3}.collectionArt{position:absolute;inset:80px 0 0;overflow:hidden}.mountainBack{position:absolute;left:-5%;right:20%;bottom:-10px;height:105px;background:#9992d2;clip-path:polygon(0 100%,25% 43%,38% 68%,57% 25%,100% 100%)}.bigStar{position:absolute;right:13px;bottom:-8px;font-size:82px;color:#f4c768;text-shadow:0 4px 8px rgba(91,68,72,.12)}.openBook{position:absolute;left:28px;bottom:15px;width:140px;height:60px;background:#f7eee1;border-radius:6px 30px 7px 30px;box-shadow:0 5px 8px rgba(74,57,49,.12);font-size:50px;text-align:center;color:#c1a99c}.cup{position:absolute;right:35px;bottom:18px;font-size:38px}.leafArt{position:absolute;left:0;bottom:5px;font-size:65px;color:#769070;opacity:.7}.stackedBooks{position:absolute;left:30px;bottom:4px;width:180px;height:60px;background:#9c7562;border-radius:5px;box-shadow:0 -17px 0 #b98d79,0 -34px 0 #d39f91}.paperNote{position:absolute;right:22px;bottom:17px;background:#f8ead6;padding:11px 10px;font-family:cursive;font-size:12px;transform:rotate(7deg);box-shadow:0 5px 10px rgba(70,48,41,.1)}.hills{position:absolute;inset:38px 0 0;background:linear-gradient(#a7c7bd,#75917c);clip-path:polygon(0 58%,15% 32%,31% 51%,47% 20%,63% 47%,79% 29%,100% 53%,100% 100%,0 100%)}.flowers{position:absolute;bottom:7px;left:28px;color:white;font-size:23px}.paperStack{position:absolute;left:34px;bottom:6px;width:135px;height:65px;background:#b7a9d7;transform:rotate(-9deg);box-shadow:25px 9px 0 #d3c6e7,48px 16px 0 #f0e1d5}.ideaNote{position:absolute;right:18px;bottom:18px;background:#f8ead9;padding:9px;font-family:cursive;font-size:13px;transform:rotate(4deg)}.window{position:absolute;right:18px;top:8px;width:150px;height:130px;background:linear-gradient(135deg,#f3cfa8,#a9b9a2);border:9px solid rgba(103,73,57,.35);box-shadow:inset 0 0 0 4px rgba(255,255,255,.25)}.window:before{content:"";position:absolute;left:48%;top:0;bottom:0;border-left:5px solid rgba(103,73,57,.25)}.plantPot{position:absolute;left:35px;bottom:5px;font-size:80px;color:#71896a}
.newCollection{margin-top:17px;min-height:82px;border-radius:24px;background:#f0e7d7;display:flex;align-items:center;gap:16px;padding:16px 24px;color:#ef8fa5;border:1px solid rgba(108,76,62,.06);box-shadow:0 6px 15px rgba(80,60,49,.04);width:100%;text-align:left}.newPlus{width:46px;height:46px;border-radius:50%;background:#ffd1d8;display:grid;place-items:center}.newCollection strong{display:block;font-size:16px}.newCollection span span{font-size:12px;color:#a4777a}.newDoodle{margin-left:auto;font-family:cursive;color:#a88278;transform:rotate(-4deg);text-align:center}

.bottomNav{position:fixed;z-index:30;bottom:22px;left:50%;transform:translateX(-50%);width:min(500px,calc(100% - 60px));height:82px;border-radius:45px;background:rgba(249,240,226,.93);display:grid;grid-template-columns:1fr 1fr 1.7fr;align-items:center;padding:7px 10px;box-shadow:0 15px 30px rgba(69,53,43,.2);backdrop-filter:blur(15px)}.bottomNav button{height:66px;border-radius:38px;background:transparent;display:flex;align-items:center;justify-content:center;gap:9px;color:#76565a}.bottomNav button span{font-size:13px}.bottomNav .selected{background:#efc8aa;color:#68432f;font-weight:700}.bottomNav .selected strong{font-size:16px}

/* Everything library */
.everythingPage{min-height:100vh;background:var(--paper);padding-bottom:115px}.everythingHeader{height:260px;position:relative;overflow:hidden;padding:72px max(28px,calc((100% - 1160px)/2)) 35px;background:linear-gradient(120deg,#eac4aa,#e9cdb6)}.everythingHeader:after{content:"";position:absolute;left:-4%;right:-4%;bottom:-17px;height:34px;background:var(--paper);border-radius:50% 50% 0 0/80% 80% 0 0}.pageBack{position:absolute;left:28px;top:45px;width:48px;height:48px;border-radius:50%;background:#fff5e9;display:grid;place-items:center;transform:rotate(180deg);box-shadow:0 7px 17px rgba(82,55,43,.12)}.eyebrow{font-size:10px;letter-spacing:.16em;color:#916d61;font-weight:700}.everythingHeader h1{font-family:"Playfair Display",serif;font-size:56px;line-height:.95;margin:12px 0 8px;letter-spacing:-.035em}.everythingHeader p{margin:0;font-size:15px;color:#765b55}.everythingHeaderArt{position:absolute;right:10%;bottom:25px;width:210px;height:150px;color:rgba(106,75,59,.28)}.everythingHeaderArt span{position:absolute;right:45px;top:5px;font-size:42px}.everythingHeaderArt b{position:absolute;right:15px;top:75px;font-size:36px}.everythingHeaderArt i{position:absolute;left:20px;bottom:0;font-size:110px;font-style:normal;transform:rotate(-8deg)}

.everythingContent{width:min(1160px,calc(100% - 54px));margin:0 auto;padding-top:34px}.everythingToolbar{display:flex;gap:10px;position:relative}.everythingSearch{height:58px;flex:1;display:flex;align-items:center;gap:12px;padding:0 18px;border:1px solid rgba(94,66,56,.12);background:#fffaf3;border-radius:20px;color:#816b68}.everythingSearch input{border:0;outline:0;background:transparent;width:100%;font-size:15px;color:var(--ink)}.everythingSearch input::placeholder{color:#a28c87}.toolButton,.viewToggle{height:58px;border:1px solid rgba(94,66,56,.11);background:#fffaf3;border-radius:19px;display:flex;align-items:center;justify-content:center;gap:8px;padding:0 16px}.toolButton.pressed{background:#ead9c8}.viewToggle{padding:5px;gap:3px}.viewToggle button{height:46px;width:45px;border-radius:15px;background:transparent}.viewToggle button.active{background:#815743;color:#fff8ee}.sortMenu{position:absolute;right:83px;top:66px;z-index:15;background:#fffaf3;border:1px solid var(--line);border-radius:16px;padding:6px;box-shadow:0 12px 25px rgba(68,49,43,.13)}.sortMenu button{display:block;width:100%;text-align:left;background:transparent;padding:9px 15px;border-radius:11px}.sortMenu button.selected{background:#ead9c8}.filterRail{display:flex;gap:8px;overflow:auto;padding:15px 0 4px;scrollbar-width:none}.filterRail::-webkit-scrollbar{display:none}.filterRail button{flex:0 0 auto;display:flex;align-items:center;gap:7px;padding:10px 15px;border-radius:999px;border:1px solid rgba(94,66,56,.1);background:#fffaf3;color:#765b5d;font-size:12px}.filterRail button.active{background:#815743;color:white;border-color:#815743}.everythingSection{margin-top:31px}.recentSection{margin-top:26px}.everythingSection>.sectionTitle{margin:0 0 15px}.everythingSection>.sectionTitle button{display:flex;align-items:center;gap:5px;background:transparent;color:#765b5d}.everythingSection>.sectionTitle>strong{font-size:12px;color:#8b7370;font-weight:500}.recentRail{display:grid;grid-template-columns:repeat(5,1fr);gap:14px}.everythingGrid{display:grid;grid-template-columns:repeat(6,1fr);gap:15px}.everythingItem{min-width:0;background:#fffaf3;border-radius:17px;overflow:hidden;padding:0;text-align:left;border:1px solid rgba(89,62,53,.1);box-shadow:0 6px 15px rgba(80,57,49,.07);transition:.18s ease}.everythingItem:hover{transform:translateY(-3px);box-shadow:0 12px 24px rgba(80,57,49,.12)}.everythingItem.recent{border-radius:18px}.itemArtwork{height:145px;position:relative;overflow:hidden;display:flex;flex-direction:column;justify-content:center;align-items:center;text-align:center;padding:15px;color:#fff;background:#778}.recent .itemArtwork{height:155px}.itemArtwork:after{content:"";position:absolute;inset:0;background:linear-gradient(180deg,transparent 55%,rgba(40,29,28,.13));pointer-events:none}.itemMore{position:absolute!important;right:9px;top:7px;z-index:3;font-size:20px;line-height:1}.itemArtwork.midnight{background:linear-gradient(145deg,#122e4a,#193d61 58%,#e6b36a)}.midnight b{font-size:8px;letter-spacing:.18em}.midnight strong{font-family:Georgia,serif;font-size:22px;line-height:.95;margin:8px 0}.midnight small{font-size:8px}.itemArtwork.brighter{background:linear-gradient(145deg,#f1c8b0,#d9c7aa 55%,#a7b8c3);color:#594443}.brighter span{font-family:cursive;font-size:17px}.brighter i{position:absolute;right:20px;bottom:12px;font-size:35px;opacity:.35}.itemArtwork.kyoto{background:linear-gradient(#9eb6bd,#e5c7a5);color:#6b453c}.kyotoRoof{position:absolute;bottom:0;width:145%;height:72px;background:#87604f;clip-path:polygon(0 45%,20% 15%,50% 55%,75% 22%,100% 50%,100% 100%,0 100%)}.kyoto span{font-size:28px;z-index:2}.itemArtwork.lofi{background:linear-gradient(150deg,#3d427d,#211f50);font-size:12px;letter-spacing:.25em}.lofi b,.rain b,.tour b{margin-top:15px;width:42px;height:42px;border-radius:50%;display:grid;place-items:center;background:rgba(255,255,255,.22);font-size:13px}.itemArtwork.tour{background:linear-gradient(150deg,#17191e,#3c4b5e)}.tour span{font-family:Georgia,serif;font-size:20px}.tour small{font-size:6px;letter-spacing:.13em;margin-top:7px}.itemArtwork.dune{background:linear-gradient(145deg,#f0a03e,#8c4f27);color:#fff3d6;align-items:flex-start}.dune b{font-size:18px;letter-spacing:.15em}.dune span{font-size:8px;letter-spacing:.12em;margin-top:12px}.itemArtwork.thoughts{background:linear-gradient(145deg,#f1d7cc,#d8c4b8);color:#735654;font-family:cursive;font-size:18px}.itemArtwork.forest{background:linear-gradient(#c1d8de 0 45%,#9eb69e 46% 100%)}.forestSun{width:36px;height:36px;border-radius:50%;background:#f4d5a0;position:absolute;top:27px}.forestTrees{position:absolute;bottom:0;width:100%;height:65px;background:#5d7662;clip-path:polygon(0 100%,10% 48%,19% 75%,29% 25%,38% 70%,48% 35%,60% 75%,70% 22%,81% 67%,90% 37%,100% 70%,100% 100%)}.itemArtwork.rain{background:linear-gradient(150deg,#66689a,#292c57);font-size:15px;letter-spacing:.25em}.itemArtwork.time{background:linear-gradient(150deg,#3d5368,#172431);font-family:Georgia,serif}.time span{font-size:19px}.time small{margin-top:7px}.itemArtwork.recipes{background:linear-gradient(145deg,#f0e4d8,#d6d0c9);color:#8c746c;gap:10px}.recipes span{font-family:cursive;font-size:17px}.itemArtwork.comfort{background:linear-gradient(145deg,#24527d,#152e5d);align-items:flex-start;font-family:Georgia,serif}.comfort span{font-size:17px}.comfort small{font-family:DM Sans;font-size:8px;margin-top:12px}.itemInfo{padding:11px 12px 12px}.itemInfo strong{display:block;font-size:12px;line-height:1.2;min-height:29px}.itemInfo>small{display:block;font-size:10px;color:#896f6e;margin-top:3px;min-height:15px}.itemType{display:flex;align-items:center;gap:5px;color:#8b6d68;font-size:10px;margin-top:8px}.addSomething{width:100%;min-height:86px;margin-top:26px;border-radius:24px;background:#f7e9dd;border:1px dashed rgba(185,111,108,.28);display:flex;align-items:center;gap:14px;padding:15px 20px;text-align:left;color:#c76f78}.addCircle{width:48px;height:48px;border-radius:50%;background:#ffd0d2;display:grid;place-items:center;flex:0 0 auto}.addSomething strong{display:block;font-size:15px}.addSomething small{display:block;color:#a37b79;font-size:11px;margin-top:3px}.addSomething em{margin-left:auto;font-family:cursive;color:#a88379;text-align:center;transform:rotate(-3deg);font-style:normal;font-size:13px}.emptyEverything{text-align:center;padding:60px 20px;background:#f5eadf;border-radius:23px}.emptyEverything span{font-size:25px}.emptyEverything h3{font-family:"Playfair Display",serif;margin:8px 0 3px}.emptyEverything p{margin:0;color:#896f6e;font-size:12px}

@media(max-width:900px){
.content,.everythingContent{width:calc(100% - 30px)}.collectionGrid{grid-template-columns:repeat(2,1fr)}.heroBooks{width:43%;opacity:.8}.shelfBooks{right:35px}.cat{right:18px}.plant{right:190px}.heroText{top:155px}.heroText h1{font-size:35px}.everythingGrid{grid-template-columns:repeat(4,1fr)}.recentRail{grid-template-columns:repeat(5,190px);overflow:auto;padding-bottom:4px}
}
@media(max-width:580px){
.app{padding-bottom:108px}.hero{height:306px}.heroText{left:28px;top:166px}.heroText h1{font-size:35px}.heroText p{font-size:13px}.heroText small{margin-top:25px}.heroBooks{display:none}.heroActions{right:22px;top:82px}.heroActions button{width:51px;height:51px}.doodle.leaf{right:70px}.doodle.rocket{right:5px}.doodle.compass{left:165px}
.content{padding-top:34px}.everything{border-radius:25px;padding:16px 15px}.mediaRail{grid-template-columns:repeat(4,1fr);gap:7px}.mediaBook:nth-of-type(5){display:none}.mediaBook,.emptyMedia{height:88px;border-radius:11px}.bookCover{height:78px;width:45px}.bookCover span{font-size:9px}.cloudDoodle{display:none}.sectionTitle{margin-top:25px}.sectionTitle h2{font-size:25px}.viewControls button{padding:7px 9px}.collectionGrid{grid-template-columns:1fr 1fr;gap:10px}.collectionCard{min-height:194px;border-radius:20px;padding:15px}.collectionCard h3{font-size:15px;margin-top:15px}.collectionCard p{font-size:11px}.collectionArt{inset:72px 0 0}.newCollection{min-height:72px;border-radius:20px;padding:12px 15px}.newDoodle{display:none}.bottomNav{width:calc(100% - 36px);height:72px;bottom:14px}.bottomNav button{height:58px}.bottomNav .selected strong{font-size:14px}

/* mobile Everything */
.everythingHeader{height:248px;padding:75px 22px 30px}.pageBack{left:20px;top:24px;width:45px;height:45px}.everythingHeader h1{font-size:43px}.everythingHeader p{font-size:13px}.everythingHeaderArt{right:-25px;bottom:20px;opacity:.8;transform:scale(.72)}.everythingContent{width:calc(100% - 24px);padding-top:27px}.everythingToolbar{display:grid;grid-template-columns:1fr auto auto;gap:7px}.everythingSearch{grid-column:1/-1;height:53px;border-radius:18px}.toolButton{height:46px;padding:0 13px;border-radius:16px}.toolButton span{font-size:11px}.viewToggle{height:46px}.filterRail{margin:0 -2px}.filterRail button{padding:9px 12px;font-size:11px}.everythingSection{margin-top:25px}.everythingSection>.sectionTitle h2{font-size:24px}.everythingSection>.sectionTitle p{font-size:11px}.recentRail{grid-template-columns:repeat(5,150px);gap:10px;overflow:auto;margin-right:-12px;padding-right:12px}.recent .itemArtwork{height:128px}.everythingGrid{grid-template-columns:repeat(2,1fr);gap:10px}.itemArtwork{height:132px}.itemInfo{padding:9px 10px}.itemInfo strong{font-size:11px}.itemInfo>small{font-size:9px}.itemType{font-size:9px}.addSomething{min-height:74px;border-radius:20px;padding:11px 13px}.addCircle{width:42px;height:42px}.addSomething strong{font-size:13px}.addSomething small{font-size:9px}.addSomething em{display:none}
}

@media(min-width:581px) and (max-width:1100px){
.everythingGrid{grid-template-columns:repeat(4,1fr)}
}
`;

