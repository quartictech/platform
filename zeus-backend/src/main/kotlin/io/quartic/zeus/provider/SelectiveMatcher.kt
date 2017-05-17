package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId
import io.quartic.zeus.provider.DataProvider.TermMatcher

class SelectiveMatcher(
        private val indexedAttributes: List<String>,
        private val data: Map<ItemId, Map<String, Any>>
) : TermMatcher {
    override fun invoke(term: String): Map<ItemId, Map<String, Any>> {
        return data.filterValues { item ->
            indexedAttributes.any { item[it]?.toString() == term }
        }
    }
}