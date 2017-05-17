package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId

interface DataProvider {
    interface TermMatcher {
        operator fun invoke(term: String): Map<ItemId, Map<String, Any>>
    }

    val data: Map<ItemId, Map<String, Any>>
    val matcher: TermMatcher
}