package io.quartic.eval

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.client.ClientBuilder
import io.quartic.common.client.Retrofittable
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.secrets.encodeAsBase64
import io.quartic.common.secrets.encodeAsHex
import org.junit.Ignore
import org.junit.Test
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Ignore
class ManualBitbucketAccessTokenTest {
    private fun generateJwt(): String {
        val appId = "quartic-noobhole"
        val canonicalRequest = "POST&/site/oauth2/access_token&"    // See https://developer.atlassian.com/bitbucket/concepts/qsh.html

        val sharedSecret = "59lT3lCmlGf/10ygHffayqZX906J9+zuNP9Jj6TaRyM"

        val digest = MessageDigest.getInstance("SHA-256")
        val qsh = digest.digest(canonicalRequest.toByteArray(StandardCharsets.UTF_8)).encodeAsHex()

        val now = Instant.now().epochSecond
        return Jwts.builder()
            .claim("iat", now)
            .claim("exp", now + 10 * 60)
            .claim("iss", appId)
            .claim("qsh", qsh)
            .claim("sub", "connection:1535193")
            .signWith(SignatureAlgorithm.HS256, sharedSecret.toByteArray().encodeAsBase64())
            .compact()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class BitbucketAccessToken(
        @JsonProperty("access_token")
        val token: UnsafeSecret
    )

    @Retrofittable
    interface Bitbucket {
        @FormUrlEncoded
        @POST("/site/oauth2/access_token")
        fun acquireAccessTokenAsync(
            @Header("Authorization") auth: String,
            @Field("grant_type") grantType: String
        ): CompletableFuture<BitbucketAccessToken>
    }



    @Test
    fun yeah() {
        val client = ClientBuilder(javaClass).retrofit<Bitbucket>("https://bitbucket.org")

        val token = client.acquireAccessTokenAsync("JWT ${generateJwt()}", "urn:bitbucket:oauth2:jwt").get()
        println(token.token.veryUnsafe)
    }
}
