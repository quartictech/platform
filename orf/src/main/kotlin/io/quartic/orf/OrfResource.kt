package io.quartic.orf

import com.google.common.base.Preconditions.checkArgument
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.TextCodec.BASE64
import io.quartic.common.logging.logger
import io.quartic.common.uid.UidGenerator
import io.quartic.orf.model.AuthResponse
import io.quartic.orf.model.JwtId
import java.time.Clock
import java.time.temporal.TemporalAmount
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/authenticate")
class OrfResource(
    private val base64EncodedKey: String,
    private val timeToLive: TemporalAmount,
    private val clock: Clock,
    private val jtiGenerator: UidGenerator<JwtId>
) {
    val LOG by logger()

    // TODO: proper auth
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun authenticate(userId: String): AuthResponse {
        val jti = jtiGenerator.get()
        LOG.info("Generated JWT with jti '$jti' for userId '$userId'")
        // Currently no need for iss or aud - only one issuer and one audience
        return AuthResponse(Jwts.builder()
            .signWith(ALGORITHM, base64EncodedKey)
            .setSubject(userId)
            .setExpiration(Date.from(expiration()))
            .setId(jti.toString())
            .compact())
    }

    private fun expiration() = clock.instant() + timeToLive

    init {
        checkArgument(BASE64.decode(base64EncodedKey).size == KEY_LENGTH_BITS / 8,
            "Key is not exactly $KEY_LENGTH_BITS bits long")
    }

    companion object {
        // We can use HMAC for now as client-side verification of tokens is not an issue
        val KEY_LENGTH_BITS = 512
        val ALGORITHM = SignatureAlgorithm.HS512
    }
}
