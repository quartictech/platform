package io.quartic.eval.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.quartic.common.model.CustomerId
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.eval.api.model.BuildTrigger.Companion.VERSION
import java.time.Instant

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(BuildTrigger.GithubWebhook::class, name = "github_webhook_${VERSION}"),
    JsonSubTypes.Type(BuildTrigger.Manual::class, name = "manual_${VERSION}")
)
sealed class BuildTrigger {
    abstract fun branch(): String

    enum class TriggerType {
        EVALUATE,
        EXECUTE
    }

    data class GithubWebhook(
        val deliveryId: String,
        val repoId: Long,
        val ref: String,
        val commit: String,
        val timestamp: Instant,
        val repoName: String,
        val repoOwner: String,
        val installationId: Long,
        val rawWebhook: Map<String, Any>
    ): BuildTrigger() {
        override fun branch(): String = ref.removePrefix("refs/heads/")
    }

    data class Manual(
       val user: String,
       val timestamp: Instant,
       val customerId: CustomerId,
       val branch: String,
       val triggerType: TriggerType,
       val silent: Boolean = false
    ): BuildTrigger() {
        override fun branch() = branch
    }

    companion object {
        const val VERSION = "v1"
    }
}

