package com.curio.app.data

/** The species id of the original pet. Any design missing a `# pet=` line
 *  (i.e. everything saved before multi-pet) resolves to this. */
const val PET_CURIE_ID = "curie"

/**
 * The kinds of editable parts a pet can declare. The Pet Designer can stay
 * generic by switching on this type instead of hardcoding a branch per pet
 * (Phase 6 — multi-pet foundations).
 */
enum class EditablePetPartType {
    BODY,
    POSE,
    DETAIL_LAYER,
    FACE,
    EYES,
    MOUTH,
    ACCESSORY,
    EFFECT,
    ANIMATION_FRAME,
    ACTION,
    DIALOGUE
}

/**
 * One editable part a pet supports. The registry drives which editor
 * controls are shown for a pet (pixel canvas vs face controls vs timeline
 * vs dialogue form), so adding a pet never requires a designer redesign.
 */
data class EditablePetPart(
    val id: String,
    val displayName: String,
    val type: EditablePetPartType,
    val supportsPixelEditing: Boolean = false,
    val supportsColorEditing: Boolean = false,
    val supportsBlueprint: Boolean = true
)

/**
 * A pet species definition — everything the designer needs to know to edit
 * a pet. Animations and actions are declared by id (the ids the designer
 * already understands from [BUILTIN_ANIMATIONS] / [PetReactionEvents]), so
 * no extra definition types are needed until custom actions (Phase 7).
 */
data class PetDefinition(
    val id: String,
    val displayName: String,
    val tagline: String,
    val defaultDesign: PetDesign,
    val editableParts: List<EditablePetPart>,
    val animationIds: List<String>,
    val actionEventIds: List<String>
)

/**
 * Registry of known pets. Adding a pet later is ONE new [PetDefinition]
 * entry here — the designer's Pet library, strip and editors derive from
 * this list, so nothing else needs to change.
 */
object PetRegistry {
    /** Curie — the original pet; the only species today. */
    val CURIE = PetDefinition(
        id = PET_CURIE_ID,
        displayName = "Curie",
        tagline = "Your curious little explorer",
        defaultDesign = PetDesign.DEFAULT,
        editableParts = listOf(
            EditablePetPart("body", "Body", EditablePetPartType.BODY, supportsPixelEditing = true, supportsColorEditing = true),
            EditablePetPart("curled", "Curled pose", EditablePetPartType.POSE, supportsPixelEditing = true, supportsColorEditing = true),
            EditablePetPart("face", "Faces", EditablePetPartType.FACE, supportsPixelEditing = true),
            EditablePetPart("details", "Detail layers", EditablePetPartType.DETAIL_LAYER, supportsPixelEditing = true),
            EditablePetPart("animation", "Animations", EditablePetPartType.ANIMATION_FRAME),
            EditablePetPart("action", "Actions", EditablePetPartType.ACTION),
            EditablePetPart("dialogue", "Dialogue", EditablePetPartType.DIALOGUE)
        ),
        animationIds = BUILTIN_ANIMATIONS.map { it.id },
        actionEventIds = PetReactionEvents.ALL
    )

    /** All known pets, in library display order. */
    val all: List<PetDefinition> = listOf(CURIE)

    fun byId(id: String): PetDefinition? = all.firstOrNull { it.id == id }

    /**
     * Resolves a design's species id. Unknown ids (a design imported from a
     * future version, or a pet that was removed) fall back to Curie so the
     * designer NEVER crashes on a pet it can't find — the pixel art is
     * still fully editable under Curie's rules.
     */
    fun resolve(id: String): PetDefinition = byId(id) ?: CURIE
}

/** The definition for the pet a design belongs to (never null — see [PetRegistry.resolve]). */
val PetDesign.definition: PetDefinition
    get() = PetRegistry.resolve(petSpeciesId)
