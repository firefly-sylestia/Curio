package com.curio.app.features.petdesigner

import com.curio.app.data.PetFaceMoods
import com.curio.app.data.PetReactionEvents
import com.curio.app.data.petAnimationName

/**
 * v8.45 — Pet Designer Universal Editor (redesign plan, Phase 1): the
 * designer becomes a target-based studio. The user first picks WHAT to edit
 * (a [PetEditorTarget]) on one of three local pages ([PetDesignerPage]), then
 * edits it in ONE universal editor, with Save always visible below.
 *
 * The editor tabs (PREVIEW/BODY/FACES/COLORS/TOOLS) are replaced by this
 * model; each exposed editor surface (body canvas, mood faces, reactions,
 * palette) is reached through a target. Detail drawing is intentionally not an
 * editor target; detail visibility remains available in Settings.
 */
// v8.52 — the three-page studio redesign: Pets (pick your companion + see
// its animations), Editor (choose a target, then just the editor), Settings
// (look toggles, accessories, presets, shapes, export).
internal enum class PetDesignerPage { PETS, EDITOR, SETTINGS }

/**
 * What the universal editor is currently editing. Each target drives the
 * editor's mode: a pixel canvas (body / curled pose), face controls (per
 * mood), a reaction form (per event), or the palette editor.
 */
// Serializable so the selected target survives configuration changes via
// rememberSaveable (a plain sealed interface would crash on rotation).
internal sealed interface PetEditorTarget : java.io.Serializable {
    val id: String
    val title: String

    /** The pet's body canvas (grid "body"). */
    data object Body : PetEditorTarget {
        override val id = "body"
        override val title = "Body"
    }

    /** The asleep / curled pose canvas (grid "curled"). */
    data object CurledPose : PetEditorTarget {
        override val id = "curled"
        override val title = "Curled pose"
    }

    /** One mood's face. */
    data class Face(val mood: String) : PetEditorTarget {
        override val id = "face:$mood"
        override val title = "${PetFaceMoods.label(mood)} face"
    }

    /** One event's reaction rule (an action the pet performs). */
    data class Reaction(val event: String) : PetEditorTarget {
        override val id = "reaction:$event"
        override val title = PetReactionEvents.label(event)
    }

    /**
     * v8.53 — one user-defined custom action (Phase 7). The sentinel id
     * [NEW_CUSTOM_ACTION_ID] means "create a fresh action" — the screen
     * replaces it with a real id when the target is selected.
     */
    data class CustomAction(val actionId: String) : PetEditorTarget {
        override val id = "custom:$actionId"
        override val title = "Custom action"
    }

    companion object {
        /** Sentinel id meaning "create a new custom action". */
        const val NEW_CUSTOM_ACTION_ID = "__new__"
    }

    /** One animation — opens its frame timeline editor (v8.48). */
    data class Animation(val animationId: String) : PetEditorTarget {
        override val id = "animation:$animationId"
        override val title = petAnimationName(animationId)
    }

    /** The full palette editor. */
    data object Colors : PetEditorTarget {
        override val id = "colors"
        override val title = "Colors"
    }
}
