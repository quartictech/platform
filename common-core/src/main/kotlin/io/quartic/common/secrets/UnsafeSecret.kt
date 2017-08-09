package io.quartic.common.secrets

data class UnsafeSecret(private val raw: String) {
    override fun toString() = "{UnsafeSecret}"      // To prevent accidental logging and stuff

    val veryUnsafe = raw
}
