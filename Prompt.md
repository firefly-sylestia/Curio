# Prompt — Current Request

## Request
"remove the dislike and make the like as Favorite and put the category in the bottom like that category and the favorite icon, and the category gets expanded automatically just like when i tap like it gets expanded. and remove that category from the top corner. and again dont push this, and the animation of expanding and collapse just like the home nav pill buttons. and also in shuffle page the category and the Filters the texts size it feels a little offset so if it is make it centered"

## Status: IN PROGRESS

## Changes made

### TopicRevealScreen.kt
- **Removed dislike button** from RevealSentimentPill — only Favorite remains
- **Renamed "Like" to "Favorite"** with Star/StarOutline icon (filled when active)
- **Removed category chip from top bar** — category no longer shown in top-left corner
- **New bottom bar** (`RevealCategoryFavoriteBar`): category icon + name on left (expands when favorited), favorite star on right
- Category pill expands from 56dp (icon-only) to 160dp (icon + name) using the same nav-pill spring (stiffness=120, dampingRatio=1)
- Favorite star toggles between Star (filled) and StarOutline (outline)

### SpinScreen.kt
- Added `textAlign = TextAlign.Center` to DeckControlButton's Text for proper centering

## Remaining work
- Commit + push (user said don't push yet)
