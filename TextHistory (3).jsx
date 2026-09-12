import React, { useMemo, useState } from "react";
import {
  Search, MoreVertical, X, ChevronDown, ChevronUp, Pin, Copy,
  RotateCcw, Trash2, GitBranch, Maximize2, ArrowLeft, Plus,
  Check, GitCompare, Sparkles, Clock3, SlidersHorizontal
} from "lucide-react";


const HISTORY_CSS = '@import url("https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600&family=Playfair+Display:wght@500;600&display=swap");\n\n:root {\n  --bg: #eee5d7;\n  --panel: #f8f3e9;\n  --card: #fbf9f4;\n  --ink: #4b2730;\n  --muted: #8c6f70;\n  --line: #d9c9bd;\n  --rose: #ec9eb0;\n  --rose-soft: #f5d9df;\n  --brown: #754b3d;\n  --brown-soft: #eadbd0;\n  --danger: #9b5d68;\n}\n\n* { box-sizing: border-box; }\nbody { margin: 0; background: var(--bg); color: var(--ink); font-family: "DM Sans", sans-serif; }\nbutton, input { font: inherit; }\nbutton { border: 0; cursor: pointer; color: inherit; }\n\n.app-shell { min-height: 100vh; padding: 28px; display: flex; justify-content: center; }\n.history-panel {\n  width: min(720px, 100%);\n  min-height: calc(100vh - 56px);\n  background: var(--panel);\n  border-radius: 34px;\n  box-shadow: 0 28px 80px rgba(78, 52, 45, .15);\n  padding: 30px 24px 100px;\n  position: relative;\n  overflow: hidden;\n}\n\n.topbar { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; }\n.eyebrow { display:flex; align-items:center; gap:7px; color: var(--muted); font-size: 12px; }\nh1 { font: 600 31px "Playfair Display", serif; margin: 5px 0 2px; letter-spacing: -.5px; }\n.topbar p { margin: 0; color: var(--muted); font-size: 13px; }\n.topbar p span { padding: 0 5px; }\n.top-actions { display:flex; align-items:center; gap:7px; }\n.search-box { width: 210px; height: 40px; display:flex; align-items:center; gap:8px; padding:0 12px; background:#fffaf1; border:1px solid #e7dcd0; border-radius:15px; color:var(--muted); }\n.search-box input { border:0; outline:0; background:transparent; width:100%; color:var(--ink); font-size:13px; }\n.icon-btn { width:31px; height:31px; border-radius:10px; display:grid; place-items:center; background:transparent; color:var(--muted); transition: .2s ease; }\n.icon-btn:hover { background:var(--brown-soft); color:var(--ink); }\n.icon-btn.danger:hover { color: var(--danger); background:#f6e3e5; }\n\n.view-switcher { display:flex; align-items:center; gap:0; margin:25px 0 13px; background:#eadfd3; border-radius:17px; padding:3px; width: 285px; }\n.view-switcher button { flex:1; padding:10px 18px; border-radius:14px; background:transparent; color:var(--muted); font-size:13px; }\n.view-switcher button.active { background:var(--brown); color:#fffaf4; box-shadow: 0 5px 15px rgba(117,75,61,.18); }\n.view-switcher .clear { flex:0 0 auto; margin-left:8px; background:#fffaf2; color:var(--ink); border:1px solid #e6d9cd; }\n\n.filter-row { display:flex; gap:7px; overflow:auto; padding-bottom:5px; }\n.filter-row button { background:#eee3d8; color:var(--muted); border-radius:14px; padding:8px 14px; font-size:12px; white-space:nowrap; }\n.filter-row button.active { background:var(--brown); color:#fffaf4; }\n\n.compare-banner { display:flex; align-items:center; gap:9px; margin:14px 0; padding:11px 13px; border-radius:15px; background:#f5dfe3; color:var(--ink); font-size:12px; }\n.compare-banner span { flex:1; }\n.compare-banner button { background:transparent; text-decoration:underline; font-size:12px; }\n\n.groups { margin-top:18px; }\n.history-group { margin-bottom:17px; }\n.group-heading { width:100%; display:flex; align-items:center; gap:8px; padding:12px 6px; background:transparent; text-align:left; }\n.group-heading > span { font:500 18px "Playfair Display", serif; display:flex; align-items:center; gap:7px; }\n.group-heading > span svg { color:var(--rose); }\n.group-heading small { color:var(--muted); font-size:12px; flex:1; }\n.group-heading > svg { color:var(--muted); }\n\n.group-content { overflow:hidden; }\n.list-stack { display:flex; flex-direction:column; gap:10px; padding:8px 0; }\n.snapshot-card {\n  position:relative; background:var(--card); border:1px solid #eee3d7; border-radius:22px;\n  padding:14px 14px 11px; box-shadow:0 7px 25px rgba(90,65,55,.055); cursor:pointer;\n  transition: border-color .2s, box-shadow .2s;\n}\n.snapshot-card:hover, .snapshot-card.selected { border-color:#e5aeb8; box-shadow:0 11px 32px rgba(113,73,66,.10); }\n.snapshot-card.current { border-color:#e2a2b1; }\n.card-top, .tree-meta, .card-bottom, .tree-footer { display:flex; align-items:center; gap:8px; }\n.card-top .time, .tree-meta > span:first-child { font-size:12px; color:var(--ink); }\n.delta, .tree-delta { color:var(--rose); font-size:12px; }\n.delta.branch, .tree-delta.branch { display:flex; align-items:center; gap:4px; }\n.card-top .icon-btn { margin-left:auto; }\n.card-text, .tree-preview { margin:10px 0 12px; font-size:13px; line-height:1.52; color:#3e2930; }\n.card-bottom { border-top:1px solid var(--line); padding-top:8px; }\n.kind, .tree-footer > span { color:var(--rose); font-size:12px; }\n.inline-actions { display:flex; margin-left:auto; gap:2px; }\n.current-pill { position:absolute; right:13px; bottom:10px; background:#f1dfd7; border-radius:12px; padding:5px 8px; font-size:11px; display:flex; align-items:center; gap:4px; }\n\n.tree { position:relative; padding:7px 0 7px; }\n.tree-row { position:relative; min-height:108px; }\n.tree-row:not(:last-child)::after { content:""; position:absolute; left:24px; top:30px; bottom:-12px; width:1px; background:linear-gradient(var(--rose), var(--line)); }\n.tree-rail { position:absolute; top:0; bottom:0; width:24px; display:flex; justify-content:center; pointer-events:none; }\n.tree-dot { position:absolute; top:19px; width:13px; height:13px; border-radius:50%; background:#c9a6a6; border:3px solid var(--panel); box-shadow:0 0 0 1px #b99392; z-index:2; }\n.tree-dot.selected { background:var(--rose); box-shadow:0 0 0 1px var(--rose), 0 0 0 6px rgba(236,158,176,.15); }\n.tree-dot.current { width:16px; height:16px; top:17px; background:var(--rose); box-shadow:0 0 0 2px var(--panel), 0 0 0 5px rgba(236,158,176,.25); }\n.tree-card { background:var(--card); border:1px solid #eee3d7; border-radius:18px; padding:12px 12px 8px; cursor:pointer; box-shadow:0 5px 20px rgba(90,65,55,.045); }\n.tree-card:hover, .tree-card.selected { border-color:#e5aeb8; }\n.tree-meta { min-height:20px; }\n.tree-meta .tree-more { margin-left:auto; color:var(--muted); display:grid; place-items:center; }\n.tree-footer { border-top:1px solid var(--line); padding-top:7px; }\n\n.add-edit { margin:5px 0 0 34px; background:transparent; color:var(--muted); font-size:12px; display:flex; align-items:center; gap:5px; padding:6px 0; }\n.add-edit:hover { color:var(--ink); }\n\n.fab { position:absolute; right:25px; bottom:25px; width:54px; height:54px; border-radius:19px; background:var(--brown); color:#fff; display:grid; place-items:center; box-shadow:0 14px 30px rgba(117,75,61,.25); transition: transform .2s; }\n.fab:hover { transform:translateY(-2px); }\n\n.detail-panel, .compare-panel {\n  position:fixed; right:28px; top:28px; width:min(430px, calc(100vw - 56px)); height:calc(100vh - 56px);\n  background:#f9f4ea; border-radius:30px; box-shadow:0 28px 80px rgba(70,45,39,.22); padding:25px; overflow:auto; z-index:20;\n}\n.detail-head, .compare-head { display:flex; align-items:center; gap:10px; color:var(--muted); font-size:12px; }\n.detail-head > span { flex:1; }\n.back { background:transparent; color:var(--ink); display:grid; place-items:center; }\n.detail-node-line { display:flex; gap:14px; align-items:flex-start; margin:35px 0 18px; }\n.big-dot { width:18px; height:18px; margin-top:4px; border-radius:50%; background:#b99b99; box-shadow:0 0 0 5px #efe0d8; }\n.big-dot.current { background:var(--rose); box-shadow:0 0 0 5px #f4dce0, 0 0 0 9px rgba(236,158,176,.18); }\n.detail-label { color:var(--rose); font-size:12px; }\n.detail-node-line h2 { font:600 25px "Playfair Display",serif; margin:4px 0 0; }\n.full-text-card { background:#fffaf3; border:1px solid #eee2d5; border-radius:21px; padding:18px; }\n.full-text { font-size:15px; line-height:1.65; }\n.text-link { margin-top:12px; background:transparent; color:var(--brown); display:flex; align-items:center; gap:6px; font-size:12px; }\n.stats { margin-top:14px; color:var(--muted); font-size:11px; }\n.stats span { padding:0 4px; }\n.detail-section { margin-top:25px; }\n.detail-section h3 { display:flex; align-items:center; gap:7px; font-size:12px; margin:0 0 10px; }\n.change-note, .lineage { background:#efe4d9; border-radius:16px; padding:13px; color:var(--muted); font-size:12px; line-height:1.5; }\n.action-grid { display:grid; grid-template-columns:1fr 1fr; gap:8px; }\n.action-grid button { display:flex; align-items:center; justify-content:center; gap:7px; padding:12px; border-radius:15px; background:#eee2d7; color:var(--ink); font-size:12px; }\n.action-grid button:hover { background:#e6d4c7; }\n.action-grid .wide { grid-column:1/-1; }\n\n.compare-panel { width:min(760px, calc(100vw - 56px)); height:auto; max-height:calc(100vh - 56px); left:50%; right:auto; transform:translateX(-50%); top:50%; bottom:auto; margin-top:0; }\n.compare-head { justify-content:space-between; color:var(--ink); font-weight:600; font-size:14px; }\n.compare-head > div { display:flex; align-items:center; gap:7px; }\n.compare-columns { display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-top:18px; }\n.compare-columns > div { background:#fffaf3; border:1px solid #eee2d5; border-radius:18px; padding:15px; }\n.compare-columns span { color:var(--muted); font-size:11px; }\n.compare-columns p { line-height:1.55; font-size:13px; }\n.diff-result { margin-top:12px; background:#efe3d8; border-radius:18px; padding:15px; }\n.diff-row { display:flex; gap:10px; margin:8px 0; font-size:12px; align-items:flex-start; }\n.diff-row > span { width:60px; color:var(--muted); }\nmark { border-radius:6px; padding:3px 5px; }\n.removed { background:#f2d7da; color:#8e5660; }\n.added { background:#dcebe1; color:#4c765b; }\n.close-compare { width:100%; margin-top:12px; padding:12px; border-radius:15px; background:var(--brown); color:#fff; }\n\n.menu-popover { position:fixed; right:44px; top:130px; z-index:40; width:190px; background:#fffaf3; border:1px solid #eaded1; border-radius:17px; box-shadow:0 18px 45px rgba(70,45,39,.16); padding:7px; }\n.menu-title { padding:8px 10px; color:var(--muted); font-size:11px; border-bottom:1px solid #eaded1; margin-bottom:4px; }\n.menu-popover button { width:100%; padding:9px 10px; border-radius:10px; display:flex; align-items:center; gap:9px; background:transparent; font-size:12px; text-align:left; }\n.menu-popover button:hover { background:#eee2d7; }\n.menu-popover svg { width:15px; }\n.menu-popover .danger-row { color:var(--danger); }\n\n.toast { position:fixed; left:50%; bottom:28px; transform:translateX(-50%); z-index:100; background:var(--ink); color:#fff; border-radius:14px; padding:10px 15px; font-size:12px; box-shadow:0 10px 30px rgba(50,30,30,.2); }\n\n@media (max-width: 760px) {\n  .app-shell { padding:0; }\n  .history-panel { min-height:100vh; border-radius:0; padding:22px 16px 100px; }\n  .topbar { flex-direction:column; }\n  .top-actions { width:100%; }\n  .search-box { flex:1; width:auto; }\n  .view-switcher { width:100%; }\n  .detail-panel { inset:0; width:100%; height:100%; border-radius:0; }\n  .compare-panel { left:0; top:0; width:100%; max-height:100vh; transform:none; border-radius:0; }\n  .compare-columns { grid-template-columns:1fr; }\n  .tree-row:not(:last-child)::after { left:20px; }\n  .tree-rail { left:-4px !important; }\n  .tree-card { margin-left:30px !important; }\n}\n\n@keyframes dotPulse { 0%,100% { transform: scale(1); } 50% { transform: scale(1.14); } }\n.tree-dot.current { animation: dotPulse 1.8s ease-in-out infinite; }\n@keyframes fadeInUp { from { opacity:0; transform: translateY(-8px); } to { opacity:1; transform: translateY(0); } }\n@keyframes fadeInGrow { from { opacity:0; transform: translateY(-6px); } to { opacity:1; transform: translateY(0); } }\n@keyframes fadeInScale { from { opacity:0; transform: scale(.96) translateY(5px); } to { opacity:1; transform: scale(1) translateY(0); } }\n@keyframes fadeInRight { from { opacity:0; transform: translateX(40px); } to { opacity:1; transform: translateX(0); } }\n@keyframes fadeInPanel { from { opacity:0; transform: translateY(30px); } to { opacity:1; transform: translateY(0); } }\n@keyframes fadeInPlain { from { opacity:0; } to { opacity:1; } }\n.anim-fade-up { animation: fadeInUp .25s ease; }\n.anim-fade-grow { animation: fadeInGrow .28s cubic-bezier(.22,1,.36,1); }\n.anim-fade-scale { animation: fadeInScale .18s ease; }\n.anim-fade-right { animation: fadeInRight .3s cubic-bezier(.25,1,.4,1); }\n.anim-fade-panel { animation: fadeInPanel .3s cubic-bezier(.25,1,.4,1); }\n.anim-fade { animation: fadeInPlain .2s ease; }\n';
if (typeof document !== "undefined" && !document.getElementById("text-history-styles")) {
  const style = document.createElement("style");
  style.id = "text-history-styles";
  style.textContent = HISTORY_CSS;
  document.head.appendChild(style);
}

