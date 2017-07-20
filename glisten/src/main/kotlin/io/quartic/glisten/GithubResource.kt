package io.quartic.glisten

import com.fasterxml.jackson.module.kotlin.convertValue
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.glisten.github.model.PushEvent
import io.quartic.glisten.model.Notification
import io.quartic.glisten.model.Registration
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/github")
class GithubResource(
    registrations: Map<String, Registration>,
    private val notify: (Notification) -> Unit
) {
    // TODO - handle webhook secret token (see https://developer.github.com/webhooks/securing/)
    // TODO - handle DoS due to massive payload causing OOM

    private val LOG by logger()

    private val installations = registrations
        .entries
        .associateBy({ e -> e.value.installationId }, { e -> e.key })

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun handleEvent(
        @HeaderParam("X-Github-Event") eventType: String,
        @HeaderParam("X-Github-Delivery") deliveryId: String,
        body: Map<String, Any>
    ) {
        // TODO - handle PingEvent, InstallationEvent, and InstallationRepositoriesEvent
        when (eventType) {
            "push" -> handlePushEvent(parseEvent(body, deliveryId), deliveryId)
            else -> LOG.info("[$deliveryId] Ignored event of type '$eventType'")
        }
    }

    private fun handlePushEvent(pushEvent: PushEvent, deliveryId: String) {
        val customer = installations[pushEvent.installation.id]
            ?: throw ForbiddenException("[$deliveryId] Unregistered installation ${pushEvent.installation.id}")

        // TODO - we shouldn't be logging this kind of detail
        LOG.info("[$deliveryId] Push (customer = '$customer', repo = '${pushEvent.repository.fullName}', ref = '${pushEvent.ref}')")

        notify(Notification(customer, pushEvent.repository.cloneUrl))
    }

    private inline fun <reified T : Any> parseEvent(body: Map<String, Any>, deliveryId: String): T =
        try {
            OBJECT_MAPPER.convertValue(body)
        } catch (e: Exception) {
            throw BadRequestException("[$deliveryId] Unparsable payload", e)
        }
}
