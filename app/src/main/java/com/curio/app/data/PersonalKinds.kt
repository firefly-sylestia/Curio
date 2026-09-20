package com.curio.app.data

/**
 * v426 — WHAT KIND OF THING IS ON THE SHELF.
 *
 * The member's own note was exact: *"why as books? keep them as manga or
 * whatever they are called, but add them to be able to add in my shelf"* — a
 * manga is not a book with a different cover, it is a manga, and a shelf that
 * called it one would be lying about the thing the member is collecting. So the
 * KIND is a column on the row ([PersonalBookEntity.kind]) and a label the shelf
 * and the page say out loud, while the row itself stays the same row: the same
 * chapters, the same reading progress, the same reader, the same file.
 *
 * ── WHY A STRING AND NOT AN ENUM COLUMN ─────────────────────────────────
 *
 * Room stores this as TEXT (see MIGRATION_20_21) rather than an ordinal, for the
 * same reason every other stored id in this family is a string: an enum's
 * ordinal is a silent contract that a future insert renumbers, and a shelf row
 * outlives any one build. [idOf] is the only door a stored value takes on its
 * way back in, so an unknown id (a row written by a newer build) reads as a
 * plain book instead of crashing the shelf.
 */
object PersonalKinds {

    /** A printed book, a novel, a memoir — the shelf's own default. */
    const val BOOK = "book"

    /** Japanese comics: volumes of chapters, read right to left. */
    const val MANGA = "manga"

    /** Korean comics — the long-strip webtoon shape far more often than not. */
    const val MANHWA = "manhwa"

    /** Chinese comics, the same shape as manhwa. */
    const val MANHUA = "manhua"

    /** Western comics: issues bound into volumes and trade paperbacks. */
    const val COMIC = "comic"

    /** Prose with a manga's own shape (AniList and MAL both know these). */
    const val LIGHT_NOVEL = "light-novel"

    /** Every kind the shelf offers, in the order the picker shows them. */
    val all: List<String> = listOf(BOOK, MANGA, MANHWA, MANHUA, COMIC, LIGHT_NOVEL)

    /** The kind's own name, as the shelf and the page say it. */
    fun label(kind: String): String = when (kind) {
        MANGA -> "Manga"
        MANHWA -> "Manhwa"
        MANHUA -> "Manhua"
        COMIC -> "Comic"
        LIGHT_NOVEL -> "Light novel"
        else -> "Book"
    }

    /**
     * True for the kinds whose metadata comes from the COMICS sources
     * ([MangaFetch]) rather than from a books catalogue: manga, manhwa, manhua
     * and light novels all live in AniList / MangaDex / MAL / Kitsu, and Open
     * Library has never heard of them (a search there for a volume of One Piece
     * answers with a study guide about it, or nothing at all).
     */
    fun isComics(kind: String): Boolean =
        kind == MANGA || kind == MANHWA || kind == MANHUA || kind == LIGHT_NOVEL

    /** The kind id for a stored value, falling back to [BOOK] for anything new. */
    fun idOf(value: String?): String =
        all.firstOrNull { it.equals(value?.trim(), ignoreCase = true) } ?: BOOK
}
