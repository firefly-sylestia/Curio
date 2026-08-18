package com.curio.app.data

import android.app.AppOpsManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple SharedPreferences wrapper for Curio user preferences.
 *
 * Stores display name, theme mode, daily reminder, and data flags
 * across app restarts. Used by ProfileScreen and SettingsScreen.
 */
/**
 * Spin density sizing strength (v7.4) — replaces the old on/off switch with
 * three levels:
 *  - [OFF] — density sizing disabled entirely (the DIMENSION rule can still
 *    compact short screens via "Smart Spin layout").
 *  - [COMPACT] — classic rule: under 440 dpi the deck compacts, 440+ dpi
 *    gets a roomier deck.
 *  - [EXTRA_COMPACT] — adds a 2x tier: under ~350 dpi the deck shrinks even
 *    further so very low-dpi phones fit everything comfortably.
 */
enum class SmartDensityMode { OFF, COMPACT, EXTRA_COMPACT }

object AppPreferences {

    /** Topic sentiment constants — like/dislike from Topic Reveal feeds the
     *  Spin shuffle weighting (liked topics + their category get more weight,
     *  disliked get less — never fully blocked). [SENTIMENT_NONE] clears. */
    const val SENTIMENT_LIKE = "like"
    const val SENTIMENT_DISLIKE = "dislike"
    const val SENTIMENT_NONE = "none"

    /** Theme mode constants (v81 — the reimagined dark mode): "light"
     *  (default, the v78 shipped behavior), "dark" (pitch-black + glow),
     *  or "system" (follows the device night flag). */
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    private const val NAME = "curio_app_prefs"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_THEME_MODE = "theme_mode"       // "light", "dark", "system" (v81)
    private const val KEY_CUSTOM_TAGLINE = "custom_streak_tagline"
    private const val KEY_LAST_NOTIFIED_UPDATE = "last_notified_update_version"
    private const val KEY_PET_CHATTER = "pet_chatter"     // "talkative", "cozy", "quiet"
    private const val KEY_PET_GAME_FREQUENCY = "pet_game_frequency" // "relaxed", "normal", "eager"
    private const val KEY_PET_BIRTHDAY = "pet_birthday_epoch_day"
    private const val KEY_SAVES_WEEK_START = "saves_week_start_epoch_day"
    private const val KEY_SAVES_WEEK_COUNTS = "saves_week_counts"
    private const val KEY_PASTEL_COLORS_ENABLED = "pastel_colors_enabled"
    private const val KEY_PASTEL_CROWN_DEPTH = "pastel_crown_depth"
    // v185 — proper M3 Material theme (opt-in, default OFF): the whole
    // color system re-does per M3 guidelines (single primary, neutral
    // surfaces, 36 lane accents collapsed to ~6 muted families).
    private const val KEY_MATERIAL_THEME = "material_theme"
    private const val KEY_HERO_BLUE = "hero_azure_enabled"   // sky-azure hero variant (v27l)
    private const val KEY_HERO_FOLLOW_LANE = "hero_follow_lane"  // shared hero + page follow the Spin lane (v30)
    // v28 — dark-mode elevation visibility: black shadows vanish on
    // near-black surfaces, so dark mode can draw a soft LIGHT glow shadow
    // (default OFF). The v28 hairline outline option was REMOVED.
    private const val KEY_DARK_GLOW = "dark_glow"
    private const val KEY_PROMO_MODE = "promo_mode"   // hidden promo/demo-content mode
    // v7.7 — experimental peek-card redesign, four independent toggles so
    // each upgrade can be A/B'd on its own: top-lit gradient fill, tinted
    // hairline, soft shadows, roomier two-line near titles. Each OFF by
    // default — the classic flat deck stays the shipping look until the
    // experiment settles.
    private const val KEY_PEEK_GRADIENT = "peek_gradient"
    private const val KEY_PEEK_HAIRLINE = "peek_hairline"
    private const val KEY_PEEK_SHADOWS = "peek_shadows"
    private const val KEY_PEEK_TITLES = "peek_titles"
    private const val KEY_PEEK_TAIL_FADE = "peek_tail_fade"
    // v7.13 — Main card (hero ticket) redesign toggles: enhanced gradient
    // fill, accent border, soft shadow. All OFF by default so the current
    // hero card stays exactly as-is until enabled. (Enhanced typography was
    // promoted to the shipped default in v7.16 — no longer toggleable.)
    private const val KEY_HERO_GRADIENT = "hero_gradient"
    private const val KEY_HERO_BORDER = "hero_border"
    private const val KEY_HERO_SHADOW = "hero_shadow"
    // v10 — dual-accent blend gradient: the hero card wears the category
    // accent blended with a warm golden companion for a richer palette.
    // Toggleable (default OFF); works across all theme styles.
    private const val KEY_HERO_BLEND_GRADIENT = "hero_blend_gradient"
    private const val KEY_3D_BUTTON_GRADIENT = "3d_button_gradient"
    private const val KEY_REMINDER_ENABLED = "reminder_enabled"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_TINT_WASH_ENABLED = "tint_wash_enabled"
    private const val KEY_ENTRY_META_ENABLED = "entry_meta_enabled"
    private const val KEY_SMART_SPIN_LAYOUT = "smart_spin_layout"
    // v7.4 — the density rule is a 3-way STRENGTH picker now. The legacy
    // boolean key below is read once for migration and then removed.
    private const val KEY_SMART_DENSITY_MODE = "smart_density_mode"
    private const val KEY_LEGACY_SMART_DENSITY_LAYOUT = "smart_density_layout"
    private const val KEY_EXPLORE_SESSIONS_ENABLED = "explore_sessions_enabled"
    private const val KEY_LIVE_NOTIFICATIONS_ENABLED = "live_notifications_enabled"
    private const val KEY_OVERLAY_BUBBLE_ENABLED = "overlay_bubble_enabled"
    // v23 — whether the "Show the explore bubble" opt-in row appears inside
    // the Explore now dialog (default OFF; the Notifications toggle
    // re-shows it there as a single-line choice).
    private const val KEY_SHOW_BUBBLE_OPT_IN_DIALOG = "show_bubble_opt_in_dialog"
    // v19 — the search engine the "Explore in browser" button opens (Google
    // by default; DuckDuckGo, Bing, Brave, Ecosia, Startpage, Yahoo).
    private const val KEY_SEARCH_ENGINE = "search_engine"
    // v27s — the music service the "Watch in" explore action opens for
    // Album / Artist / Song topics (YouTube Music by default; Apple Music,
    // Spotify).
    private const val KEY_MUSIC_SERVICE = "music_service"
    // v27 — recycle-bin retention: how many days soft-deleted captures stay
    // before being auto-deleted forever (0 = keep forever). Default 30 days.
    private const val KEY_RECYCLE_BIN_EXPIRY_DAYS = "recycle_bin_expiry_days"
    private const val DEFAULT_RECYCLE_BIN_EXPIRY_DAYS = 30
    // v8.1 — "don't nag" flag: once the user declines the "Display over
    // other apps" permission (dismisses the prompt or returns from system
    // settings without granting), all AUTOMATIC overlay prompts are
    // suppressed until they explicitly try to enable the bubble from
    // Settings (the Settings toggle clears it).
    private const val KEY_OVERLAY_ASK_DECLINED = "overlay_ask_declined"
    // Experimental voice-to-text/dictation. Default OFF so microphone
    // transcription never appears or starts until the user opts in.
    private const val KEY_VOICE_TO_TEXT_ENABLED = "voice_to_text_enabled"
    // v125 — the selected offline transcription model id (Vosk catalog id).
    private const val KEY_OFFLINE_MODEL = "offline_model_id"
    // v8.5 — the Curio pet companion (spec §10): the pixel pet + its
    // rule-based dialogue + the category passport/discovery features on
    // Quests and Home. Default ON; a user-facing Appearance toggle gates
    // the whole companion layer so it can be A/B'd and reverted without a
    // code change (per the experiment rules, the toggle is removed once the
    // companion design is decided).
    private const val KEY_PET_ENABLED = "pet_enabled"
    private const val KEY_FLOATING_PET_ENABLED = "floating_pet_enabled"
    // v8.43 — the pet's local LEARNING model (CurioPetBrain): the pet
    // observes real activity and grows its own personality + catchphrases.
    // Default ON per the user; off falls back to the classic rule-based
    // lines. Independent of [KEY_PET_ENABLED]: the pet layer can be on
    // while the learning brain is off.
    private const val KEY_PET_BRAIN_ENABLED = "pet_brain_enabled"
    private const val KEY_AUTO_OPEN_REVEAL = "auto_open_reveal"
    private const val KEY_PINNED_TOPICS = "pinned_topics"   // JSON array of PinnedTopic
    private const val KEY_SAVED_QUOTES = "saved_quotes"      // JSON array of SavedQuote
    private const val KEY_TOPIC_SENTIMENTS = "topic_sentiments"  // JSON object: "CATEGORY:topicId" -> "like"/"dislike"
    private const val KEY_LAST_SPIN_CATEGORY = "last_spin_category"
    private const val KEY_LAST_SPIN_CATEGORIES = "last_spin_categories"   // comma-joined set
    private const val KEY_LANDED_TOPIC_PREFIX = "landed_topic_"
    // v7.94 — Manage Categories persistence: the hidden set + the custom
    // order, both comma-joined CategoryId names. Previously the screen kept
    // only a local rememberSaveable list, so toggles/order died on app
    // restart and nothing else in the app honored them.
    private const val KEY_HIDDEN_CATEGORIES = "hidden_categories"
    private const val KEY_CATEGORY_ORDER = "category_order"
    // v8.34 — custom pet design (Pet designer playground): the imported
    // design's full text (palette + body/curled grids). Always-on when
    // saved — the pet sprite renders this instead of the default until the
    // user resets it. Null = default design.
    private const val KEY_PET_DESIGN = "pet_design"
    // v8.39 — custom reaction speech is saved with the pet design but stays
    // opt-in so the built-in Curie dialogue remains the default experience.
    private const val KEY_CUSTOM_REACTION_LINES = "custom_reaction_lines"
    // v8.47 — recently-applied palette colors for the pet designer picker.
    private const val KEY_PET_RECENT_COLORS = "pet_recent_colors"
    // v8.56 — the two user-saved custom pet slots (Pet studio Pets page).
    private const val KEY_PET_CUSTOM_1 = "pet_custom_1"
    private const val KEY_PET_CUSTOM_2 = "pet_custom_2"
    // v9.3 — custom flower bed design (32×18 pixel rows).
    private const val KEY_BED_DESIGN = "bed_design_rows"

