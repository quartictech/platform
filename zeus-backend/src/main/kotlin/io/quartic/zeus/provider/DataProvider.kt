package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId

interface DataProvider {
    interface TermMatcher {
        operator fun invoke(terms: Set<String>, limit: Int = 0): Map<ItemId, Map<String, Any>>
    }

    val prettyName: String
    val data: Map<ItemId, Map<String, Any>>
    val matcher: TermMatcher
}