/*
  Text History Tree
  -----------------
  A single-file React implementation of the supplied concept.
  Dependencies:
    react
    react-dom
    lucide-react

  The CSS can live in styles.css, or be merged into your app's stylesheet.
*/

const seedGroups = [
  {
    id: "quick-fact",
    title: "Quick fact",
    snapshots: [
      {
        id: "q1",
        time: "8 Sep · 20:37",
        label: "Original",
        delta: "Original",
        text: "The 1975 2016, the 74-minute double album that made them the defining voice of a generation, blending dreamy pop, sharp commentary and emotional chaos.",
        parent: null,
        pinned: false
      },
      {
        id: "q2",
        time: "9 Sep · 10:50",
        label: "+1 edit",
        delta: "+1 edit",
        text: "The cut-up writer of Naked Lunch (1959), the novel that surprised readers with its fragmented structure and strange humour.",
        parent: "q1",
        pinned: false
      },
      {
        id: "q3",
        time: "12h ago",
        label: "+1 edit",
        delta: "+1 edit",
        text: "Margaret Mitchell's 1936 Pulitzer Prize-winning novel follows Scarlett O'Hara through love, loss and a changing South.",
        parent: "q2",
        pinned: false
      },
      {
        id: "q4",
        time: "57m ago",
        label: "+1 edit",
        delta: "+1 edit",
        text: "Emily Brontë's 1847 novel, a love story told through intertwined generations, memory and the wild Yorkshire moors.",
        parent: "q3",
        pinned: false
      },
      {
        id: "q5",
        time: "38m ago",
        label: "+1 edit",
        delta: "+1 edit",
        text: "The 1975 2018, the album that made them critics' darlings, balancing glossy pop production with anxious social commentary.",
        parent: "q4",
        pinned: true
      }
    ]
  },
  {
    id: "chapter-review",
    title: "Chapter review",
    snapshots: [
      {
        id: "c1",
        time: "8 Sep · 16:53",
        label: "+14",
        delta: "+14 words",
        text: "I just finished reading Chapters 6 and 7 of Animal Farm. The revolution's promises feel increasingly betrayed.",
        parent: null,
        pinned: false
      },
      {
        id: "c2",
        time: "12h ago",
        label: "+6",
        delta: "+6 words",
        text: "I just finished reading Chapters 6 and 7 of Animal Farm. The revolution's promises feel increasingly betrayed, especially as equality becomes another slogan.",
        parent: "c1",
        pinned: false
      }
    ]
  },
  {
    id: "ideas",
    title: "Ideas",
    snapshots: [
      {
        id: "i1",
        time: "Yesterday",
        label: "Original",
        delta: "Original",
        text: "A visual archive where every edit remains visible instead of disappearing into a version number.",
        parent: null,
        pinned: false
      },
      {
        id: "i2",
        time: "Yesterday",
        label: "+1",
        delta: "+1 word",
        text: "A visual archive where every edit remains visible instead of disappearing into a boring version number.",
        parent: "i1",
        pinned: false
      },
      {
        id: "i3",
        time: "Yesterday",
        label: "Branch",
        delta: "Restored branch",
        text: "A visual archive where every edit remains visible, with branches for experiments and alternate ideas.",
        parent: "i1",
        branch: true,
        pinned: false
      }
    ]
  }
];