    // ── Display name ─────────────────────────────────────────────────
    fun getDisplayName(context: Context): String =
        prefs(context).getString(KEY_DISPLAY_NAME, null) ?: "Curious Explorer"

    fun setDisplayName(context: Context, name: String) =
        prefs(context).edit().putString(KEY_DISPLAY_NAME, name).apply()

    // ── Custom streak tagline (v53) ──────────────────────────────────
    // The Profile hero tagline: a user-set line replaces the automatic
    // streak-based tagline; empty = use the automatic one again.
    fun getCustomStreakTagline(context: Context): String =
        prefs(context).getString(KEY_CUSTOM_TAGLINE, "").orEmpty()

    fun setCustomStreakTagline(context: Context, tagline: String) =
        prefs(context).edit().putString(KEY_CUSTOM_TAGLINE, tagline.trim()).apply()

    // ── Profile avatar (v103) ────────────────────────────────────────
    // The saved profile photo's absolute path inside the app's private
    // files dir ("" = none). The image itself lives in filesDir as
    // profile_avatar_<timestamp>.png; only the path is stored here.
    private const val KEY_PROFILE_AVATAR = "profile_avatar"
    fun getProfileAvatarPath(context: Context): String =
        prefs(context).getString(KEY_PROFILE_AVATAR, "").orEmpty()

    fun setProfileAvatarPath(context: Context, path: String) =
        prefs(context).edit().putString(KEY_PROFILE_AVATAR, path).apply()

    // ── Update-notification dedupe (v53) ─────────────────────────────
    // The version tag of the newest update that has ALREADY been announced
    // with a notification — the updater only notifies once per new release
    // ("once the update comes, not always"), while the toast fires on every
    // check that finds an update.
    fun getLastNotifiedUpdateVersion(context: Context): String =
        prefs(context).getString(KEY_LAST_NOTIFIED_UPDATE, "").orEmpty()

    fun setLastNotifiedUpdateVersion(context: Context, version: String) =
        prefs(context).edit().putString(KEY_LAST_NOTIFIED_UPDATE, version).apply()

    // ── Update-check result cache (v115) ─────────────────────────────
    // The last successful check's release info is SAVED locally so the
    // Updates page shows the release notes instantly on open — no
    // reloading the network result on every visit (the page still
    // refreshes in the background).
    private const val KEY_UPDATE_CACHE_TAG = "update_cache_tag"
    private const val KEY_UPDATE_CACHE_NOTES = "update_cache_notes"
    private const val KEY_UPDATE_CACHE_HTML = "update_cache_html_url"
    private const val KEY_UPDATE_CACHE_APK = "update_cache_apk_url"

    fun getCachedUpdateInfo(context: Context): UpdateInfo? {
        val tag = prefs(context).getString(KEY_UPDATE_CACHE_TAG, "").orEmpty()
        if (tag.isBlank()) return null
        return UpdateInfo(
            tagName = tag,
            htmlUrl = prefs(context).getString(KEY_UPDATE_CACHE_HTML, "").orEmpty()
                .ifBlank { "https://github.com/firefly-sylestia/Curio/releases/tag/$tag" },
            releaseNotes = prefs(context).getString(KEY_UPDATE_CACHE_NOTES, "").orEmpty()
                .takeIf { it.isNotBlank() },
            apkUrl = prefs(context).getString(KEY_UPDATE_CACHE_APK, "").orEmpty()
                .takeIf { it.isNotBlank() }
        )
    }

    fun setCachedUpdateInfo(context: Context, info: UpdateInfo) {
        prefs(context).edit()
            .putString(KEY_UPDATE_CACHE_TAG, info.tagName)
            .putString(KEY_UPDATE_CACHE_NOTES, info.releaseNotes.orEmpty())
            .putString(KEY_UPDATE_CACHE_HTML, info.htmlUrl)
            .putString(KEY_UPDATE_CACHE_APK, info.apkUrl.orEmpty())
            .apply()
    }

    // Theme mode (v81) — "light" / "dark" / "system". v78 removed the
    // theme system; the reimagined dark mode (pitch-black pages, dark
    // same-hue hero shades, Samsung-style inner glow) returns here as a
    // single mode (no AMOLED/Material styles). Default LIGHT — the v78
    // shipped behavior. Seeded from prefs in [initThemeMode].
    var themeModeState by mutableStateOf(THEME_LIGHT)
        private set

    // Pastel color mode (v7.5) — a user toggle that softens every category
    // accent (fills become pastel with deep-matching ink in light mode,
    // muted deep pastels in dark) and pastel-izes the mixed-deck blends and
    // every blended/tinted color derived from the accents. Independent of
    // theme STYLE (combines with Curio, AMOLED and Material) and theme MODE.
    // Default ON (v7.x — the soft look is the app's shipped default now).
    // Seeded from prefs in [initThemeMode].
    var pastelColorsState by mutableStateOf(true)
        private set

    // v185 — proper M3 Material theme (default OFF): the whole color system
    // re-does per M3 guidelines when on. Opt-in — the default app look is
    // untouched.
    var materialThemeState by mutableStateOf(false)
        private set

    // Sky-azure hero variant (v27l) — when ON, the shared torn hero
    // (Home / Profile / Settings / Cabinet) wears the app's airy pastel
    // azure instead of the rose-wood. v42 — azure is back AND the DEFAULT
    // (the v31 grey-out is gone); rose is now the opt-out.
    var heroBlueState by mutableStateOf(true)
        private set

    // v30 — "Hero follows Spin lane" (Appearance): when ON and the Spin
    // deck is on a single lane, the shared hero AND its page background
    // wear that category's accent + wash (the Cabinet's language) instead
    // of the rose/azure. Off by default — rose stays.
    var heroFollowLaneState by mutableStateOf(false)
        private set

    // v28 — dark-mode elevation: black shadows are invisible on the
    // midnight surfaces, so dark mode draws a soft LIGHT glow shadow
    // (default OFF; users can opt into the glow from Appearance). Light mode
    // is untouched either way.
    var darkGlowState by mutableStateOf(false)
        private set

    // Pastel crown depth (v7.12, EXPERIMENTAL) — when pastel mode is ON
    // and this toggle is ON, the top of pastel card gradients gets a
    // subtle 5% black deepen so every card reads with a gentle darker
    // crown for depth instead of a uniform pastel from edge to edge.
    // Default ON. Only takes effect when pastel mode is active.
    var pastelCrownDepthState by mutableStateOf(true)
        private set

    // Hidden promo/demo-content mode (v7.107) — OFF by default; v24 it is
    // reached from the Experiments screen (Settings → Experiments → Promo
    // mode, or the Version row's five-tap in Support & diagnostics) and its
    // own page's toggle is the one control. While ON, the app shows promo sample
    // content everywhere (Home hero stats + recents, Profile level,
    // Quests level, Cabinet grid) so the user can screenshot the app for
    // store promotion. Demo data is derived from real topics via
    // [PromoMode] — no user data is touched. Default OFF.
    var promoModeState by mutableStateOf(false)
        private set

    // Peek-deck redesign (v7.7, EXPERIMENTAL) — the Spin deck's background
    // peek cards wear four independently-toggleable upgrades: a top-lit
    // gradient fill, a category-tinted hairline border, soft ambient
    // shadows, and roomier two-line near-card titles. Each defaults OFF;
    // the classic flat deck stays the default until the experiment
    // concludes (then the winning path is hardcoded and the toggles removed).
    var peekGradientState by mutableStateOf(false)
        private set
    var peekHairlineState by mutableStateOf(false)
        private set
    var peekShadowsState by mutableStateOf(false)
        private set
    var peekTitlesState by mutableStateOf(false)
        private set
    /** Experimental newer peek motion: travel first, then fade at the exit tail. */
    var peekTailFadeState by mutableStateOf(false)
        private set

    // Main card (hero ticket) redesign (v7.13, EXPERIMENTAL) — the Spin
    // deck's front hero card wears four independently-toggleable upgrades:
    // an enhanced top-lit gradient fill, an accent-tinted border, a soft
    // ambient shadow, and enhanced typography (bolder title, bigger
    // subtitle). v15 — the enhanced gradient is promoted to the shipped
    // default (ON, still toggleable in Experiments); the other upgrades
    // stay OFF.
    var heroGradientState by mutableStateOf(true)
        private set
    // v10 — promoted from experiment to always-on: the accent border on the
    // hero card is now the shipped default.
    var heroBorderState by mutableStateOf(true)
        private set
    // v92 — the One UI tinted shadow (accent-colored layered glow) is now
    // the shipped default for the hero ticket; the Experiments toggle
    // remains for comparison.
    var heroShadowState by mutableStateOf(true)
    /** v27 — experimental paper accents (Settings → Experiments → Paper & headers). */
    var paperHeaderCutsState by mutableStateOf(false)
    var paperHeaderHolesState by mutableStateOf(false)
    var paperHoleRingsState by mutableStateOf(false)
    /** v27v — which 3D ring style the pin holes wear ("coil" | "split" | "oblique"). */
    var paperHoleRingStyleState by mutableStateOf("coil")
    // v97 — the Paper stat card experiment PASSED: on by default app-wide
    // (the Experiments toggles stay for comparison).
    var paperStatCardsState by mutableStateOf(true)
    // v101 — the pill glow (dark mode) is the SUBTLE top-only version by
    // default (gentler glass edge + a glow that hugs the pill's top); the
    // toggle restores the fuller glow for comparison.
    var pillGlowSubtleState by mutableStateOf(true)
    var paperStatTearState by mutableStateOf(false)
        private set
    // v108 — torn heroes wear ONLY their bottom tear by default; the white
    // paper under-sheet (the extra layered lip below the seam) is an opt-in
    // experiment in Settings → Experiments → Paper & headers.
    var heroTearSheetState by mutableStateOf(false)
    // v208 — experiment: the Spin Categories/Filter buttons (and their
    // vertical variants) wear the floating NAV-PILL look (capsule, calmed
    // accent active fill, elevated container) instead of the category
    // rounded-24 buttons. Default OFF.
    var navPillButtonsState by mutableStateOf(false)
        private set
    /** v27u — Home tint experiments (Settings → Experiments → Home tint). */
    var homeTintState by mutableStateOf(false)
    var homeHeroTintState by mutableStateOf(false)
    var homeTintFollowLaneState by mutableStateOf(false)
    var homeTintCategoryIdState by mutableStateOf(CategoryId.WILDCARD.name)
    // v27j — header fill depth. ON by default: the torn-hero headers wear a
    // slightly DARKER version of the category's painter accent. Turning it
    // off restores the exact pre-toggle accent. Watermark glyphs, ink and
    // everything else are untouched — only the banner fill color deepens.
    var headerDeepState by mutableStateOf(true)
        private set
    // v10 — dual-accent blend gradient toggle (default OFF). When on, the
    // hero card wears a richer multi-accent blend instead of the plain
    // vertical gradient.
    var heroBlendGradientState by mutableStateOf(false)
        private set

