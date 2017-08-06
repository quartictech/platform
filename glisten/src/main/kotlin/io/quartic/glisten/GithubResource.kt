package io.quartic.glisten

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.glisten.github.model.PushEvent
import io.quartic.glisten.model.Notification
import io.quartic.glisten.model.Registration
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.*
import javax.ws.rs.core.MediaType



@Path("/github")
class GithubResource(
    registrations: Map<String, Registration>,
    private val secretToken: String,
    private val notify: (Notification) -> Unit
) {
    // TODO - handle DoS due to massive payload causing OOM

    private val LOG by logger()

    private val installations = registrations
        .entries
        .associateBy({ e -> e.value.installationId }, { e -> e.key })

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun handleEvent(
        @HeaderParam("X-Github-Event") eventType: String?,
        @HeaderParam("X-Github-Delivery") deliveryId: String?,
        @HeaderParam("X-Hub-Signature") signature: String?,
        payload: String
    ) = EventHandler(
        validatePresent(eventType),
        validatePresent(deliveryId),
        validatePresent(signature),
        payload
    )()

    private inner class EventHandler(
        val eventType: String,
        val deliveryId: String,
        val signature: String,
        val payload: String
    ) {
        operator fun invoke() {
            validateSignature()

            // TODO - handle PingEvent, InstallationEvent, and InstallationRepositoriesEvent
            when (eventType) {
                "push" -> handlePushEvent(parseEvent(payload, deliveryId))
                else -> LOG.info("[$deliveryId] Ignored event of type '$eventType'")
            }
        }

        // TODO - maybe move to DW auth
        private fun validateSignature() {
            val keySpec = SecretKeySpec(secretToken.toByteArray(), "HmacSHA1")

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(keySpec)
            val result = mac.doFinal(payload.toByteArray())

            val expected = "sha1=${Hex.encodeHexString(result)}"

            if (!MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())) {
                throw NotAuthorizedException("[$deliveryId] Signature mismatch")
            }
        }

        private fun handlePushEvent(pushEvent: PushEvent) {
            val customer = installations[pushEvent.installation.id]
                ?: throw ForbiddenException("[$deliveryId] Unregistered installation ${pushEvent.installation.id}")

            // TODO - we shouldn't be logging this kind of detail
            LOG.info("[$deliveryId] Push (customer = '$customer', repo = '${pushEvent.repository.fullName}', ref = '${pushEvent.ref}')")

            notify(Notification(customer, pushEvent.repository.cloneUrl))
        }
    }

    private inline fun <reified T : Any> validatePresent(x : T?) = x ?: throw BadRequestException("Missing header")

    private inline fun <reified T : Any> parseEvent(body: String, deliveryId: String): T =
        try {
            OBJECT_MAPPER.readValue(body)
        } catch (e: Exception) {
            throw BadRequestException("[$deliveryId] Unparsable payload", e)
        }
}
