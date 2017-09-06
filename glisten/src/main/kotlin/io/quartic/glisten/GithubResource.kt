package io.quartic.glisten

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.logging.logger
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.api.EvalTriggerService
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.github.PushEvent
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest
import java.time.Clock
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/hooks/github")
class GithubResource(
    private val secret: UnsafeSecret,
    private val trigger: EvalTriggerService,
    private val clock: Clock = Clock.systemUTC()
) {
    // TODO - handle DoS due to massive payload causing OOM

    private val LOG by logger()

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
            val rawEvent = OBJECT_MAPPER.readValue<Map<String, Any>>(payload)
            when (eventType) {
                "push" -> handlePushEvent(parseEvent(payload, deliveryId), rawEvent)
                else -> LOG.info("Ignored event of type '$eventType'".nicely())
            }
        }

        // TODO - maybe move to DW auth
        private fun validateSignature() {
            val keySpec = SecretKeySpec(secret.veryUnsafe.toByteArray(), "HmacSHA1")

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(keySpec)
            val result = mac.doFinal(payload.toByteArray())

            val expected = "sha1=${Hex.encodeHexString(result)}"

            if (!MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())) {
                throw NotAuthorizedException("Signature mismatch".nicely())
            }
        }

        private fun handlePushEvent(event: PushEvent, rawEvent: Map<String, Any>) {
            fun String.toMessage() = "Trigger $this (repoId = '${event.repository.id} (${event.repository.fullName}), ref = '${event.ref}')".nicely()

            try {
                trigger.trigger(
                    BuildTrigger.GithubWebhook(
                        deliveryId = deliveryId,
                        repoId = event.repository.id,
                        repoName = event.repository.name,
                        repoOwner = event.repository.owner.name,
                        installationId = event.installation.id,
                        ref = event.ref,
                        commit = event.headCommit.id,
                        timestamp = clock.instant(),
                        rawWebhook = rawEvent
                    )
                )
                LOG.info("success".toMessage())
            } catch (e: Exception) {
                LOG.warn("failed".toMessage(), e)
            }
        }

        private fun String.nicely() = "[$deliveryId] $this"
    }

    private inline fun <reified T : Any> validatePresent(x : T?) = x ?: throw BadRequestException("Missing header")

    private inline fun <reified T : Any> parseEvent(body: String, deliveryId: String): T =
        try {
            OBJECT_MAPPER.readValue(body)
        } catch (e: Exception) {
            throw BadRequestException("[$deliveryId] Unparsable payload", e)
        }
}
