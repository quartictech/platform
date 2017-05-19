package io.quartic.zeus.provider

import io.quartic.zeus.model.ItemId
import io.quartic.zeus.provider.DataProvider.TermMatcher

class SelectiveMatcher(
        private val indexedAttributes: Set<String>,
        private val data: Map<ItemId, Map<String, Any>>
) : TermMatcher {
    override fun invoke(terms: Set<String>, limit: Int): Map<ItemId, Map<String, Any>> {
        // TODO: this needs optimising
        val filteredSequence = data
                .asSequence()
                .filter({ item ->
                    indexedAttributes.any {
                        terms.any { term ->
                            (item.value[it]?.toString() ?: "").toLowerCase().contains(term.toLowerCase())
                        }
                    }
                })

        return (if (limit > 0) filteredSequence.take(limit) else filteredSequence)
                .associateBy({ it.key }, { it.value })
    }
}