package io.quartic.weyl.core.model

import io.quartic.weyl.core.model.Alert.Level.INFO

data class Alert(
        val title: String,
        val body: String?,
        val level: Level = INFO
) {
    enum class Level {
        INFO,
        WARNING,
        SEVERE
    }
}
