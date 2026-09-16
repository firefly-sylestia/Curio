package com.curio.app.features.create

import android.content.Context
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCollection
import com.curio.app.data.CurioCollectionMember
import com.curio.app.data.CurioDatabase
import com.curio.app.data.CurioEntry
import com.curio.app.data.CaptureRepository
import java.util.UUID

/**
 * Single save boundary for entries created from the Create Entry launcher.
 * Journal and Book are personal creations, so they are stored in Room and
 * immediately attached to the seeded Personal Cabinet shelf.
 */
suspend fun saveCreatedEntryToPersonalCollection(context: Context, entry: CurioEntry) {
    val db = CurioDatabase.getInstance(context)
    CaptureRepository(db.captureDao(), db.cachedTopicDao()).save(entry)

    val collections = AppPreferences.getCabinetCollections(context)
    val personal = collections.firstOrNull { it.id == "shelf:personal" }
        ?: CurioCollection(
            id = "shelf:personal",
            name = "Personal",
            createdAtMillis = System.currentTimeMillis(),
            members = emptyList()
        )

    if (personal.members.any {
            it.kind == CurioCollectionMember.MemberKind.ENTRY && it.refName == entry.id
        }) return

    val updated = personal.copy(
        members = personal.members + CurioCollectionMember(
            kind = CurioCollectionMember.MemberKind.ENTRY,
            categoryName = null,
            refName = entry.id
        )
    )
    AppPreferences.addOrReplaceCollection(context, updated)
}

fun newCreatedEntryId(): String = "created-${UUID.randomUUID()}"
