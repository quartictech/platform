package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId
import io.quartic.zeus.provider.DataProvider.TermMatcher

class SelectiveMatcher(
        private val indexedAttributes: Set<String>,
        private val data: Map<ItemId, Map<String, Any>>
) : TermMatcher {
    // TODO: this needs optimising
    override fun invoke(terms: Set<String>): Map<ItemId, Map<String, Any>> {
        return data.filterValues { item ->
            indexedAttributes.any { terms.any { term -> (item[it]?.toString() ?: "").toLowerCase().contains(term.toLowerCase()) } }
        }
    }
}