package io.quartic.weyl.core.source

import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.weyl.core.model.Tag

class DatasetAuthoriser(private val rules: Map<DatasetNamespace, List<Tag>>) {
    fun isAllowed(coords: DatasetCoordinates, config: DatasetConfig): Boolean {
        val rule = rules[coords.namespace] ?: return false

        if (rule.isEmpty()) {
            return true
        }

        val tags = config.extensions["tags"] as? Collection<*> ?: return false

        return !tags.intersect(rule).isEmpty()
    }
}