    // 3D button gradient & shadow (v7.11, EXPERIMENTAL) — when ON, the
    // Spin shuffle button wears a radial 3D gradient (highlighted top,
    // shaded bottom) with a soft ambient shadow so it reads as a raised
    // sphere instead of a flat circle. Also fixes the orbiting ring dots
    // in pastel mode (they switch to a contrasting ink so they stay
    // visible on the pastel surface). Default ON. When the experiment
    // settles, hardcode the winner and remove the toggle.
    var threeDButtonState by mutableStateOf(true)
        private set

    var reminderEnabledState by mutableStateOf(false)
        private set

    /**
     * Reactive category-tint state — updated by [setTintWashEnabled] so page
     * backgrounds (via categoryBackgroundWash) instantly revert to the plain
     * theme background when the user toggles it in settings.
     */
    var tintWashEnabledState by mutableStateOf(true)
        private set

    /**
     * Reactive entry-meta state — updated by [setEntryMetaEnabled] so the
     * saved-entry meta card (date & time / mood / type), the "Captured
     * today · 3:42 PM" time, and the journal's mood + attachment sections
     * recompose instantly when the user toggles it in Settings. Default ON.
     * Seeded from prefs in [initThemeMode].
     */
    var entryMetaEnabledState by mutableStateOf(true)
    // Smart Spin layout — the DIMENSION rule of the Spin page's smart
    // compact system: short screens get the compact (or extra-compact)
    // layout. v7.35 — Default OFF: the roomy Spin layout ships by default
    // and compact is opt-in (still toggleable in Settings). Seeded from
    // prefs in [initThemeMode].
    var smartSpinLayoutState by mutableStateOf(false)
    // Smart density mode — the DENSITY rule of the Spin page's smart
    // sizing, now a 3-way strength picker (v7.4): OFF disables density
    // sizing, COMPACT keeps the classic rule (under 440 dpi → smaller,
    // 440+ dpi → roomier), EXTRA_COMPACT adds a 2x tier for very low dpi
    // (under 350 dpi → even smaller deck). v7.35 — Default OFF: the deck
    // ships at its natural size; compact sizing is opt-in. Seeded from
    // prefs in [initThemeMode].
    var smartDensityModeState by mutableStateOf(SmartDensityMode.OFF)
    // Explore sessions — the explore-now timer/reminder/done flow. Default
    // ON; off disables the timer notification + reminder + done prompt while
    // Explore-now still opens the browser and records recently-explored.
    var exploreSessionsEnabledState by mutableStateOf(true)
        private set

    // v19 — the chosen search engine id ("google", "duckduckgo", …) for the
    // Explore browser button. Reactive so the Topic Reveal dialog copy and
    // the Settings row update the moment it changes.
    var searchEngineState by mutableStateOf(SearchEngine.GOOGLE.id)
    // v27s — the chosen music service id ("youtube", "youtube_music",
    // "apple_music", "spotify") for the "Watch in" action on Album /
    // Artist / Song topics. YouTube is the default for new preferences.
    var musicServiceState by mutableStateOf(MusicService.YOUTUBE.id)
    var recycleBinExpiryDaysState by mutableStateOf(DEFAULT_RECYCLE_BIN_EXPIRY_DAYS)
        private set

    // Live explore notifications — the persistent chronometer notification
    // with Pause/Stop controls shown while exploring (like Samsung/Google's
    // live-updating ongoing notifications). Default ON; off means no ongoing
    // notification at all — only the end-of-session reminder + bubble.
    var liveNotificationsEnabledState by mutableStateOf(true)
        private set

    // Floating explore bubble — a Messenger-style timer bubble drawn over
    // OTHER apps (the browser) via SYSTEM_ALERT_WINDOW. Default ON; off
    // means the timer lives only in the notification (when live
    // notifications are on) — there is no in-app pill fallback.
    // v22 — default OFF: the bubble is now opt-in from the explore dialog
    // (and the Settings toggle enables it anytime).
    var overlayBubbleEnabledState by mutableStateOf(false)
        private set
    // v23 — whether the Explore dialog shows its bubble opt-in row. Hidden
    // by default; the Notifications toggle re-shows it (single-line, no
    // subtext) so the dialog stays clean while the Settings toggle still
    // enables the bubble itself.
    var showBubbleOptInDialogState by mutableStateOf(false)
        private set

    // v8.1 — whether the user has declined the "Display over other apps"
    // permission (see [isOverlayAskDeclined]). Suppresses the automatic
    // explore-start prompt; explicit Settings toggles always work and
    // clear it.
    var overlayAskDeclinedState by mutableStateOf(false)
        private set

    // Voice-to-text/dictation (experimental) — opt-in only. This controls
    // dictation in Sound Bite fields and saved voice-note details; ordinary
    // microphone recording remains available regardless of this toggle.
    var voiceToTextEnabledState by mutableStateOf(false)
        private set

    // v125 — the OFFLINE transcription model for pre-recorded sound bites
    // (Vosk). "" = none downloaded/selected; the value is the model id from
    // [VoskModels.CATALOG]. Dictation (live OS recognizer) is unaffected —
    // this only gates the recorded-audio → text flow on the detail page.
    var offlineModelIdState by mutableStateOf("")
        private set
    // Bumped by download/delete so screens reading [VoskModels.isDownloaded]
    // recompose with the fresh state when they regain focus.
    var offlineModelVersionState by mutableIntStateOf(0)
        private set

    /**
     * Curio pet companion state (v8.5) — gates the pixel pet sprite, its
     * rule-based dialogue, and the passport/discovery companion layer on
     * Quests and Home (spec §10). Default ON. Seeded from prefs in
     * [initThemeMode]; off hides the pet entirely and restores the plain
     * quests layout.
     */
    var petEnabledState by mutableStateOf(true)
        private set
    // v8.8 — whether the pet floats freely on every screen (draggable +
    // wanders). Independent of [petEnabledState]: the pet layer can be on
    // while the floating companion is off (the pet then stays in its bed).
    var floatingPetEnabledState by mutableStateOf(true)
        private set
    // v8.43 — whether the pet's learning brain is on (default ON): the pet
    // builds a personality from the user's real activity and develops its
    // own catchphrases. Off = classic rule-based lines only.
    var petBrainEnabledState by mutableStateOf(true)
        private set
    // v16 — how talkative the pet is: "talkative" (lines ~1.4x), "cozy"
    // (default, unchanged), "quiet" (lines ~0.35x — mostly motion).
    var petChatterState by mutableStateOf("cozy")
        private set
    // v16 — how often the pet starts games on its own: "relaxed" (~0.55x),
    // "normal" (default), "eager" (~1.5x).
    var petGameFrequencyState by mutableStateOf("normal")
        private set
    // v8.16 — whether the Spin deck auto-opens the landed topic's reveal the
    // moment the wheel settles. v8.21 — DEFAULT ON: the reveal opens by
    // itself when the deck lands (the tour and pet lines adapt). Turn it
    // OFF to make the deck land quietly with the front card staying
    // tappable until the user opens it manually.
    var autoOpenRevealState by mutableStateOf(true)
        private set

    // v8.39 — custom reaction lines are an explicit opt-in. The editor can
    // always be used, but Curie only speaks saved custom lines when enabled.
    var customReactionLinesState by mutableStateOf(false)
        private set

    /**
     * Reactive pinned-topics state — updated by [pinTopic] / [unpinTopic] so
     * the Topic Reveal pin button and the Topic History "Pinned" section
     * recompose instantly. Seeded from prefs in [initThemeMode].
     */
    var pinnedTopicsState by mutableStateOf<List<PinnedTopic>>(emptyList())
        private set

    /**
     * Reactive saved-quotes state — updated by [saveQuote] /
     * [removeSavedQuote] so the saved-entry bookmark buttons and the Home
     * screen's "Saved" shelf recompose instantly. Seeded from prefs in
     * [initThemeMode].
     */
    var savedQuotesState by mutableStateOf<List<SavedQuote>>(emptyList())
        private set

    /**
     * Reactive topic-sentiment state — keyed "CATEGORY:topicId" → "like" /
     * "dislike". Updated by [setTopicSentiment] so the Topic Reveal buttons
     * and the Spin shuffle recompose/pick with the latest votes. Seeded from
     * prefs in [initThemeMode].
     */
    var topicSentimentsState by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /**
     * Reactive hidden-categories state (v7.94) — set via Manage Categories.
     * [CurioCategories.visible] reads it, so Home/Cabinet/Picker/Spin all
     * drop hidden lanes instantly and persistently. Seeded from prefs in
     * [initThemeMode].
     */
    var hiddenCategoriesState by mutableStateOf<Set<CategoryId>>(emptySet())
        private set

    /**
     * Reactive category ORDER state (v7.94) — set via Manage Categories.
     * Empty = the default order. [CurioCategories.visible] applies it, so
     * the Home/Cabinet chip rows and the pickers honor the user's reorder.
     * Seeded from prefs in [initThemeMode].
     */
    var categoryOrderState by mutableStateOf<List<CategoryId>>(emptyList())
        private set

