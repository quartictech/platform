package io.quartic.glisten

import com.fasterxml.jackson.module.kotlin.convertValue
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.glisten.github.model.PushEvent
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/github")
class GithubResource {
    // TODO - handle webhook secret token (see https://developer.github.com/webhooks/securing/)
    // TODO - handle PingEvent, InstallationEvent, and InstallationRepositoriesEvent
    // TODO - look up based on installation ID or repo ID or something
    // TODO - handle DoS due to massive payload causing OOM

    private val LOG by logger()

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun handleEvent(
        @HeaderParam("X-Github-Event") eventType: String,
        @HeaderParam("X-Github-Delivery") deliveryId: String,
        body: Map<String, Any>
    ) {
        if (eventType != "push") {
            LOG.info("[$deliveryId] Ignored event of type '$eventType'")
            return
        }

        val pushEvent = try {
            OBJECT_MAPPER.convertValue<PushEvent>(body)
        } catch (e: Exception) {
            throw BadRequestException("[$deliveryId] Unparsable payload", e)
        }

        // TODO - we shouldn't be logging this kind of detail
        LOG.info("[$deliveryId] Commit(s) pushed for repo '${pushEvent.repository.fullName}' (ref: '${pushEvent.ref}') by '${pushEvent.pusher.name}'")

        // TODO - need to do something useful here
    }
}