const filters = ["All", "Edits", "Restores", "Pinned"];

function shortText(text, n = 92) {
  return text.length > n ? text.slice(0, n).trimEnd() + "…" : text;
}

function words(text) {
  return text.trim() ? text.trim().split(/\s+/).length : 0;
}

function changeParts(a, b) {
  const A = a.split(/\s+/), B = b.split(/\s+/);
  let start = 0;
  while (start < A.length && start < B.length && A[start] === B[start]) start++;
  let endA = A.length - 1, endB = B.length - 1;
  while (endA >= start && endB >= start && A[endA] === B[endB]) { endA--; endB--; }
  return {
    before: A.slice(start, endA + 1).join(" "),
    after: B.slice(start, endB + 1).join(" ")
  };
}

function IconButton({ title, onClick, children, danger = false }) {
  return (
    <button
      className={`icon-btn ${danger ? "danger" : ""}`}
      title={title}
      aria-label={title}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

function SnapshotCard({
  node, selected, current, onSelect, onPin, onCompare, onRestore, onCopy, onDelete, onMore
}) {
  return (
    <div
      className={`snapshot-card anim-fade-up ${selected ? "selected" : ""} ${current ? "current" : ""}`}
      onClick={() => onSelect(node)}
    >
      <div className="card-top">
        <span className="time">{node.time}</span>
        <span className={`delta ${node.branch ? "branch" : ""}`}>
          {node.branch ? <GitBranch size={12}/> : node.delta}
        </span>
        <IconButton title="More actions" onClick={(e) => { e.stopPropagation(); onMore(node); }}>
          <MoreVertical size={16}/>
        </IconButton>
      </div>

      <div className="card-text">{shortText(node.text)}</div>

      <div className="card-bottom">
        <span className="kind">{node.label}</span>
        <div className="inline-actions">
          <IconButton title={node.pinned ? "Unpin" : "Pin"} onClick={(e) => { e.stopPropagation(); onPin(node); }}>
            <Pin size={15} fill={node.pinned ? "currentColor" : "none"}/>
          </IconButton>
          <IconButton title="Compare" onClick={(e) => { e.stopPropagation(); onCompare(node); }}>
            <GitCompare size={15}/>
          </IconButton>
          <IconButton title="Restore as new branch" onClick={(e) => { e.stopPropagation(); onRestore(node); }}>
            <RotateCcw size={15}/>
          </IconButton>
          <IconButton title="Copy text" onClick={(e) => { e.stopPropagation(); onCopy(node); }}>
            <Copy size={15}/>
          </IconButton>
          <IconButton title="Delete snapshot" danger onClick={(e) => { e.stopPropagation(); onDelete(node); }}>
            <Trash2 size={15}/>
          </IconButton>
        </div>
      </div>

      {current && <div className="current-pill"><Check size={12}/> Current</div>}
    </div>
  );
}

function TreeNode({ node, depth, selectedId, currentId, onSelect, onPin, onCompare, onRestore, onCopy, onDelete, onMore }) {
  return (
    <div className="tree-row anim-fade-grow">
      <div className="tree-rail" style={{ left: `${depth * 22 + 12}px` }}>
        <div
          className={`tree-dot ${selectedId === node.id ? "selected" : ""} ${currentId === node.id ? "current" : ""}`}
        />
      </div>

      <div
        className={`tree-card ${selectedId === node.id ? "selected" : ""} ${currentId === node.id ? "current" : ""}`}
        style={{ marginLeft: `${depth * 22 + 34}px` }}
        onClick={() => onSelect(node)}
      >
        <div className="tree-meta">
          <span>{node.time}</span>
          <span className={`tree-delta ${node.branch ? "branch" : ""}`}>
            {node.branch ? "Restored branch" : node.delta}
          </span>
          <span className="tree-more" onClick={(e) => { e.stopPropagation(); onMore(node); }}>
            <MoreVertical size={15}/>
          </span>
        </div>
        <div className="tree-preview">{shortText(node.text, 105)}</div>
        <div className="tree-footer">
          <span>{node.label}</span>
          <div className="inline-actions">
            <IconButton title={node.pinned ? "Unpin" : "Pin"} onClick={(e) => { e.stopPropagation(); onPin(node); }}>
              <Pin size={14} fill={node.pinned ? "currentColor" : "none"}/>
            </IconButton>
            <IconButton title="Compare" onClick={(e) => { e.stopPropagation(); onCompare(node); }}>
              <GitCompare size={14}/>
            </IconButton>
            <IconButton title="Restore as new branch" onClick={(e) => { e.stopPropagation(); onRestore(node); }}>
              <RotateCcw size={14}/>
            </IconButton>
            <IconButton title="Copy text" onClick={(e) => { e.stopPropagation(); onCopy(node); }}>
              <Copy size={14}/>
            </IconButton>
            <IconButton title="Delete snapshot" danger onClick={(e) => { e.stopPropagation(); onDelete(node); }}>
              <Trash2 size={14}/>
            </IconButton>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function TextHistory() {
  const [groups, setGroups] = useState(seedGroups);
  const [view, setView] = useState("tree");
  const [filter, setFilter] = useState("All");
  const [selected, setSelected] = useState(null);
  const [compareA, setCompareA] = useState(null);
  const [currentId, setCurrentId] = useState("q5");
  const [collapsed, setCollapsed] = useState({});
  const [search, setSearch] = useState("");
  const [toast, setToast] = useState("");
  const [moreNode, setMoreNode] = useState(null);
  const [showFull, setShowFull] = useState(false);

  const allNodes = useMemo(() => groups.flatMap(g => g.snapshots.map(s => ({...s, groupId: g.id, groupTitle: g.title}))), [groups]);

  const filteredGroups = useMemo(() => {
    const q = search.trim().toLowerCase();
    return groups.map(g => ({
      ...g,
      snapshots: g.snapshots.filter(n => {
        const matchesSearch = !q || n.text.toLowerCase().includes(q) || n.time.toLowerCase().includes(q);
        const matchesFilter =
          filter === "All" ||
          (filter === "Pinned" && n.pinned) ||
          (filter === "Restores" && n.branch) ||
          (filter === "Edits" && !n.branch);
        return matchesSearch && matchesFilter;
      })
    })).filter(g => g.snapshots.length);
  }, [groups, filter, search]);

  const selectedNode = selected ? allNodes.find(n => n.id === selected.id) : null;

  function notify(msg) {
    setToast(msg);
    window.clearTimeout(window.__historyToast);
    window.__historyToast = window.setTimeout(() => setToast(""), 1800);
  }

  function pin(node) {
    setGroups(gs => gs.map(g => ({...g, snapshots: g.snapshots.map(n => n.id === node.id ? {...n, pinned: !n.pinned} : n)})));
    notify(node.pinned ? "Snapshot unpinned" : "Snapshot pinned");
  }

  async function copy(node) {
    try { await navigator.clipboard.writeText(node.text); } catch {}
    notify("Text copied");
  }

  function restore(node) {
    const clone = {
      ...node,
      id: `${node.id}-branch-${Date.now()}`,
      time: "Just now",
      label: "Restored branch",
      delta: "Restored branch",
      parent: node.id,
      branch: true,
      pinned: false
    };
    setGroups(gs => gs.map(g => g.id === node.groupId
      ? {...g, snapshots: [...g.snapshots, clone]}
      : g
    ));
    setCurrentId(clone.id);
    setSelected(clone);
    notify("Restored as a new branch");
  }

  function remove(node) {
    if (node.id === currentId) notify("Current snapshot can't be deleted");
    else {
      setGroups(gs => gs.map(g => ({...g, snapshots: g.snapshots.filter(n => n.id !== node.id)})));
      if (selected?.id === node.id) setSelected(null);
      notify("Snapshot deleted");
    }
  }

  function compare(node) {
    if (!compareA) {
      setCompareA(node);
      notify("Select another snapshot to compare");
    } else if (compareA.id === node.id) {
      setCompareA(null);
    } else {
      setSelected(node);
    }
  }

  const compareB = compareA && selected && selected.id !== compareA.id ? selected : null;
  const compareResult = compareB ? changeParts(compareA.text, compareB.text) : null;

  const toggleGroup = id => setCollapsed(c => ({...c, [id]: !c[id]}));

  function addEdit(groupId) {
    const group = groups.find(g => g.id === groupId);
    const last = group.snapshots[group.snapshots.length - 1];
    const clone = {
      ...last,
      id: `${groupId}-${Date.now()}`,
      time: "Just now",
      label: "+1 edit",
      delta: "+1 edit",
      parent: last.id,
      pinned: false,
      text: last.text + " This sentence was refined in the latest edit."
    };
    setGroups(gs => gs.map(g => g.id === groupId ? {...g, snapshots: [...g.snapshots, clone]} : g));
    setCurrentId(clone.id);
    setSelected(clone);
    notify("New edit added");
  }

  return (
    <div className="app-shell">
      <main className="history-panel">
        <header className="topbar">
          <div>
            <div className="eyebrow"><Clock3 size={13}/> Text history</div>
            <h1>Text history</h1>
            <p>{allNodes.length} snapshots <span>·</span> {allNodes.filter(n => n.branch).length || 1} branches</p>
          </div>

          <div className="top-actions">
            <label className="search-box">
              <Search size={18}/>
              <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search history"/>
            </label>
            <IconButton title="Filters"><SlidersHorizontal size={18}/></IconButton>
            <IconButton title="More"><MoreVertical size={19}/></IconButton>
          </div>
        </header>

        <div className="view-switcher">
          <button className={view === "list" ? "active" : ""} onClick={() => setView("list")}>List</button>
          <button className={view === "tree" ? "active" : ""} onClick={() => setView("tree")}>Tree</button>
          <button className="clear" onClick={() => notify("Clear history requires confirmation")}>Clear</button>
        </div>

        <div className="filter-row">
          {filters.map(f => <button key={f} className={filter === f ? "active" : ""} onClick={() => setFilter(f)}>{f}</button>)}
        </div>

        {compareA && (
          <div className="compare-banner anim-fade-up">
            <GitCompare size={16}/>
            <span><b>{compareA.time}</b> selected · choose another snapshot</span>
            <button onClick={() => setCompareA(null)}>Cancel</button>
          </div>
        )}

        <section className="groups">
          {filteredGroups.map(group => (
            <section key={group.id} className="history-group">
              <button className="group-heading" onClick={() => toggleGroup(group.id)}>
                <span><Sparkles size={16}/> {group.title}</span>
                <small>{group.snapshots.length} snapshots{group.snapshots.some(s => s.branch) ? " · branch" : ""}</small>
                {collapsed[group.id] ? <ChevronDown size={17}/> : <ChevronUp size={17}/>}
              </button>

              {!collapsed[group.id] && (
                  <div
                    className={`group-content ${view} anim-fade`}
                  >
                    {view === "list" ? (
                      <div className="list-stack">
                        {group.snapshots.map(node => (
                          <SnapshotCard key={node.id} node={node} selected={selected?.id === node.id}
                            current={currentId === node.id} onSelect={n => setSelected(n)}
                            onPin={pin} onCompare={compare} onRestore={restore} onCopy={copy}
                            onDelete={remove} onMore={setMoreNode}/>
                        ))}
                      </div>
                    ) : (
                      <div className="tree">
                        {group.snapshots.map((node, idx) => (
                          <TreeNode key={node.id} node={node} depth={node.branch ? 1 : 0}
                            selectedId={selected?.id} currentId={currentId}
                            onSelect={n => compareA && compareA.id !== n.id ? setSelected(n) : setSelected(n)}
                            onPin={pin} onCompare={compare} onRestore={restore} onCopy={copy}
                            onDelete={remove} onMore={setMoreNode}/>
                        ))}
                      </div>
                    )}
                    <button className="add-edit" onClick={() => addEdit(group.id)}><Plus size={15}/> Add edit</button>
                  </div>
                )}
            </section>
          ))}
        </section>

        <button className="fab" title="Create new edit" onClick={() => addEdit("quick-fact")}><Plus size={23}/></button>
      </main>

      {selectedNode && !compareB && (
          <aside
            className="detail-panel anim-fade-right"
          >
            <div className="detail-head">
              <button className="back" onClick={() => setSelected(null)}><ArrowLeft size={19}/></button>
              <span>{selectedNode.time}</span>
              <IconButton title="Close" onClick={() => setSelected(null)}><X size={18}/></IconButton>
            </div>

            <div className="detail-node-line">
              <div className={`big-dot ${currentId === selectedNode.id ? "current" : ""}`}/>
              <div>
                <span className="detail-label">{selectedNode.label}</span>
                <h2>{selectedNode.branch ? "Restored version" : selectedNode.label === "Original" ? "Original version" : "Edited version"}</h2>
              </div>
            </div>

            <article className="full-text-card">
              <div className="full-text">{showFull ? selectedNode.text : shortText(selectedNode.text, 230)}</div>
              {selectedNode.text.length > 230 && (
                <button className="text-link" onClick={() => setShowFull(v => !v)}>
                  {showFull ? "Show less" : "View full text"} <Maximize2 size={13}/>
                </button>
              )}
              <div className="stats">{words(selectedNode.text)} words <span>·</span> {selectedNode.text.length} characters</div>
            </article>

            <div className="detail-section">
              <h3>Changes</h3>
              <div className="change-note">
                {selectedNode.parent ? "This snapshot continues from the previous version. The earlier text remains available in the tree." : "This is the original version. All later edits are based on this snapshot."}
              </div>
            </div>

            <div className="detail-section">
              <h3>Actions</h3>
              <div className="action-grid">
                <button onClick={() => pin(selectedNode)}><Pin size={17}/> {selectedNode.pinned ? "Unpin" : "Pin"}</button>
                <button onClick={() => compare(selectedNode)}><GitCompare size={17}/> Compare</button>
                <button onClick={() => restore(selectedNode)}><RotateCcw size={17}/> Restore</button>
                <button onClick={() => copy(selectedNode)}><Copy size={17}/> Copy</button>
                <button className="wide" onClick={() => setShowFull(true)}><Maximize2 size={17}/> View in full screen</button>
              </div>
            </div>

            <div className="detail-section">
              <h3><GitBranch size={16}/> Lineage</h3>
              <div className="lineage">
                {selectedNode.parent ? <span>↳ Based on <b>{selectedNode.parent}</b></span> : <span>● Root snapshot</span>}
                <span>→ Current: <b>{currentId}</b></span>
              </div>
            </div>
          </aside>
        )}

        {compareB && compareResult && (
          <aside className="compare-panel anim-fade-panel">
            <div className="compare-head">
              <div><GitCompare size={18}/> Comparing versions</div>
              <IconButton title="Close comparison" onClick={() => {setCompareA(null); setSelected(null)}}><X size={18}/></IconButton>
            </div>
            <div className="compare-columns">
              <div>
                <span>{compareA.time}</span>
                <p>{compareA.text}</p>
              </div>
              <div>
                <span>{compareB.time}</span>
                <p>{compareB.text}</p>
              </div>
            </div>
            <div className="diff-result">
              <div className="diff-row">
                <span>Removed</span><mark className="removed">{compareResult.before || "No obvious word-level removal"}</mark>
              </div>
              <div className="diff-row">
                <span>Added</span><mark className="added">{compareResult.after || "No obvious word-level addition"}</mark>
              </div>
            </div>
            <button className="close-compare" onClick={() => {setCompareA(null); setSelected(null)}}>Done</button>
          </aside>
        )}

        {moreNode && (
          <div className="menu-popover anim-fade-scale">
            <div className="menu-title">{moreNode.time}</div>
            <button onClick={() => {setSelected(moreNode);setMoreNode(null)}}><Maximize2/> View full text</button>
            <button onClick={() => {compare(moreNode);setMoreNode(null)}}><GitCompare/> Compare</button>
            <button onClick={() => {restore(moreNode);setMoreNode(null)}}><GitBranch/> Create branch</button>
            <button onClick={() => {copy(moreNode);setMoreNode(null)}}><Copy/> Copy text</button>
            <button className="danger-row" onClick={() => {remove(moreNode);setMoreNode(null)}}><Trash2/> Delete</button>
          </div>
        )}

      {toast && <div className="toast anim-fade-up">{toast}</div>}
    </div>
  );
}