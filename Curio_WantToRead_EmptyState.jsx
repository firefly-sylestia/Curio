import React, { useState } from "react";

export default function App() {
  const [items, setItems] = useState([]);
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  const addCapture = () => {
    setItems((prev) => [
      ...prev,
      {
        id: Date.now(),
        title: "A new discovery",
      },
    ]);
  };

  return (
    <div className="curio-shell">
      <style>{`
        * { box-sizing: border-box; }
        html, body, #root { margin: 0; min-height: 100%; }
        body {
          font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          background: #f5eee3;
          color: #35131f;
        }

        button { font: inherit; }

        .curio-shell {
          width: 100%;
          min-height: 100vh;
          min-height: 100dvh;
          background: #f8f1e7;
          overflow-x: hidden;
        }

        .phone {
          width: min(100%, 430px);
          min-height: 100vh;
          min-height: 100dvh;
          margin: 0 auto;
          position: relative;
          background: #f8f1e7;
          overflow: hidden;
        }

        .hero {
          height: 305px;
          padding: 24px 30px 0;
          position: relative;
          background: #efcbb0;
          border-bottom-left-radius: 50% 12px;
          border-bottom-right-radius: 50% 12px;
        }

        .status {
          height: 27px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          color: #573725;
          font-size: 14px;
          letter-spacing: .2px;
        }

        .status-right {
          display: flex;
          gap: 7px;
          align-items: center;
          font-size: 12px;
          font-weight: 650;
        }

        .hero-actions {
          position: absolute;
          right: 26px;
          top: 85px;
          display: flex;
          gap: 12px;
        }

        .round {
          width: 70px;
          height: 70px;
          border: 0;
          border-radius: 50%;
          background: rgba(255, 250, 244, .86);
          color: #573725;
          display: grid;
          place-items: center;
          cursor: pointer;
          box-shadow: 0 5px 16px rgba(93, 58, 38, .08);
          transition: transform .18s ease, background .18s ease;
        }

        .round.small {
          width: 54px;
          height: 54px;
        }

        .round:hover { transform: translateY(-2px); }
        .round:active { transform: scale(.96); }

        .hero-copy {
          position: absolute;
          left: 30px;
          bottom: 57px;
        }

        .eyebrow {
          margin: 0 0 4px;
          color: #754a39;
          font-size: 14px;
          opacity: .82;
        }

        .hero h1 {
          margin: 0;
          color: #482117;
          font-family: Georgia, "Times New Roman", serif;
          font-size: 39px;
          font-weight: 500;
          letter-spacing: -1.2px;
        }

        .count {
          margin-top: 4px;
          color: #765044;
          font-size: 16px;
        }

        .content {
          padding: 30px 28px 150px;
          min-height: calc(100vh - 305px);
          min-height: calc(100dvh - 305px);
        }

        .collection-head {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 16px;
        }

        .collection-title h2 {
          margin: 0;
          font-family: Georgia, "Times New Roman", serif;
          color: #42121e;
          font-size: 28px;
          font-weight: 500;
          letter-spacing: -.5px;
        }

        .collection-title p {
          margin: 3px 0 0;
          color: #80645b;
          font-size: 15px;
        }

        .collection-actions {
          display: flex;
          align-items: center;
          gap: 9px;
        }

        .add-top {
          border: 0;
          background: #fce1df;
          color: #e97892;
          height: 54px;
          padding: 0 19px;
          border-radius: 27px;
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          transition: transform .18s ease, background .18s ease;
        }

        .add-top:hover { transform: translateY(-2px); background: #f9d9d9; }
        .add-top span { font-size: 26px; line-height: 1; font-weight: 300; }

        .menu-wrap { position: relative; }

        .menu {
          position: absolute;
          right: 0;
          top: 63px;
          width: 155px;
          padding: 7px;
          border-radius: 17px;
          background: #fffaf3;
          box-shadow: 0 12px 30px rgba(67, 39, 27, .14);
          z-index: 10;
        }

        .menu button {
          width: 100%;
          border: 0;
          background: transparent;
          text-align: left;
          padding: 11px 12px;
          border-radius: 11px;
          color: #54372f;
          cursor: pointer;
        }

        .menu button:hover { background: #f3e8db; }

        .search {
          margin-top: 18px;
          display: flex;
          align-items: center;
          gap: 10px;
          overflow: hidden;
          max-height: 0;
          opacity: 0;
          transition: max-height .25s ease, opacity .2s ease;
        }

        .search.open {
          max-height: 55px;
          opacity: 1;
        }

        .search input {
          width: 100%;
          height: 44px;
          border: 1px solid #eadbc9;
          border-radius: 22px;
          background: #fffaf3;
          padding: 0 17px;
          outline: none;
          color: #4b2930;
        }

        .empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          padding-top: 105px;
        }

        .book-art {
          width: 205px;
          height: 155px;
          position: relative;
          margin-bottom: 22px;
        }

        .ground {
          position: absolute;
          width: 170px;
          height: 23px;
          left: 18px;
          bottom: 8px;
          border-radius: 50%;
          background: #ead8c8;
          opacity: .8;
        }

        .book {
          position: absolute;
          left: 31px;
          width: 145px;
          height: 25px;
          border: 2px solid #a7785c;
          border-radius: 7px 8px 9px 6px;
          background: #d9a887;
          transform: rotate(-2deg);
          box-shadow: 0 4px 0 rgba(133, 87, 65, .06);
        }

        .book.one { bottom: 25px; }
        .book.two {
          bottom: 47px;
          width: 136px;
          left: 37px;
          background: #f1e3d2;
          transform: rotate(3deg);
        }

        .book-lines {
          position: absolute;
          right: 11px;
          top: 6px;
          width: 55px;
          height: 2px;
          background: #bd9379;
          opacity: .6;
          box-shadow: 0 6px 0 #bd9379, 0 12px 0 #bd9379;
        }

        .leaf {
          position: absolute;
          left: 13px;
          bottom: 39px;
          width: 42px;
          height: 67px;
          transform: rotate(-15deg);
        }

        .leaf:before {
          content: "";
          position: absolute;
          left: 20px;
          top: 8px;
          width: 2px;
          height: 59px;
          background: #8d8068;
        }

        .leaf i {
          position: absolute;
          width: 17px;
          height: 9px;
          border: 2px solid #8d8068;
          border-radius: 100% 0 100% 0;
          background: #d8cbb6;
        }

        .leaf i:nth-child(1) { left: 4px; top: 17px; transform: rotate(-26deg); }
        .leaf i:nth-child(2) { left: 19px; top: 27px; transform: rotate(21deg); }
        .leaf i:nth-child(3) { left: 2px; top: 35px; transform: rotate(-28deg); }
        .leaf i:nth-child(4) { left: 20px; top: 44px; transform: rotate(22deg); }

        .card {
          position: absolute;
          left: 66px;
          bottom: 48px;
          width: 83px;
          height: 103px;
          border: 2px solid #a7785c;
          border-radius: 4px;
          background: #fff6e9;
          transform: rotate(4deg);
          box-shadow: 0 5px 8px rgba(108, 73, 51, .07);
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 12px;
        }

        .card-text {
          font-family: Georgia, "Times New Roman", serif;
          color: #9a715d;
          font-size: 12px;
          line-height: 1.65;
          letter-spacing: 2px;
        }

        .spark {
          position: absolute;
          color: #a87b61;
          font-family: Georgia, serif;
        }

        .spark.one { right: 17px; top: 28px; font-size: 25px; }
        .spark.two { right: 3px; top: 58px; font-size: 15px; }

        .empty h3 {
          margin: 0;
          font-family: Georgia, "Times New Roman", serif;
          font-size: 31px;
          font-weight: 500;
          letter-spacing: -.6px;
          color: #431321;
        }

        .empty-copy {
          margin: 14px 0 0;
          color: #81665d;
          font-size: 16px;
          line-height: 1.55;
          max-width: 315px;
        }

        .primary {
          margin-top: 25px;
          height: 58px;
          padding: 0 26px;
          border: 0;
          border-radius: 29px;
          background: #ed8197;
          color: white;
          display: flex;
          align-items: center;
          gap: 11px;
          font-size: 16px;
          cursor: pointer;
          box-shadow: 0 8px 20px rgba(215, 105, 129, .16);
          transition: transform .18s ease, box-shadow .18s ease;
        }

        .primary:hover {
          transform: translateY(-2px);
          box-shadow: 0 11px 24px rgba(215, 105, 129, .22);
        }

        .primary:active { transform: scale(.98); }

        .primary .plus {
          font-size: 26px;
          line-height: 1;
          font-weight: 250;
        }

        .divider {
          width: min(100%, 350px);
          display: flex;
          align-items: center;
          gap: 15px;
          margin-top: 39px;
          color: #c9a895;
          font-size: 10px;
          letter-spacing: 4px;
          white-space: nowrap;
        }

        .divider:before, .divider:after {
          content: "";
          height: 1px;
          flex: 1;
          background: #ddc7b5;
        }

        .saved-list {
          width: 100%;
          margin-top: 45px;
          display: grid;
          gap: 10px;
        }

        .saved-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 15px 17px;
          background: #fffaf3;
          border: 1px solid #eee1d4;
          border-radius: 17px;
          color: #4d2a32;
        }

        .bottom-nav {
          position: fixed;
          z-index: 20;
          left: 50%;
          bottom: 18px;
          transform: translateX(-50%);
          width: min(calc(100% - 36px), 380px);
          height: 82px;
          padding: 9px;
          border-radius: 43px;
          background: rgba(250, 244, 235, .94);
          box-shadow: 0 15px 30px rgba(70, 45, 32, .18);
          backdrop-filter: blur(16px);
          display: grid;
          grid-template-columns: 1fr 1fr 1.75fr;
          gap: 5px;
        }

        .nav-item {
          border: 0;
          background: transparent;
          color: #633e42;
          border-radius: 36px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 9px;
          cursor: pointer;
          transition: background .2s ease, transform .18s ease;
        }

        .nav-item:hover { transform: translateY(-1px); }

        .nav-item.active {
          background: #f1ccb4;
          color: #5b301c;
          font-weight: 700;
        }

        .icon {
          width: 25px;
          height: 25px;
          display: block;
        }

        .nav-label { font-size: 16px; }

        @media (max-width: 380px) {
          .hero { padding-left: 23px; padding-right: 23px; }
          .hero-copy { left: 23px; }
          .hero h1 { font-size: 35px; }
          .hero-actions { right: 18px; gap: 7px; }
          .round { width: 61px; height: 61px; }
          .content { padding-left: 20px; padding-right: 20px; }
          .collection-title h2 { font-size: 25px; }
          .empty { padding-top: 80px; }
          .nav-label { font-size: 14px; }
        }
      `}</style>

      <div className="phone">
        <header className="hero">
          <div className="status">
            <span>9:04&nbsp;&nbsp;Sep 10</span>
            <span className="status-right">
              <span>LTE+</span><span>▮▮▮</span><span>71%</span>
            </span>
          </div>

          <div className="hero-actions">
            <button className="round" aria-label="Go back" onClick={() => history.back()}>
              <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M19 12H5M11 6l-6 6 6 6" />
              </svg>
            </button>
            <button className="round" aria-label="Search" onClick={() => setSearchOpen((v) => !v)}>
              <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="10.8" cy="10.8" r="6.7" />
                <path d="m16 16 5 5" />
              </svg>
            </button>
            <div className="menu-wrap">
              <button className="round" aria-label="More" onClick={() => setMenuOpen((v) => !v)}>
                <svg className="icon" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="5" r="1.7" /><circle cx="12" cy="12" r="1.7" /><circle cx="12" cy="19" r="1.7" />
                </svg>
              </button>
              {menuOpen && (
                <div className="menu">
                  <button onClick={() => setItems([])}>Clear collection</button>
                  <button onClick={() => setMenuOpen(false)}>Close menu</button>
                </div>
              )}
            </div>
          </div>

          <div className="hero-copy">
            <h1>Want to Read</h1>
            <div className="count">{items.length} {items.length === 1 ? "item" : "items"}</div>
          </div>
        </header>

        <main className="content">
          <div className="collection-head">
            <div className="collection-title">
              <h2>Want to Read</h2>
              <p>{items.length} {items.length === 1 ? "item" : "items"}</p>
            </div>

            <div className="collection-actions">
              <button className="add-top" onClick={addCapture}>
                <span>+</span> Add
              </button>
              <div className="menu-wrap">
                <button className="round small" onClick={() => setMenuOpen((v) => !v)} aria-label="Collection menu">
                  <svg className="icon" viewBox="0 0 24 24" fill="currentColor">
                    <circle cx="12" cy="5" r="1.7" /><circle cx="12" cy="12" r="1.7" /><circle cx="12" cy="19" r="1.7" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div className={`search ${searchOpen ? "open" : ""}`}>
            <input placeholder="Search this collection..." autoFocus={searchOpen} />
          </div>

          {items.length === 0 ? (
            <section className="empty">
              <div className="book-art" aria-hidden="true">
                <div className="ground" />
                <div className="book one"><div className="book-lines" /></div>
                <div className="book two"><div className="book-lines" /></div>
                <div className="leaf">
                  <i /><i /><i /><i />
                </div>
                <div className="card">
                  <div className="card-text">Good<br />reads<br />await</div>
                </div>
                <div className="spark one">✦</div>
                <div className="spark two">✧</div>
              </div>

              <h3>Nothing here yet</h3>
              <p className="empty-copy">
                Save something from your discoveries<br />
                or add a capture here.
              </p>

              <button className="primary" onClick={addCapture}>
                <span className="plus">+</span>
                Add a capture
              </button>

              <div className="divider">SMALL DISCOVERIES ADD UP</div>
            </section>
          ) : (
            <div className="saved-list">
              {items.map((item) => (
                <div className="saved-item" key={item.id}>
                  <span>{item.title}</span>
                  <span>›</span>
                </div>
              ))}
            </div>
          )}
        </main>

        <nav className="bottom-nav">
          <button className="nav-item" aria-label="Home">
            <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="m4 10 8-6 8 6v10a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1z" />
            </svg>
          </button>

          <button className="nav-item" aria-label="Discover">
            <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z" />
              <path d="m19 15 .8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z" />
            </svg>
          </button>

          <button className="nav-item active" aria-current="page">
            <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 7h16v13H4z" />
              <path d="M6 4h12v3H6zM8 11h8" />
            </svg>
            <span className="nav-label">Cabinet</span>
          </button>
        </nav>
      </div>
    </div>
  );
}
