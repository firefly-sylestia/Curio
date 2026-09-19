# Prompt.md — current request

## 1. The reader: a zoomed page could not be moved, and the column looked wrong

The member's ask: *"with pinched zoom still the pages slips when its on side by
side pages properly fix the page chnaging when pinced zoom, also the vertical
zoom is fine now but again really bad and buggy, so please fix it, i cant even
move around when zoomed in in vetical scrolling, also kind of weird looking when
the above pages are not separated of their on pages etc its kind of bad behavior
and weird look fix and refine mit and also fix the ocl and push the fix."*

### The real cause of BOTH zoom bugs — `readerZoomedPan` cancelled its own drag

`readerZoomThisPage` passed `focus = focus + drag`, and the helper solved

```
next = at - centre - (at - centre - pan) * ratio
```

At a **constant zoom** (`ratio == 1`, which is every one-finger pan) that
expression collapses to exactly `pan` — the drag cancelled itself out. So a
magnified page answered "no room" to every pan, `Offset.Zero` was returned, the
drag was left unconsumed, and the surface underneath took it:

- **vertical reader** — the column scrolled instead of the page moving:
  *"i cant even move around when zoomed in in vetical scrolling"*;
- **paged reader** — the pager turned the page you were magnifying:
  *"the pages slips when its on side by side pages"* / *"the page chnaging when
  pinced zoom"*.

**The fix (v404).** The drag is folded in by `readerZoomedPan` itself:

```
next = pan * ratio + drag + (focus - centre) * (1 - ratio)
```

`drag = (0,0)` for a double tap, so the anchoring half still grows the page
about the tapped word; at `ratio == 1` it reduces to `pan + drag`, which is what
makes a zoomed page move. The caller now passes `focus` WITHOUT the drag. The
travel is still clamped to the page's own room (`drawn` inside `box`), so a drag
the page has no room for is still handed back — a page turn at the end of a
magnified page still arrives on the next swipe, which is the member's own rule.

### The column's look — sheets, not one continuous strip

The PDF column stacked its pages **flush** (`spacedBy(0.dp)`), each clipped at
its own edge, so a run of scans read as one strip with slivers of rounding down
it — and with one page magnified inside its frame the whole thing read as a
printout with the odd page swollen (*"its kind of weird looking when the above
pages are not separated of their on pages"*). Each page is now a sheet: it sits
on the reader's own paper (`.background(palette.paper)`), wears a hairline edge
(`.border(1.dp, ink @ 10%)`) and has air around it
(`contentPadding(start/end 14, top 10, bottom 18)` + `spacedBy(16.dp)`).

### Also in this batch

- The stale duplicate of `readerZoomedPan`'s old doc comment (which sat orphaned
  above `readerZoomThisPage`) is gone; the helper carries the derivation itself.
- `app/AGENTS.md`: the reader rules that said zoom "waits for a second pointer"
  and that "the focus cancels" at a constant zoom were both wrong after v403 and
  are rewritten (the drag must never be folded in by the caller).

## 2. The compile error (CI red)

```
PersonalCanvas.kt:2877 Unresolved reference 'radius'
PersonalCanvas.kt:2881 Unresolved reference 'hairline'
...
```

HEAD had the new print-cell branch of the landing ghost **outside** the
`if (band > 0f) { ... }` block where `band`, `lead`, `radius` and `hairline` are
declared (HEAD: the branch opens at 2814 and closes at 2850, with
`carriedIsPrint` at 2866). The working tree already moves that branch inside the
band's own scope (the branch closes at 2902), so `radius`/`hairline` resolve. No
further code change was needed — the fix is committed in this push.

## 3. Not pushed by choice, and known gaps

- CI is unconfirmed on the previous two commits; this push is meant to turn it
  green.
- The member asked for the moderation/forms work to be held until they say.