    /**
     * Reactive custom-pet-design state (v8.34) — the design's import text,
     * or null when the pet wears its default look. Updated by
     * [setPetDesign] / [clearPetDesign] so the pet sprite recomposes
     * instantly when a design is saved in the designer playground.
     */
    var petDesignState by mutableStateOf<String?>(null)
        private set

    /**
     * Reactive custom-pet slots (v8.56) — the two user-saved pet designs
     * (full design text) or null when a slot is empty. Updated by
     * [setCustomPet] / [clearCustomPet] so the Pet studio's Pets page cards
     * recompose the moment a pet is saved or removed.
     */
    var customPetsState by mutableStateOf<List<String?>>(listOf(null, null))
    /** Legacy custom flower-bed rows; the current home uses a fixed house scene. */
    var bedDesignRowsState by mutableStateOf<List<String>?>(null)
    /** v9.5 — evolution path chosen at level 15 (null = baby, no choice yet). */
    var evoPathState by mutableStateOf<String?>(null)
        private set
    /** v9.6 — experimental per-part size and position controls. */
    var petPartTransformsState by mutableStateOf(false)
        private set

    fun initThemeMode(context: Context) {
        themeModeState = getThemeMode(context)
        pastelColorsState = isPastelColorsEnabled(context)
        pastelCrownDepthState = isPastelCrownDepthEnabled(context)
        materialThemeState = isMaterialThemeEnabled(context)
        heroBlueState = isHeroBlueEnabled(context)
        heroFollowLaneState = isHeroFollowLaneEnabled(context)
        darkGlowState = isDarkGlowEnabled(context)
        promoModeState = isPromoModeEnabled(context)
        peekGradientState = isPeekGradientEnabled(context)
        peekHairlineState = isPeekHairlineEnabled(context)
        peekShadowsState = isPeekShadowsEnabled(context)
        peekTitlesState = isPeekTitlesEnabled(context)
        peekTailFadeState = isPeekTailFadeEnabled(context)
        heroGradientState = isHeroGradientEnabled(context)
        heroBorderState = isHeroBorderEnabled(context)
        heroShadowState = isHeroShadowEnabled(context)
        paperHeaderCutsState = isPaperHeaderCutsEnabled(context)
        paperHeaderHolesState = isPaperHeaderHolesEnabled(context)
        paperHoleRingsState = isPaperHoleRingsEnabled(context)
        paperHoleRingStyleState = getPaperHoleRingStyle(context)
        paperStatCardsState = isPaperStatCardsEnabled(context)
        paperStatTearState = isPaperStatTearEnabled(context)
        pillGlowSubtleState = isPillGlowSubtleEnabled(context)
        heroTearSheetState = isHeroTearSheetEnabled(context)
        navPillButtonsState = isNavPillButtonsEnabled(context)
        homeTintState = isHomeTintEnabled(context)
        homeHeroTintState = isHomeHeroTintEnabled(context)
        homeTintFollowLaneState = isHomeTintFollowLaneEnabled(context)
        homeTintCategoryIdState = getHomeTintCategory(context)
        headerDeepState = isHeaderDeepEnabled(context)
        heroBlendGradientState = isHeroBlendGradientEnabled(context)
        threeDButtonState = is3DButtonGradientEnabled(context)
        reminderEnabledState = isReminderEnabled(context)
        tintWashEnabledState = isTintWashEnabled(context)
        entryMetaEnabledState = isEntryMetaEnabled(context)
        smartSpinLayoutState = isSmartSpinLayoutEnabled(context)
        smartDensityModeState = getSmartDensityMode(context)
        exploreSessionsEnabledState = isExploreSessionsEnabled(context)
        searchEngineState = getSearchEngine(context)
        musicServiceState = getMusicService(context)
        recycleBinExpiryDaysState = getRecycleBinExpiryDays(context)
        liveNotificationsEnabledState = isLiveNotificationsEnabled(context)
        overlayBubbleEnabledState = isOverlayBubbleEnabled(context)
        showBubbleOptInDialogState = isShowBubbleOptInDialog(context)
        overlayAskDeclinedState = isOverlayAskDeclined(context)
        voiceToTextEnabledState = isVoiceToTextEnabled(context)
        offlineModelIdState = getOfflineModelId(context)
        petEnabledState = isPetEnabled(context)
        floatingPetEnabledState = isFloatingPetEnabled(context)
        petBrainEnabledState = isPetBrainEnabled(context)
        petChatterState = getPetChatter(context)
        petGameFrequencyState = getPetGameFrequency(context)
        autoOpenRevealState = isAutoOpenReveal(context)
        customReactionLinesState = isCustomReactionLinesEnabled(context)
        pinnedTopicsState = getPinnedTopics(context)
        savedQuotesState = getSavedQuotes(context)
        topicSentimentsState = getTopicSentiments(context)
        hiddenCategoriesState = getHiddenCategories(context)
        categoryOrderState = getCategoryOrder(context)
        petDesignState = getPetDesign(context)
        customPetsState = getCustomPets(context)
        bedDesignRowsState = getBedDesignRows(context)
        evoPathState = getEvoPath(context)
        petPartTransformsState = isPetPartTransformsEnabled(context)
        updateCheckerEnabledState = isUpdateCheckerEnabled(context)
        autoBackupEnabledState = isAutoBackupEnabled(context)
    }

