package io.quartic.zeus.model

import java.time.ZonedDateTime

data class Note(
        val id: NoteId,
        val created: ZonedDateTime,
        val text: String
)