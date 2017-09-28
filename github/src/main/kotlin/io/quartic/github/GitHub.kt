package io.quartic.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import org.apache.http.client.utils.URIBuilder
import retrofit2.http.*
import java.net.URI
import java.util.concurrent.CompletableFuture


@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubOrganization(
    val id: Long,
    val login: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubUser(
    val id: Long,
    val login: String,
    val name: String,
    val avatarUrl: URI
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessToken(
    val accessToken: String?,
    val error: String?,
    val errorDescription: String?
)

@Retrofittable
interface GitHubOAuthClient {
    @POST("login/oauth/access_token")
    @retrofit2.http.Headers("Accept: application/json")
    fun accessTokenAsync(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("redirect_uri") redirectUri: String,
        @Query("code") code: String
    ): CompletableFuture<AccessToken>
}

@Retrofittable
interface GitHubClient {
    @GET("user/{userId}")
    @retrofit2.http.Headers("Accept: application/json")
    fun userAsync(
        @Path("userId") userId: Int
    ): CompletableFuture<GitHubUser>

    @GET("user")
    @retrofit2.http.Headers("Accept: application/json")
    fun userAsync(
        @Header("Authorization") auth: AuthToken
    ): CompletableFuture<GitHubUser>

    @GET("user/orgs")
    @retrofit2.http.Headers("Accept: application/json")
    fun organizationsAsync(
        @Header("Authorization") auth: AuthToken
    ): CompletableFuture<List<GitHubOrganization>>
}

data class AuthToken(private val token: String) {
    override fun toString() = "token ${token}"
}

fun oauthUrl(
    oauthRoot: URI,
    clientId: String,
    redirectUri: String,
    scopes: List<String>,
    state: String
): URI = URIBuilder(oauthRoot)
    .setPath("/login/oauth/authorize")
    .setParameter("client_id", clientId)
    .setParameter("redirect_uri", redirectUri)
    .setParameter("scope", scopes.joinToString(" "))
    .setParameter("state", state)
    .build()


