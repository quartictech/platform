package io.quartic.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.client.ClientBuilder
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import io.quartic.common.secrets.UnsafeSecret
import org.apache.commons.codec.binary.Base64
import retrofit2.http.*
import java.net.URI
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
    data class GitHubInstallationAccessToken(val token: UnsafeSecret) {
        fun authorizationCredentials() = "token ${token.veryUnsafe}"
        fun urlCredentials() = "x-access-token:${token.veryUnsafe}"
    }

    @Retrofittable
    interface GitHubInstallationRetrofit {
        @POST("installations/{installationId}/access_tokens")
        @retrofit2.http.Headers("Accept: application/vnd.github.machine-man-preview+json")
        fun installationAccessTokenAsync(
            @Path("installationId") installationId: Long,
            @Header("Authorization") auth: String
        ): CompletableFuture<GitHubInstallationAccessToken>

        @POST("repos/{owner}/{repo}/statuses/{sha}")
        @retrofit2.http.Headers("Accept: application/vnd.github.machine-man-preview+json")
        fun sendStatus(
            @Path("owner") owner: String,
            @Path("repo") repo: String,
            @Path("sha") sha: String,
            @Header("Authorization") auth: String,
            @Body status: StatusCreate
        ): CompletableFuture<Void>

        @GET("repositories/{repoId}")
        @retrofit2.http.Headers("Accept: application/vnd.github.machine-man-preview+json")
        fun getRepository(
            @Path("repoId") repoId: Long,
            @Header("Authorization") auth: String
        ): CompletableFuture<Repository>
    }

    private val githubRetrofit = clientBuilder.retrofit<GitHubInstallationRetrofit>(githubApiRoot)
    private val privateKey = KeyFactory.getInstance("RSA")
        .generatePrivate(PKCS8EncodedKeySpec(Base64.decodeBase64(key.veryUnsafe)))

    // See https://developer.github.com/apps/building-integrations/setting-up-and-registering-github-apps/about-authentication-options-for-github-apps/
    private fun generateJwt(): String {
        val now = Instant.now().epochSecond
        return Jwts.builder()
            .claim("iat", now)
            .claim("exp", now + 5 * 60) // 10-minutes is the max, but set lower to avoid rejection due to clock skew issues
            .claim("iss", appId)
            .signWith(SignatureAlgorithm.RS256, privateKey)
            .compact()
    }

    fun accessTokenAsync(installationId: Long) = githubRetrofit.installationAccessTokenAsync(
        installationId,
        "Bearer ${generateJwt()}"
    )

    fun sendStatusAsync(
        owner: String,
        repo: String,
        sha: String,
        status: StatusCreate,
        accessToken: GitHubInstallationAccessToken
    ) = githubRetrofit.sendStatus(owner, repo, sha, accessToken.authorizationCredentials(), status)

    fun getRepositoryAsync(repoId: Long, accessToken: GitHubInstallationAccessToken) =
        githubRetrofit.getRepository(repoId, accessToken.authorizationCredentials())
}





