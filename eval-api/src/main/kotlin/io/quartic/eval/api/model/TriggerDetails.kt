package io.quartic.eval.api.model

import io.quartic.common.model.CustomerId
import java.net.URI
import java.time.Instant

sealed class BuildTrigger {
    data class GithubWebhook(
        val deliveryId: String,
        val repoId: Long,
        val ref: String,
        val commit: String,
        val timestamp: Instant,
        val rawWebhook: Map<String, Any>
    ): BuildTrigger() {
        fun branch(): String = ref.removePrefix("refs/heads/")
    }

    data class Manual(
       val user: String,
       val timestamp: Instant,
       val customerId: CustomerId
    ): BuildTrigger()
}

data class BuildSpec(
    val cloneUrl: URI,
    val branch: String,
    val commit: String
)

//data class TriggerDetails(
//    val type: String,
//    val deliveryId: String,     // For logging purposes
//    val installationId: Long,
//    val repoId: Long,
//    val repoFullName: String,
//    val repoName: String,
//    val repoOwner: String,
//    val cloneUrl: URI,
//    val ref: String,
//    val commit: String,
//    val timestamp: Instant,
//    val rawWebhook: Map<String, Any> = emptyMap()
//) {
//}
