package io.quartic.bild

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import feign.Headers
import feign.Param
import feign.RequestLine
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.client.client
import org.apache.commons.codec.binary.Base64
import java.security.Key
import java.security.KeyFactory
import java.time.Instant
import java.security.spec.PKCS8EncodedKeySpec





class GithubInstallationClient(private val appId: String, private val githubApiRoot: String, private val key: String) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GitHubInstallationAccessToken(
        val token: String
    )

    interface GitHubInstallation {
        @RequestLine("POST /installations/{installationId}/access_tokens")
        @Headers("Authorization: Bearer {jwt}", "Accept: application/vnd.github.machine-man-preview+json")
        fun installationAccessToken(@Param("installationId") installationId: Long, @Param("jwt") jwt: String): GitHubInstallationAccessToken
    }

    val github = client<GitHubInstallation>(javaClass, githubApiRoot)
    val privateKey: Key

    init {
        val encoded = Base64.decodeBase64(key)
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
}





