package com.curio.app.data

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
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

    /** Theme mode constants used across ProfileScreen, SettingsScreen, CurioTheme. */
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    /**
     * Theme style constants — the visual identity Curio wears:
     *  - [THEME_STYLE_DEFAULT] — Curio's warm cream palette + category tints.
     *  - [THEME_STYLE_AMOLED] — forced dark, pure-black surfaces, tints off.
     *  - [THEME_STYLE_MATERIAL] — the device's Material dynamic colors for
     *    surfaces/backgrounds/controls; category accents stay the true
     *    researched colors (the old ~40% blend toward the dynamic primary
     *    made reds turn purple and teals turn olive — dropped), tints off.
     */
    const val THEME_STYLE_DEFAULT = "default"
    const val THEME_STYLE_AMOLED = "amoled"
    const val THEME_STYLE_MATERIAL = "material"

    /** Topic sentiment constants — like/dislike from Topic Reveal feeds the
     *  Spin shuffle weighting (liked topics + their category get more weight,
     *  disliked get less — never fully blocked). [SENTIMENT_NONE] clears. */
    const val SENTIMENT_LIKE = "like"
    const val SENTIMENT_DISLIKE = "dislike"
    const val SENTIMENT_NONE = "none"

    private const val NAME = "curio_app_prefs"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_THEME_MODE = "theme_mode"        // "light", "dark", "system"
    private const val KEY_THEME_STYLE = "theme_style"      // "default", "amoled", "material"
    private const val KEY_PASTEL_COLORS_ENABLED = "pastel_colors_enabled"
    private const val KEY_PASTEL_CROWN_DEPTH = "pastel_crown_depth"
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
    // v7.13 — Main card (hero ticket) redesign toggles: enhanced gradient
    // fill, accent border, soft shadow. All OFF by default so the current
    // hero card stays exactly as-is until enabled. (Enhanced typography was
    // promoted to the shipped default in v7.16 — no longer toggleable.)
    private const val KEY_HERO_GRADIENT = "hero_gradient"
    private const val KEY_HERO_BORDER = "hero_border"
    private const val KEY_HERO_SHADOW = "hero_shadow"
    private const val KEY_MATERIAL_CARD_BLENDS = "material_card_blends"
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
    // v8.1 — "don't nag" flag: once the user declines the "Display over
    // other apps" permission (dismisses the prompt or returns from system
    // settings without granting), all AUTOMATIC overlay prompts are
    // suppressed until they explicitly try to enable the bubble from
    // Settings (the Settings toggle clears it).
    private const val KEY_OVERLAY_ASK_DECLINED = "overlay_ask_declined"
    // Experimental voice-to-text/dictation. Default OFF so microphone
    // transcription never appears or starts until the user opts in.
    private const val KEY_VOICE_TO_TEXT_ENABLED = "voice_to_text_enabled"
    private const val KEY_GUIDE_ENABLED = "guide_enabled"
    // v8.5 — the Curio pet companion (spec §10): the pixel pet + its
    // rule-based dialogue + the category passport/discovery features on
    // Quests and Home. Default ON; a user-facing Appearance toggle gates
    // the whole companion layer so it can be A/B'd and reverted without a
    // code change (per the experiment rules, the toggle is removed once the
    // companion design is decided).
    private const val KEY_PET_ENABLED = "pet_enabled"
    private const val KEY_FLOATING_PET_ENABLED = "floating_pet_enabled"
    // v8.2 — the quest tour's one-time offer: once the user has taken OR
    // declined it ("No, thanks" / dismissing the prompt), the offer never
    // reappears and the first quest navigates normally.
    private const val KEY_GUIDE_TOUR_OFFERED = "guide_tour_offered"
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

    // ── Display name ─────────────────────────────────────────────────
    fun getDisplayName(context: Context): String =
        prefs(context).getString(KEY_DISPLAY_NAME, null) ?: "Curious Explorer"

    fun setDisplayName(context: Context, name: String) =
        prefs(context).edit().putString(KEY_DISPLAY_NAME, name).apply()

    /**
     * Reactive theme mode state — updated by [setThemeMode] so [CurioTheme]
     * recomposes instantly when the user toggles the theme in settings.
     * Call [initThemeMode] once at app startup to seed it from prefs.
     */
    var themeModeState by mutableStateOf(THEME_SYSTEM)
        private set

    /**
     * Reactive theme-style state — updated by [setThemeStyle] so [CurioTheme]
     * (and every theme-aware helper) recomposes instantly when the user picks
     * a style in Settings. Seeded from prefs in [initThemeMode].
     */
    var themeStyleState by mutableStateOf(THEME_STYLE_DEFAULT)
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

    // Pastel crown depth (v7.12, EXPERIMENTAL) — when pastel mode is ON
    // and this toggle is ON, the top of pastel card gradients gets a
    // subtle 5% black deepen so every card reads with a gentle darker
    // crown for depth instead of a uniform pastel from edge to edge.
    // Default ON. Only takes effect when pastel mode is active.
    var pastelCrownDepthState by mutableStateOf(true)
        private set

    // Hidden promo/demo-content mode (v7.107) — unlocked by tapping the
    // Version row in Support & diagnostics five times (tap five times
    // again to turn it OFF). While ON, the app shows promotional sample
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

    // Main card (hero ticket) redesign (v7.13, EXPERIMENTAL) — the Spin
    // deck's front hero card wears four independently-toggleable upgrades:
    // an enhanced top-lit gradient fill, an accent-tinted border, a soft
    // ambient shadow, and enhanced typography (bolder title, bigger
    // subtitle). Each defaults OFF; the current hero card stays unchanged
    // until the experiment settles.
    var heroGradientState by mutableStateOf(false)
        private set
    var heroBorderState by mutableStateOf(false)
        private set
    var heroShadowState by mutableStateOf(false)
        private set

    // Material card blends (v7.8, EXPERIMENTAL) — when ON, cards in the
    // Material theme style wear a MIXED gradient of the category accent + the
    // device's dynamic Material colors (primary / secondary / tertiary) in the
    // same multi-stop style as the mixed deck, so the card reads as a category
    // × device blend. Default ON (the Material style's headline look); only
    // takes effect while the Material style is active. When the experiment
    // settles, hardcode the winner and remove the toggle.
    var materialCardBlendsState by mutableStateOf(true)
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
    var overlayBubbleEnabledState by mutableStateOf(true)
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

    /**
     * Guided-tour state (v8.0) — the MASTER SWITCH for the one-time quest
     * tour: when ON (default), the Quests page offers the tap-along tour the
     * first time the user taps the first quest (see [isGuideTourOffered]).
     * Toggleable in Settings; the tour is never auto-shown from other
     * screens (v8.2).
     */
    var guideEnabledState by mutableStateOf(true)
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

    // v8.2 — whether the one-time tour offer has been shown (taken or
    // declined). Suppresses the offer on the Quests page so it never nags.
    var guideTourOfferedState by mutableStateOf(false)
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

    fun initThemeMode(context: Context) {
        themeModeState = getThemeMode(context)
        themeStyleState = getThemeStyle(context)
        pastelColorsState = isPastelColorsEnabled(context)
        pastelCrownDepthState = isPastelCrownDepthEnabled(context)
        promoModeState = isPromoModeEnabled(context)
        peekGradientState = isPeekGradientEnabled(context)
        peekHairlineState = isPeekHairlineEnabled(context)
        peekShadowsState = isPeekShadowsEnabled(context)
        peekTitlesState = isPeekTitlesEnabled(context)
        heroGradientState = isHeroGradientEnabled(context)
        heroBorderState = isHeroBorderEnabled(context)
        heroShadowState = isHeroShadowEnabled(context)
        materialCardBlendsState = isMaterialCardBlendsEnabled(context)
        threeDButtonState = is3DButtonGradientEnabled(context)
        reminderEnabledState = isReminderEnabled(context)
        tintWashEnabledState = isTintWashEnabled(context)
        entryMetaEnabledState = isEntryMetaEnabled(context)
        smartSpinLayoutState = isSmartSpinLayoutEnabled(context)
        smartDensityModeState = getSmartDensityMode(context)
        exploreSessionsEnabledState = isExploreSessionsEnabled(context)
        liveNotificationsEnabledState = isLiveNotificationsEnabled(context)
        overlayBubbleEnabledState = isOverlayBubbleEnabled(context)
        overlayAskDeclinedState = isOverlayAskDeclined(context)
        voiceToTextEnabledState = isVoiceToTextEnabled(context)
        guideEnabledState = isGuideEnabled(context)
        guideTourOfferedState = isGuideTourOffered(context)
        petEnabledState = isPetEnabled(context)
        floatingPetEnabledState = isFloatingPetEnabled(context)
        pinnedTopicsState = getPinnedTopics(context)
        savedQuotesState = getSavedQuotes(context)
        topicSentimentsState = getTopicSentiments(context)
        hiddenCategoriesState = getHiddenCategories(context)
        categoryOrderState = getCategoryOrder(context)
    }

    // ── Theme ────────────────────────────────────────────────────────
    fun getThemeMode(context: Context): String =
        prefs(context).getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
        themeModeState = mode
    }

    // ── Theme style ──────────────────────────────────────────────────
    fun getThemeStyle(context: Context): String =
        prefs(context).getString(KEY_THEME_STYLE, THEME_STYLE_DEFAULT) ?: THEME_STYLE_DEFAULT

    fun setThemeStyle(context: Context, style: String) {
        prefs(context).edit().putString(KEY_THEME_STYLE, style).apply()
        themeStyleState = style
    }

    // ── Pastel color mode ─────────────────────────────────────────────
    /** Whether the pastel color mode is on (default on). */
    fun isPastelColorsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PASTEL_COLORS_ENABLED, true)

    fun setPastelColorsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PASTEL_COLORS_ENABLED, enabled).apply()
        pastelColorsState = enabled
    }

    // ── Pastel crown depth (v7.12 experimental) ───────────────────────
    /** Whether pastel card gradients get a subtle 5% black deepen at the top (default on). */
    fun isPastelCrownDepthEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PASTEL_CROWN_DEPTH, true)

    fun setPastelCrownDepthEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PASTEL_CROWN_DEPTH, enabled).apply()
        pastelCrownDepthState = enabled
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

    // ── Main card (hero ticket) redesign (v7.13 experimental) ──────────
    /** Whether the enhanced hero-card gradient fill is on (default off). */
    fun isHeroGradientEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_GRADIENT, false)

    fun setHeroGradientEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_GRADIENT, enabled).apply()
        heroGradientState = enabled
    }

    /** Whether the accent-tinted hero-card border is on (default off). */
    fun isHeroBorderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_BORDER, false)

    fun setHeroBorderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_BORDER, enabled).apply()
        heroBorderState = enabled
    }

    /** Whether the soft hero-card shadow is on (default off). */
    fun isHeroShadowEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HERO_SHADOW, false)

    fun setHeroShadowEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HERO_SHADOW, enabled).apply()
        heroShadowState = enabled
    }

    // ── Material card blends (v7.8 experimental) ───────────────────────
    /** Whether Material-style cards mix the category accent with the device's dynamic colors (default on). */
    fun isMaterialCardBlendsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MATERIAL_CARD_BLENDS, true)

    fun setMaterialCardBlendsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MATERIAL_CARD_BLENDS, enabled).apply()
        materialCardBlendsState = enabled
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
     * Whether category-tinted backgrounds are EFFECTIVELY on: the user
     * toggle AND the theme style must both allow them. The AMOLED and
     * Material styles force the tint off (pure black / device colors)
     * regardless of the toggle, so [CurioTheme] and the wash helpers read
     * this instead of the raw toggle.
     */
    fun tintWashEffective(): Boolean =
        tintWashEnabledState && themeStyleState == THEME_STYLE_DEFAULT

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
     * Whether the floating explore bubble is on. Default ON. Off = the
     * timer lives only in the notification (when live notifications are on).
     */
    fun isOverlayBubbleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERLAY_BUBBLE_ENABLED, true)

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

    // ── Guided tour ──────────────────────────────────────────────────
    fun isGuideEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GUIDE_ENABLED, true)

    fun setGuideEnabled(context: Context, enabled: Boolean) {
        guideEnabledState = enabled
        prefs(context).edit().putBoolean(KEY_GUIDE_ENABLED, enabled).apply()
    }

    /**
     * Whether the one-time quest-tour offer has already been shown (taken
     * or declined). Once true, the Quests page never offers the tour again
     * and the first quest navigates normally (v8.2).
     */
    fun isGuideTourOffered(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GUIDE_TOUR_OFFERED, false)

    fun setGuideTourOffered(context: Context, offered: Boolean) {
        guideTourOfferedState = offered
        prefs(context).edit().putBoolean(KEY_GUIDE_TOUR_OFFERED, offered).apply()
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

    // ── Last-used Spin category — persisted so the Spin tab opens where ─
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
