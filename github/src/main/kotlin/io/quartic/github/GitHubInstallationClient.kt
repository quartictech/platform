package io.quartic.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import feign.Headers
import feign.Param
import feign.RequestLine
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.client.ClientBuilder
import io.quartic.common.client.Retrofittable
import io.quartic.common.secrets.UnsafeSecret
import org.apache.commons.codec.binary.Base64
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.net.URI
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.concurrent.CompletableFuture

class GitHubInstallationClient(
    private val appId: String,
    githubApiRoot: URI,
    key: UnsafeSecret,
    clientBuilder: ClientBuilder
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GitHubInstallationAccessToken(
        val token: String
    )

    // TODO - eliminate the non-async client, then rename the other one
    interface GitHubInstallation {
        @RequestLine("POST /installations/{installationId}/access_tokens")
        @Headers("Authorization: Bearer {jwt}", "Accept: application/vnd.github.machine-man-preview+json")
        fun installationAccessToken(
            @Param("installationId") installationId: Long,
            @Param("jwt") jwt: String
        ): GitHubInstallationAccessToken
    }

    @Retrofittable
    interface GitHubInstallationRetrofit {
        @POST("/installations/{installationId}/access_tokens")
        @retrofit2.http.Headers("Accept: application/vnd.github.machine-man-preview+json")
        fun installationAccessTokenAsync(
            @Path("installationId") installationId: Long,
            @Header("Authorization") auth: String
        ): CompletableFuture<GitHubInstallationAccessToken>
    }

    private val github = clientBuilder.feign<GitHubInstallation>(githubApiRoot)
    private val githubRetrofit = clientBuilder.retrofit<GitHubInstallationRetrofit>(githubApiRoot)
    private val privateKey: Key

    init {
        val encoded = Base64.decodeBase64(key.veryUnsafe)
        val spec = PKCS8EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        privateKey = keyFactory.generatePrivate(spec)
    }

    // See https://developer.github.com/apps/building-integrations/setting-up-and-registering-github-apps/about-authentication-options-for-github-apps/
    private fun generateJwt(): String {
        val now = Instant.now().epochSecond

        val jwt = Jwts.builder()
            .claim("iat", now)
            // 10 minutes
            .claim("exp", now + 10 * 60)
            .claim("iss", appId)
            .signWith(SignatureAlgorithm.RS256, privateKey)
            .compact()

        return jwt
    }

    fun accessToken(installationId: Long) = github.installationAccessToken(installationId, generateJwt())
    fun accessTokenAsync(installationId: Long) = githubRetrofit.installationAccessTokenAsync(installationId, "Bearer ${generateJwt()}")
}