    // ── Theme mode (v81) ────────────────────────────────────────────
    /** The stored theme mode — "light" (default), "dark", or "system". */
    fun getThemeMode(context: Context): String =
        prefs(context).getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
        themeModeState = mode
    }

    /** Whether the app renders dark right now — resolves "system" via the
     *  device night flag (non-composable twin of [isCurioDarkTheme]). */
    fun isDarkTheme(context: Context): Boolean = when (getThemeMode(context)) {
        THEME_DARK -> true
        THEME_SYSTEM -> (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        else -> false
    }

    // ── Pastel color mode ───────────────────────────────────────────
    /** Whether the pastel color mode is on (default on). */
    fun isPastelColorsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PASTEL_COLORS_ENABLED, true)

    fun setPastelColorsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PASTEL_COLORS_ENABLED, enabled).apply()
        pastelColorsState = enabled
    }

    // ── v185 — proper M3 Material theme (opt-in, default OFF) ──────────
    /** Whether the proper M3 Material color theme is on (default off). */
    fun isMaterialThemeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MATERIAL_THEME, false)

    fun setMaterialThemeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MATERIAL_THEME, enabled).apply()
        materialThemeState = enabled
    }

    // ── Pastel crown depth (v7.12 experimental) ───────────────────────
    /** Whether pastel card gradients get a subtle 5% black deepen at the top (default on). */
    fun isPastelCrownDepthEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PASTEL_CROWN_DEPTH, true)

    fun setPastelCrownDepthEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PASTEL_CROWN_DEPTH, enabled).apply()
        pastelCrownDepthState = enabled
    }

    // ── Sky-azure hero (v27l) ─────────────────────────────────────────
    /** Whether the shared torn hero wears the airy pastel azure (v42: default ON). */
    fun isHeroBlueEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_BLUE, true)

    fun setHeroBlueEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_BLUE, enabled).apply()
        heroBlueState = enabled
    }

    // ── Hero follows the Spin lane (v30) ──────────────────────────────
    /** Whether the shared hero + page background follow the Spin lane's
     *  category (Appearance toggle, default off). */
    fun isHeroFollowLaneEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_FOLLOW_LANE, false)

    fun setHeroFollowLaneEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_FOLLOW_LANE, enabled).apply()
        heroFollowLaneState = enabled
    }

    // ── Dark-mode elevation (v28) ────────────────────────────────────
    /** Whether dark mode draws the soft light glow shadow (default off). */
    fun isDarkGlowEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK_GLOW, false)

    fun setDarkGlowEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_GLOW, enabled).apply()
        darkGlowState = enabled
    }

    // ── Promo/demo-content mode (v7.107 hidden) ───────────────────────
    /** Whether the hidden promo demo-content mode is on (default off). */
    fun isPromoModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PROMO_MODE, false)

    fun setPromoModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PROMO_MODE, enabled).apply()
        promoModeState = enabled
    }

    // ── Peek-deck redesign (v7.7 experimental) ────────────────────────
    /** Whether the top-lit gradient peek-card fill is on (default off). */
    fun isPeekGradientEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PEEK_GRADIENT, false)

    fun setPeekGradientEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PEEK_GRADIENT, enabled).apply()
        peekGradientState = enabled
    }

    /** Whether the category-tinted peek-card hairline is on (default off). */
    fun isPeekHairlineEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PEEK_HAIRLINE, false)

    fun setPeekHairlineEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PEEK_HAIRLINE, enabled).apply()
        peekHairlineState = enabled
    }

    /** Whether soft peek-card shadows are on (default off). */
    fun isPeekShadowsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PEEK_SHADOWS, false)

    fun setPeekShadowsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PEEK_SHADOWS, enabled).apply()
        peekShadowsState = enabled
    }

    /** Whether roomier two-line near-card titles are on (default off). */
    fun isPeekTitlesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PEEK_TITLES, false)

    fun setPeekTitlesEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PEEK_TITLES, enabled).apply()
        peekTitlesState = enabled
    }

    /** Whether the newer travel-then-tail-fade peek motion is on (default off). */
    fun isPeekTailFadeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PEEK_TAIL_FADE, false)

    fun setPeekTailFadeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PEEK_TAIL_FADE, enabled).apply()
        peekTailFadeState = enabled
    }

    // ── Main card (hero ticket) redesign (v7.13 experimental) ──────────
    /** Whether the enhanced hero-card gradient fill is on (v15 — default on). */
    fun isHeroGradientEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_GRADIENT, true)

    // ── Pet chatter + game frequency (v16) ─────────────────────────────
    /** Talkative / cozy / quiet — how often the pet speaks lines. */
    fun getPetChatter(context: Context): String =
        prefs(context).getString(KEY_PET_CHATTER, "cozy") ?: "cozy"

    fun setPetChatter(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_PET_CHATTER, mode).apply()
        petChatterState = mode
    }

    /** Relaxed / normal / eager — how often the pet starts games. */
    fun getPetGameFrequency(context: Context): String =
        prefs(context).getString(KEY_PET_GAME_FREQUENCY, "normal") ?: "normal"

    fun setPetGameFrequency(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_PET_GAME_FREQUENCY, mode).apply()
        petGameFrequencyState = mode
    }

    // ── Pet birthday (v16) — first-launch epoch day, set once ──────────
    /** The epoch day the app first ran — the pet's "hatch day". */
    fun petBirthdayEpochDay(context: Context): Long {
        val p = prefs(context)
        val existing = p.getLong(KEY_PET_BIRTHDAY, 0L)
        if (existing > 0L) return existing
        val today = java.util.Calendar.getInstance().timeInMillis / 86_400_000L
        p.edit().putLong(KEY_PET_BIRTHDAY, today).apply()
        return today
    }

    // ── Weekly-save memory (v16) — "you saved 3 songs this week" ───────
    /** Records a save in [categoryName]; counts roll within a 7-day window. */
    fun noteWeeklySave(context: Context, categoryName: String) {
        val p = prefs(context)
        val window = p.getLong(KEY_SAVES_WEEK_START, 0L)
        val today = java.util.Calendar.getInstance().timeInMillis / 86_400_000L
        if (window == 0L || today - window >= 7L) {
            p.edit()
                .putLong(KEY_SAVES_WEEK_START, today)
                .putString(KEY_SAVES_WEEK_COUNTS, "")
                .apply()
        }
        val raw = p.getString(KEY_SAVES_WEEK_COUNTS, "") ?: ""
        val counts = raw.split('|').filter { it.isNotEmpty() }.associate {
            val i = it.indexOf('=')
            if (i > 0) it.substring(0, i) to (it.substring(i + 1).toIntOrNull() ?: 0) else it to 1
        }.toMutableMap()
        counts[categoryName] = (counts[categoryName] ?: 0) + 1
        p.edit().putString(
            KEY_SAVES_WEEK_COUNTS,
            counts.entries.joinToString("|") { "${it.key}=${it.value}" }
        ).apply()
    }

    /** The top lane saved this week (name → count), for pet memory lines. */
    fun weeklySaveSummary(context: Context): List<Pair<String, Int>> {
        val p = prefs(context)
        val window = p.getLong(KEY_SAVES_WEEK_START, 0L)
        val today = java.util.Calendar.getInstance().timeInMillis / 86_400_000L
        if (window == 0L || today - window >= 7L) return emptyList()
        val raw = p.getString(KEY_SAVES_WEEK_COUNTS, "") ?: ""
        return raw.split('|').filter { it.isNotEmpty() }.mapNotNull { part ->
            val i = part.indexOf('=')
            if (i > 0) part.substring(0, i) to (part.substring(i + 1).toIntOrNull() ?: 0) else null
        }.sortedByDescending { it.second }.take(1)
    }

    fun setHeroGradientEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_GRADIENT, enabled).apply()
        heroGradientState = enabled
    }

    /** Whether the accent-tinted hero-card border is on (v10 — default true, promoted from experiment). */
    fun isHeroBorderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_BORDER, true)

    fun setHeroBorderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_BORDER, enabled).apply()
        heroBorderState = enabled
    }

    /** Whether the soft hero-card shadow is on — v92: the One UI tinted
     *  shadow is now the shipped default (was off); the Experiments toggle
     *  remains for comparison. */
    fun isHeroShadowEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_SHADOW, true)

    fun setHeroShadowEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_SHADOW, enabled).apply()
        heroShadowState = enabled
    }

    // ── Paper & header experiments (v27) ─────────────────────────────
    private const val KEY_PAPER_HEADER_CUTS = "paper_header_cuts"
    private const val KEY_PAPER_HEADER_HOLES = "paper_header_holes"
    private const val KEY_PAPER_HOLE_RINGS = "paper_hole_rings"
    private const val KEY_PAPER_HOLE_RING_STYLE = "paper_hole_ring_style"
    private const val KEY_PAPER_STAT_CARDS = "paper_stat_cards"
    private const val KEY_PILL_GLOW_SUBTLE = "pill_glow_subtle"
    private const val KEY_HERO_TEAR_SHEET = "hero_tear_sheet"
    private const val KEY_HOME_TINT = "home_tint"
    private const val KEY_HOME_HERO_TINT = "home_hero_tint"
    private const val KEY_HOME_TINT_FOLLOW_LANE = "home_tint_follow_lane"
    private const val KEY_HOME_TINT_CATEGORY = "home_tint_category"
    private const val KEY_PAPER_STAT_TEAR = "paper_stat_tear"
    private const val KEY_HEADER_DEEP = "header_deep"
    private const val KEY_NAV_PILL_BUTTONS = "nav_pill_buttons"

    /** Whether the header corner cut-lines + top-right ticks accent is on (experimental, default off). */
    fun isPaperHeaderCutsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAPER_HEADER_CUTS, false)

    fun setPaperHeaderCutsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAPER_HEADER_CUTS, enabled).apply()
        paperHeaderCutsState = enabled
    }

    /** Whether the stamped pin-hole column down the header's left edge is on (experimental, default off). */
    fun isPaperHeaderHolesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAPER_HEADER_HOLES, false)

    fun setPaperHeaderHolesEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAPER_HEADER_HOLES, enabled).apply()
        paperHeaderHolesState = enabled
    }

    /** Whether the pin holes wear 3D metal rings (experimental, default off). */
    fun isPaperHoleRingsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAPER_HOLE_RINGS, false)

    fun setPaperHoleRingsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAPER_HOLE_RINGS, enabled).apply()
        paperHoleRingsState = enabled
    }

    /** Which 3D ring style the pin holes wear: "coil" | "split" | "oblique" (default "coil"). */
    fun getPaperHoleRingStyle(context: Context): String =
        prefs(context).getString(KEY_PAPER_HOLE_RING_STYLE, "coil")?.takeIf { it in setOf("coil", "split", "oblique") }
            ?: "coil"

    fun setPaperHoleRingStyle(context: Context, style: String) {
        prefs(context).edit().putString(KEY_PAPER_HOLE_RING_STYLE, style).apply()
        paperHoleRingStyleState = style
    }

    /** Whether Home's background + bottom nav wear the category tint (experimental, default off). */
    fun isHomeTintEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HOME_TINT, true)

    fun setHomeTintEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HOME_TINT, enabled).apply()
        homeTintState = enabled
    }

    /** Whether the Spin Categories/Filter buttons wear the nav-pill look (experimental, default off). */
    fun isNavPillButtonsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_PILL_BUTTONS, false)

    fun setNavPillButtonsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_PILL_BUTTONS, enabled).apply()
        navPillButtonsState = enabled
    }

    /** Whether the Home quest hero ALSO wears the tint (experimental, default off). */
    fun isHomeHeroTintEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HOME_HERO_TINT, false)

    fun setHomeHeroTintEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HOME_HERO_TINT, enabled).apply()
        homeHeroTintState = enabled
    }

    /** Whether the Home tint follows the category picked on Spin (experimental, default off). */
    fun isHomeTintFollowLaneEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HOME_TINT_FOLLOW_LANE, false)

    fun setHomeTintFollowLaneEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HOME_TINT_FOLLOW_LANE, enabled).apply()
        homeTintFollowLaneState = enabled
    }

    /** The manually picked category for the Home tint (default wildcard). */
    fun getHomeTintCategory(context: Context): String =
        prefs(context).getString(KEY_HOME_TINT_CATEGORY, CategoryId.WILDCARD.name)
            ?: CategoryId.WILDCARD.name

    fun setHomeTintCategory(context: Context, id: CategoryId) {
        prefs(context).edit().putString(KEY_HOME_TINT_CATEGORY, id.name).apply()
        homeTintCategoryIdState = id.name
    }

    /** Whether the stat panes + Profile quests wear a solid paper card (experimental, v97: default ON). */
    fun isPaperStatCardsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAPER_STAT_CARDS, true)

    fun setPaperStatCardsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAPER_STAT_CARDS, enabled).apply()
        paperStatCardsState = enabled
    }

    /** Whether the dark-mode pill glow is the subtle top-only version (v101, default ON). */
    fun isPillGlowSubtleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PILL_GLOW_SUBTLE, true)

    fun setPillGlowSubtleEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PILL_GLOW_SUBTLE, enabled).apply()
        pillGlowSubtleState = enabled
    }

    /** Whether the stat paper card wears torn paper edges (extended tear on top; experimental, default off). */
    fun isPaperStatTearEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAPER_STAT_TEAR, false)

    fun setPaperStatTearEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAPER_STAT_TEAR, enabled).apply()
        paperStatTearState = enabled
    }

    /**
     * v108 — whether the torn heroes wear their white paper UNDER-SHEET
     * (the layered lip below the hero's own bottom tear). Default OFF: the
     * hero tears straight into the page. Turning it on restores the extra
     * paper layer for comparison (Settings → Experiments → Paper & headers).
     */
    fun isHeroTearSheetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_TEAR_SHEET, false)

    fun setHeroTearSheetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_TEAR_SHEET, enabled).apply()
        heroTearSheetState = enabled
    }

    /** Whether the torn-hero headers wear a slightly darker category accent (default ON). */
    fun isHeaderDeepEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HEADER_DEEP, true)

    fun setHeaderDeepEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HEADER_DEEP, enabled).apply()
        headerDeepState = enabled
    }

    // ── Dual-accent blend gradient (v10 toggle) ────────────────────────
    /** Whether the hero card wears the dual-accent blend gradient (default OFF). */
    fun isHeroBlendGradientEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_BLEND_GRADIENT, false)

    fun setHeroBlendGradientEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_BLEND_GRADIENT, enabled).apply()
        heroBlendGradientState = enabled
    }

    // ── 3D button gradient & shadow (v7.11 experimental) ───────────────
    /** Whether the Spin shuffle button wears a 3D radial gradient + shadow (default on). */
    fun is3DButtonGradientEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_3D_BUTTON_GRADIENT, true)

    fun set3DButtonGradientEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_3D_BUTTON_GRADIENT, enabled).apply()
        threeDButtonState = enabled
    }

    // ── Category tint wash ────────────────────────────────────────────
    /** Whether category-tinted page backgrounds are enabled (default on). */
    fun isTintWashEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TINT_WASH_ENABLED, true)

    /**
     * Whether category-tinted backgrounds are on. v78 — the theme-style
     * gate is gone (AMOLED/Material styles removed); the raw user toggle
     * is the only condition now.
     */
    fun tintWashEffective(): Boolean =
        tintWashEnabledState

    fun setTintWashEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TINT_WASH_ENABLED, enabled).apply()
        tintWashEnabledState = enabled
    }

    fun isEntryMetaEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENTRY_META_ENABLED, true)

    fun setEntryMetaEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENTRY_META_ENABLED, enabled).apply()
        entryMetaEnabledState = enabled
    }

    fun isSmartSpinLayoutEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SMART_SPIN_LAYOUT, false)

    fun setSmartSpinLayoutEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SMART_SPIN_LAYOUT, enabled).apply()
        smartSpinLayoutState = enabled
    }

    /**
     * The Spin density strength — see [SmartDensityMode].
     *
     * v7.4 — migrates the pre-v7.4 boolean switch on first read: true →
     * COMPACT, false → OFF. The legacy key is removed after migration so
     * the mode key becomes the single source of truth.
     */
    fun getSmartDensityMode(context: Context): SmartDensityMode {
        val prefs = prefs(context)
        val stored = prefs.getString(KEY_SMART_DENSITY_MODE, null)
        if (stored != null) {
            return runCatching { SmartDensityMode.valueOf(stored) }
                .getOrDefault(SmartDensityMode.OFF)
        }
        // v7.35 — the pre-picker legacy key defaults to OFF now, so a fresh
        // install (no stored mode) ships with density sizing off instead of
        // the old always-compact COMPACT. Users who explicitly picked a
        // mode keep their stored choice.
        val legacy = prefs.getBoolean(KEY_LEGACY_SMART_DENSITY_LAYOUT, false)
        val migrated = if (legacy) SmartDensityMode.COMPACT else SmartDensityMode.OFF
        prefs.edit()
            .putString(KEY_SMART_DENSITY_MODE, migrated.name)
            .remove(KEY_LEGACY_SMART_DENSITY_LAYOUT)
            .apply()
        return migrated
    }

    fun setSmartDensityMode(context: Context, mode: SmartDensityMode) {
        prefs(context).edit().putString(KEY_SMART_DENSITY_MODE, mode.name).apply()
        smartDensityModeState = mode
    }

    /**
     * v19 — the user's chosen explore search engine id, defaulting to
     * Google so existing behavior is unchanged until they switch.
     */
    fun getSearchEngine(context: Context): String =
        prefs(context).getString(KEY_SEARCH_ENGINE, null) ?: SearchEngine.GOOGLE.id

    fun setSearchEngine(context: Context, engine: SearchEngine) {
        prefs(context).edit().putString(KEY_SEARCH_ENGINE, engine.id).apply()
        searchEngineState = engine.id
    }

    /**
     * v27s — the user's chosen music service id for the "Watch in" explore
     * action, defaulting to YouTube Music so existing behavior is unchanged
     * until they switch.
     */
    fun getMusicService(context: Context): String =
        prefs(context).getString(KEY_MUSIC_SERVICE, null) ?: MusicService.YOUTUBE.id

    fun setMusicService(context: Context, service: MusicService) {
        prefs(context).edit().putString(KEY_MUSIC_SERVICE, service.id).apply()
        musicServiceState = service.id
    }

    /** v27 — recycle-bin retention window in days (0 = keep forever). */
    fun getRecycleBinExpiryDays(context: Context): Int =
        prefs(context).getInt(KEY_RECYCLE_BIN_EXPIRY_DAYS, DEFAULT_RECYCLE_BIN_EXPIRY_DAYS)

    fun setRecycleBinExpiryDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_RECYCLE_BIN_EXPIRY_DAYS, days).apply()
        recycleBinExpiryDaysState = days
    }

    /** Whether the explore-session flow (timer/reminder/done prompt) is on. */
    fun isExploreSessionsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EXPLORE_SESSIONS_ENABLED, true)

    fun setExploreSessionsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_EXPLORE_SESSIONS_ENABLED, enabled).apply()
        exploreSessionsEnabledState = enabled
        if (!enabled) {
            // Turning the feature off mid-session: tear the live session
            // down so the timer, reminder and done-prompt all stop.
            ExploreSessionStore.clearSession(context)
            ExploreSessionStore.clearQueued(context)
            ExploreReminderScheduler.cancel(context)
            com.curio.app.infrastructure.ExploreSessionService.stop(context)
        }
    }

    /**
     * Whether the persistent live explore notification is on. Default ON.
     * Off = no ongoing notification; the end reminder + bubble stay.
     */
    fun isLiveNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LIVE_NOTIFICATIONS_ENABLED, true)

    fun setLiveNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LIVE_NOTIFICATIONS_ENABLED, enabled).apply()
        liveNotificationsEnabledState = enabled
        val session = ExploreSessionStore.getActiveSession(context) ?: return
        if (enabled) {
            // Flipped ON mid-session: bring the live notification back for
            // the currently active session (the bubble stays if wanted).
            com.curio.app.infrastructure.ExploreSessionService.start(context, session)
        } else {
            // Flipped OFF mid-session: drop the chronometer notification.
            // Keep the service alive when the floating bubble still wants it
            // (it swaps to the minimal bubble-active notification); otherwise
            // stop it — the session + reminder survive either way.
            if (isOverlayBubbleEnabled(context) && overlayActuallyUsable(context)) {
                com.curio.app.infrastructure.ExploreSessionService.sync(context)
            } else {
                com.curio.app.infrastructure.ExploreSessionService.stop(context)
            }
        }
    }

    /**
     * v23 — whether the Explore now dialog shows its "Show the explore
     * bubble" opt-in row. Default OFF (hidden); the Notifications toggle
     * re-shows it as a single-line choice inside the dialog.
     */
    fun isShowBubbleOptInDialog(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_BUBBLE_OPT_IN_DIALOG, false)

    fun setShowBubbleOptInDialog(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_BUBBLE_OPT_IN_DIALOG, enabled).apply()
        showBubbleOptInDialogState = enabled
    }

    /**
     * Whether the floating explore bubble is on. v22 — default OFF: the
     * bubble is opt-in (the explore dialog's "Show the explore bubble"
     * switch, or the Settings toggle). Off = the timer lives only in the
     * notification (when live notifications are on).
     */
    fun isOverlayBubbleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERLAY_BUBBLE_ENABLED, false)

    fun setOverlayBubbleEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_OVERLAY_BUBBLE_ENABLED, enabled).apply()
        overlayBubbleEnabledState = enabled
        val session = ExploreSessionStore.getActiveSession(context) ?: return
        if (enabled) {
            // Flipped ON mid-session: bring the bubble back (permission must
            // be granted — callers gate on it; the render decides).
            if (overlayActuallyUsable(context)) {
                com.curio.app.infrastructure.ExploreSessionService.start(context, session)
            }
        } else {
            // Flipped OFF mid-session: drop the bubble. Keep the service
            // alive when live notifications still want it, else stop it.
            if (isLiveNotificationsEnabled(context)) {
                com.curio.app.infrastructure.ExploreSessionService.sync(context)
            } else {
                com.curio.app.infrastructure.ExploreSessionService.stop(context)
            }
        }
    }

    /**
     * Whether AUTOMATIC "Display over other apps" prompts are suppressed
     * (v8.1) — true once the user declines the permission (dismisses the
     * explore-start prompt or returns from system settings without
     * granting). The explore flow then proceeds without the bubble, and the
     * only way back in is the explicit Settings toggle, which always opens
     * the system page and clears this flag.
     */
    fun isOverlayAskDeclined(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERLAY_ASK_DECLINED, false)

    fun setOverlayAskDeclined(context: Context, declined: Boolean) {
        prefs(context).edit().putBoolean(KEY_OVERLAY_ASK_DECLINED, declined).apply()
        overlayAskDeclinedState = declined
    }

    /** Whether experimental voice-to-text is enabled (default OFF). */
    fun isVoiceToTextEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VOICE_TO_TEXT_ENABLED, false)

    fun setVoiceToTextEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_VOICE_TO_TEXT_ENABLED, enabled).apply()
        voiceToTextEnabledState = enabled
    }

    // ── Offline transcription model (v125) ─────────────────────────────
    /** The selected offline model id ("" = none). */
    fun getOfflineModelId(context: Context): String =
        prefs(context).getString(KEY_OFFLINE_MODEL, "").orEmpty()

    fun setOfflineModelId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_OFFLINE_MODEL, id).apply()
        offlineModelIdState = id
    }

    /** Forces screens that read [VoskModels.isDownloaded] to re-check. */
    fun bumpOfflineModelVersion() {
        offlineModelVersionState++
    }

    /**
     * Whether the explore foreground service should run: live notifications
     * ON, OR the floating bubble is enabled AND the "Display over other
     * apps" permission is actually usable (the overlay is what needs the
     * service when live notifications are off). [overlayActuallyUsable]
     * — not raw [Settings.canDrawOverlays] — so an Android 15+ pending
     * grant never starts a service that has nothing it can show.
     */
    fun exploreServiceShouldRun(context: Context): Boolean =
        isExploreSessionsEnabled(context) && (
            isLiveNotificationsEnabled(context) ||
                (isOverlayBubbleEnabled(context) && overlayActuallyUsable(context))
            )

    /**
     * Whether the "Display over other apps" overlay is ACTUALLY usable right
     * now. [Settings.canDrawOverlays] alone can lie on Android 15+ (v7.35):
     * a FIRST-TIME grant — which includes a grant right after clearing app
     * data or reinstalling — can sit in the system's PENDING state, where
     * canDrawOverlays() returns true but overlay windows are silently not
     * shown. The AppOps mode is the source of truth in that state (it stays
     * MODE_IGNORED until the permission settles), so treat it as not
     * granted and let the permission prompts re-ask — toggling the special
     * access off/on in the system page resolves the pending state.
     */
    @Suppress("DEPRECATION") // AppOps has no stable non-deprecated public check API across API levels
    fun overlayActuallyUsable(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return true
        return runCatching {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(true)
    }


    // ── Pinned topics (Topic Reveal → "Pin for later") ─────────────────
    /**
     * Returns all pinned topics, newest first. Persisted as a JSON array so
     * topic names with delimiters survive round-trips.
     */
    fun getPinnedTopics(context: Context): List<PinnedTopic> {
        val raw = prefs(context).getString(KEY_PINNED_TOPICS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val id = obj.optString("categoryId")
                val cat = CategoryId.values().firstOrNull { it.name == id } ?: return@List null
                PinnedTopic(
                    categoryId = cat,
                    topicName = obj.optString("topicName"),
                    pinnedAtMillis = obj.optLong("pinnedAtMillis", System.currentTimeMillis())
                )
            }.filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isTopicPinned(context: Context, categoryId: CategoryId, topicName: String): Boolean =
        getPinnedTopics(context).any {
            it.categoryId == categoryId && it.topicName == topicName
        }

    /** Pins a topic (newest first, deduped). No-op when already pinned. */
    fun pinTopic(context: Context, categoryId: CategoryId, topicName: String) {
        if (topicName.isBlank()) return
        val current = getPinnedTopics(context)
        if (current.any { it.categoryId == categoryId && it.topicName == topicName }) return
        val updated = listOf(PinnedTopic(categoryId, topicName, System.currentTimeMillis())) + current
        savePinnedTopics(context, updated)
        CurioQuests.onTopicPinned(context)
    }

    fun unpinTopic(context: Context, categoryId: CategoryId, topicName: String) {
        val updated = getPinnedTopics(context).filterNot {
            it.categoryId == categoryId && it.topicName == topicName
        }
        savePinnedTopics(context, updated)
    }

    private fun savePinnedTopics(context: Context, topics: List<PinnedTopic>) {
        val arr = JSONArray()
        topics.forEach {
            arr.put(
                JSONObject()
                    .put("categoryId", it.categoryId.name)
                    .put("topicName", it.topicName)
                    .put("pinnedAtMillis", it.pinnedAtMillis)
            )
        }
        prefs(context).edit().putString(KEY_PINNED_TOPICS, arr.toString()).apply()
        pinnedTopicsState = topics
    }

    // ── Saved quotes (saved entry → bookmark icon on quote cards) ───────
    /**
     * Returns all bookmarked quotes, newest first. Persisted as a JSON
     * array (same pattern as [PinnedTopic]) so quote text with any
     * characters survives round-trips.
     */
    fun getSavedQuotes(context: Context): List<SavedQuote> {
        val raw = prefs(context).getString(KEY_SAVED_QUOTES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val id = obj.optString("categoryId")
                val cat = CategoryId.values().firstOrNull { it.name == id } ?: return@List null
                SavedQuote(
                    entryId = obj.optString("entryId"),
                    topicName = obj.optString("topicName"),
                    categoryId = cat,
                    quoteText = obj.optString("quoteText"),
                    savedAtMillis = obj.optLong("savedAtMillis", System.currentTimeMillis())
                )
            }.filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Bookmarks a quote (newest first, deduped by entry + quote text).
     * No-op when already saved. (The saved/unsaved check in the UI reads
     * the reactive [savedQuotesState] directly, not prefs.)
     */
    fun saveQuote(context: Context, entryId: String, topicName: String, categoryId: CategoryId, quoteText: String) {
        if (entryId.isBlank() || quoteText.isBlank()) return
        val current = getSavedQuotes(context)
        if (current.any { it.entryId == entryId && it.quoteText == quoteText }) return
        val updated = listOf(
            SavedQuote(entryId, topicName, categoryId, quoteText, System.currentTimeMillis())
        ) + current
        saveSavedQuotes(context, updated)
        CurioQuests.onQuoteSaved(context)
    }

    fun removeSavedQuote(context: Context, entryId: String, quoteText: String) {
        val updated = getSavedQuotes(context).filterNot {
            it.entryId == entryId && it.quoteText == quoteText
        }
        saveSavedQuotes(context, updated)
    }

    private fun saveSavedQuotes(context: Context, quotes: List<SavedQuote>) {
        val arr = JSONArray()
        quotes.forEach {
            arr.put(
                JSONObject()
                    .put("entryId", it.entryId)
                    .put("topicName", it.topicName)
                    .put("categoryId", it.categoryId.name)
                    .put("quoteText", it.quoteText)
                    .put("savedAtMillis", it.savedAtMillis)
            )
        }
        prefs(context).edit().putString(KEY_SAVED_QUOTES, arr.toString()).apply()
        savedQuotesState = quotes
    }

    // ── Topic sentiment (Topic Reveal → like/dislike feeds the shuffle) ──
    /**
     * Returns all topic sentiments keyed "CATEGORY:topicId" → "like" /
     * "dislike". Persisted as a JSON object so any topic id survives.
     */
    fun getTopicSentiments(context: Context): Map<String, String> {
        val raw = prefs(context).getString(KEY_TOPIC_SENTIMENTS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap { obj.keys().forEach { key -> put(key, obj.optString(key)) } }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Reactive sentiment for a topic — null / [SENTIMENT_LIKE] / [SENTIMENT_DISLIKE]. */
    fun topicSentiment(categoryId: CategoryId, topicId: String): String? =
        topicSentimentsState["${categoryId.name}:$topicId"]

    /** Sets or clears a topic's sentiment ([SENTIMENT_NONE] removes it). */
    fun setTopicSentiment(context: Context, categoryId: CategoryId, topicId: String, sentiment: String) {
        if (topicId.isBlank()) return
        val key = "${categoryId.name}:$topicId"
        val updated = getTopicSentiments(context).toMutableMap()
        if (sentiment == SENTIMENT_NONE) updated.remove(key) else updated[key] = sentiment
        saveTopicSentiments(context, updated)
        // Feed the quests system — like/dislike votes power the daily + badges.
        when (sentiment) {
            SENTIMENT_LIKE -> CurioQuests.onTopicLiked(context)
            SENTIMENT_DISLIKE -> CurioQuests.onTopicDisliked(context)
        }
    }

    private fun saveTopicSentiments(context: Context, sentiments: Map<String, String>) {
        val obj = JSONObject()
        sentiments.forEach { (k, v) -> obj.put(k, v) }
        prefs(context).edit().putString(KEY_TOPIC_SENTIMENTS, obj.toString()).apply()
        topicSentimentsState = sentiments
    }

    /**
     * Net like-minus-dislike count per CATEGORY name — drives the shuffle's
     * category factor (a category with more likes shows more, more dislikes
     * shows less, never zero). Computed from the reactive state.
     */
    fun categoryAffinityMap(): Map<String, Int> {
        val acc = mutableMapOf<String, Int>()
        topicSentimentsState.forEach { (key, sentiment) ->
            val catName = key.substringBefore(':')
            val delta = when (sentiment) {
                SENTIMENT_LIKE -> 1
                SENTIMENT_DISLIKE -> -1
                else -> 0
            }
            if (delta != 0) acc[catName] = (acc[catName] ?: 0) + delta
        }
        return acc
    }

    // ── Manage Categories (v7.94) — hidden set + custom order ──────────
    /** Whether [id] is hidden by the user (Manage Categories). */
    fun isCategoryHidden(id: CategoryId): Boolean = id in hiddenCategoriesState

    /**
     * The user's hidden category set, persisted as comma-joined names.
     * Unknown names are dropped defensively on read.
     */
    fun getHiddenCategories(context: Context): Set<CategoryId> {
        val raw = prefs(context).getString(KEY_HIDDEN_CATEGORIES, null) ?: return emptySet()
        return raw.split(",").mapNotNull { name ->
            CategoryId.values().firstOrNull { it.name == name }
        }.toSet()
    }

    /** Hides or un-hides [id]; updates the reactive state instantly. */
    fun setCategoryHidden(context: Context, id: CategoryId, hidden: Boolean) {
        val updated = if (hidden) hiddenCategoriesState + id else hiddenCategoriesState - id
        prefs(context).edit()
            .putString(KEY_HIDDEN_CATEGORIES, updated.joinToString(",") { it.name })
            .apply()
        hiddenCategoriesState = updated
    }

    /** The user's custom category order (empty = default). */
    fun getCategoryOrder(context: Context): List<CategoryId> {
        val raw = prefs(context).getString(KEY_CATEGORY_ORDER, null) ?: return emptyList()
        return raw.split(",").mapNotNull { name ->
            CategoryId.values().firstOrNull { it.name == name }
        }
    }

    /** Persists a full custom order; empty restores the default order. */
    fun setCategoryOrder(context: Context, order: List<CategoryId>) {
        val valid = order.distinct().filter { it in CategoryId.values().asList() }
        prefs(context).edit()
            .putString(KEY_CATEGORY_ORDER, valid.joinToString(",") { it.name })
            .apply()
        categoryOrderState = valid
    }

    // ── Curio pet companion (v8.5) ───────────────────────────────────
    /** Whether the Curio pet companion layer is on (default ON). */
    fun isPetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PET_ENABLED, true)

    fun setPetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PET_ENABLED, enabled).apply()
        petEnabledState = enabled
    }

    // v8.8 — floating pet toggle (default ON; see the Appearance settings).
    fun isFloatingPetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FLOATING_PET_ENABLED, true)

    fun setFloatingPetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FLOATING_PET_ENABLED, enabled).apply()
        floatingPetEnabledState = enabled
    }

    // v8.43 — the pet's learning brain toggle (default ON; Appearance).
    fun isPetBrainEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PET_BRAIN_ENABLED, true)

    fun setPetBrainEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PET_BRAIN_ENABLED, enabled).apply()
        petBrainEnabledState = enabled
    }

    // ── Pet designer recent colors (v8.47 color picker) ────────────────
    /** Recently-applied palette colors, most recent first (max 12). */
    fun getPetRecentColors(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_PET_RECENT_COLORS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getString(it) }.filter { it.length == 6 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setPetRecentColors(context: Context, colors: List<String>) {
        val arr = JSONArray()
        colors.take(12).forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_PET_RECENT_COLORS, arr.toString()).apply()
    }

    // v8.16 — auto-open the landed topic's reveal after a spin. v8.21 —
    // default ON (the reveal opens as soon as the deck settles).
    fun isAutoOpenReveal(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_OPEN_REVEAL, true)

    fun setAutoOpenReveal(context: Context, enabled: Boolean) {
        autoOpenRevealState = enabled
        prefs(context).edit().putBoolean(KEY_AUTO_OPEN_REVEAL, enabled).apply()
    }

    /** Whether Curie may speak the custom reaction lines saved in the Pet designer. */
    fun isCustomReactionLinesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CUSTOM_REACTION_LINES, false)

    fun setCustomReactionLinesEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CUSTOM_REACTION_LINES, enabled).apply()
        customReactionLinesState = enabled
    }

    // ── Daily reminder ───────────────────────────────────────────────
    fun isReminderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMINDER_ENABLED, false)

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        reminderEnabledState = enabled
        prefs(context).edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        if (enabled) {
            DailyReminderScheduler.schedule(context, getReminderHour(context))
        } else {
            DailyReminderScheduler.cancel(context)
        }
    }

    fun getReminderHour(context: Context): Int =
        prefs(context).getInt(KEY_REMINDER_HOUR, 18)   // default 6 PM

    fun setReminderHour(context: Context, hour: Int) {
        val safeHour = hour.coerceIn(0, 23)
        prefs(context).edit().putInt(KEY_REMINDER_HOUR, safeHour).apply()
        if (isReminderEnabled(context)) {
            DailyReminderScheduler.schedule(context, safeHour)
        }
    }

    // ── Last-used Spin category — persisted so the Spin tab opens where ��
    //    the user left off, even across app launches (v5.5). Falls back
    //    to WILDCARD when unset or when a stored name no longer exists.
    fun getLastSpinCategory(context: Context): CategoryId {
        val name = prefs(context).getString(KEY_LAST_SPIN_CATEGORY, null)
        return name?.let { n ->
            CategoryId.values().firstOrNull { it.name == n }
        } ?: CategoryId.WILDCARD
    }

    fun setLastSpinCategory(context: Context, id: CategoryId) =
        prefs(context).edit().putString(KEY_LAST_SPIN_CATEGORY, id.name).apply()

    /**
     * Full last-used Spin category SET (single or mixed multi-select) —
     * persisted so the Shuffle tab reopens the same deck after back
     * navigation, tab switches and app restarts. The single-category key
     * is kept in sync with the first entry for backwards compat with
     * [getLastSpinCategory].
     */
    fun getLastSpinCategories(context: Context): List<CategoryId> {
        val raw = prefs(context).getString(KEY_LAST_SPIN_CATEGORIES, null)
        val ids = raw
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { name -> CategoryId.values().firstOrNull { it.name == name } }
            .orEmpty()
        return if (ids.isNotEmpty()) ids else listOf(getLastSpinCategory(context))
    }

    fun setLastSpinCategories(context: Context, ids: List<CategoryId>) {
        val names = ids.map { it.name }.distinct()
        if (names.isEmpty()) return
        prefs(context).edit().putString(KEY_LAST_SPIN_CATEGORIES, names.joinToString(",")).apply()
        setLastSpinCategory(context, ids.first())
    }

    // ── Custom pet design (v8.34 — Pet designer playground) ──────────
    /** The saved custom pet-design text, or null for the default look. */
    fun getPetDesign(context: Context): String? =
        prefs(context).getString(KEY_PET_DESIGN, null)

    /** Saves a custom pet design (always-on: the pet wears it everywhere). */
    fun setPetDesign(context: Context, text: String) {
        prefs(context).edit().putString(KEY_PET_DESIGN, text).apply()
        petDesignState = text
    }

    /** Removes the custom design, returning the pet to its default look. */
    fun clearPetDesign(context: Context) {
        prefs(context).edit().remove(KEY_PET_DESIGN).apply()
        petDesignState = null
    }

    // ── Custom pet slots (v8.56 — Pet studio Pets page) ──────────────
    /** The two saved custom-pet design texts (null = empty slot). */
    fun getCustomPets(context: Context): List<String?> = listOf(
        prefs(context).getString(KEY_PET_CUSTOM_1, null),
        prefs(context).getString(KEY_PET_CUSTOM_2, null)
    )

    /** Saves a custom-pet design into one of the two slots (always-on). */
    fun setCustomPet(context: Context, index: Int, text: String) {
        val key = if (index == 0) KEY_PET_CUSTOM_1 else KEY_PET_CUSTOM_2
        prefs(context).edit().putString(key, text).apply()
        customPetsState = getCustomPets(context)
    }

    /** Removes one custom-pet slot (returns it to the empty state). */
    fun clearCustomPet(context: Context, index: Int) {
        val key = if (index == 0) KEY_PET_CUSTOM_1 else KEY_PET_CUSTOM_2
        prefs(context).edit().remove(key).apply()
        customPetsState = getCustomPets(context)
    }


    // ── Landed Spin topic (per category) — persisted so the landed card ──
    //    survives ANY navigation. rememberSaveable alone dies when the
    //    Spin back-stack entry is popped (e.g. top-bar back arrow to
    //    Home); mirroring the topic name here lets Spin restore it the
    //    next time it's composed. Cleared when a new spin starts.
    fun getLandedTopic(context: Context, categoryId: CategoryId): String? =
        prefs(context).getString(KEY_LANDED_TOPIC_PREFIX + categoryId.name, null)

    fun setLandedTopic(context: Context, categoryId: CategoryId, topicName: String?) {
        prefs(context).edit()
            .putString(KEY_LANDED_TOPIC_PREFIX + categoryId.name, topicName)
            .apply()
    }

    // ── Legacy flower-bed design compatibility ───────────────────────
    /** Returns old saved bed rows for migration compatibility; no UI writes them. */
    fun getBedDesignRows(context: Context): List<String>? =
        prefs(context).getString(KEY_BED_DESIGN, null)
            ?.split("\n")
            ?.takeIf { it.size == 18 && it.all { row -> row.length == 32 } }

    fun setBedDesignRows(context: Context, rows: List<String>) {
        prefs(context).edit().putString(KEY_BED_DESIGN, rows.joinToString("\n")).apply()
        bedDesignRowsState = rows
    }

    fun clearBedDesignRows(context: Context) {
        prefs(context).edit().remove(KEY_BED_DESIGN).apply()
        bedDesignRowsState = null
    }

    // ── Evolution path (v9.5) ────────────────────────────────────────
    private const val KEY_EVO_PATH = "evo_path"

    fun getEvoPath(context: Context): String? =
        prefs(context).getString(KEY_EVO_PATH, null)

    /** Reactive read (no context needed) — returns null when unset. */
    fun evoPath(): CurioPet.EvoPath? =
        evoPathState?.let { runCatching { CurioPet.EvoPath.valueOf(it) }.getOrNull() }

    fun setEvoPath(context: Context, path: CurioPet.EvoPath) {
        prefs(context).edit().putString(KEY_EVO_PATH, path.name).apply()
        evoPathState = path.name
    }

    // ── Pet editor experiments (v9.6) ────────────────────────────────
    private const val KEY_PET_PART_TRANSFORMS = "pet_part_transforms"

    fun isPetPartTransformsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PET_PART_TRANSFORMS, false)

    fun setPetPartTransformsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PET_PART_TRANSFORMS, enabled).apply()
        petPartTransformsState = enabled
    }

    // ── Update checker (opt-in) ──────────────────────────────────────
    // Curio is an offline-first app: the background update check (and its
    // notification/toast) costs data every launch. OFF by default — the
    // Updates page's toggle opts in. The MANUAL check on the Updates page
    // always works regardless of this flag.
    private const val KEY_UPDATE_CHECKER_ENABLED = "update_checker_enabled"

    var updateCheckerEnabledState by mutableStateOf(false)
        private set

    fun isUpdateCheckerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_UPDATE_CHECKER_ENABLED, false)

    fun setUpdateCheckerEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_UPDATE_CHECKER_ENABLED, enabled).apply()
        updateCheckerEnabledState = enabled
    }

    // ── Auto backup (pick a folder once) ─────────────────────────────
    // The user picks a save location ONCE (CreateDocument); the chosen
    // document URI is persisted with a persistable permission grant so a
    // background auto-backup can write to it without re-asking.
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_AUTO_BACKUP_URI = "auto_backup_uri"
    private const val KEY_AUTO_BACKUP_LAST_AT = "auto_backup_last_at"

    var autoBackupEnabledState by mutableStateOf(false)
        private set

    fun isAutoBackupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, false)

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
        autoBackupEnabledState = enabled
    }

    /** The persisted auto-backup destination URI string, or "" when unset. */
    fun getAutoBackupUri(context: Context): String =
        prefs(context).getString(KEY_AUTO_BACKUP_URI, "").orEmpty()

    fun setAutoBackupUri(context: Context, uri: String) {
        prefs(context).edit().putString(KEY_AUTO_BACKUP_URI, uri).apply()
    }

    /** Milliseconds of the last AUTO backup run, or 0 if never. */
    fun getAutoBackupLastAtMillis(context: Context): Long =
        prefs(context).getLong(KEY_AUTO_BACKUP_LAST_AT, 0L)

    fun setAutoBackupLastAtMillis(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_AUTO_BACKUP_LAST_AT, millis).apply()
    }

    // ── Internal ─────────────────────────────────────────────────────
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}

/**
 * A topic the user pinned on the Topic Reveal screen so they can revisit it
 * later (listed under "Pinned for later" in Topic History).
 */
data class PinnedTopic(
    val categoryId: CategoryId,
    val topicName: String,
    val pinnedAtMillis: Long
)

/**
 * A quote the user bookmarked from a saved entry (bookmark icon on the
 * quote card in the entry detail view). Listed on the Home screen's
 * "Saved" shelf together with [PinnedTopic]s.
 */
data class SavedQuote(
    val entryId: String,
    val topicName: String,
    val categoryId: CategoryId,
    val quoteText: String,
    val savedAtMillis: Long
)
