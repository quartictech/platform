package io.quartic.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import org.apache.http.HttpHeaders.ACCEPT
import org.apache.http.client.utils.URIBuilder
import retrofit2.http.*
import java.net.URI
import java.util.concurrent.CompletableFuture
import javax.ws.rs.core.MediaType.APPLICATION_JSON


@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubOrganization(
    val id: Long,
    val login: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    val fullName: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubUser(
    val id: Long,
    val login: String,
    val name: String?,
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
    @Headers("${ACCEPT}: ${APPLICATION_JSON}")
    fun accessTokenAsync(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("redirect_uri") redirectUri: String,
        @Query("code") code: String
    ): CompletableFuture<AccessToken>
}

@Retrofittable
interface GitHubClient {

    // https://developer.github.com/v3/users/#get-a-single-user
    @GET("user/{userId}")
    @Headers("${ACCEPT}: ${APPLICATION_JSON}")
    fun userAsync(@Path("userId") userId: Int): CompletableFuture<GitHubUser>

    // https://developer.github.com/v3/users/#get-the-authenticated-user
    @GET("user")
    @Headers("${ACCEPT}: ${APPLICATION_JSON}")
    fun userAsync(@Header("Authorization") auth: AuthToken): CompletableFuture<GitHubUser>

    // https://developer.github.com/v3/orgs/#list-your-organizations
    @GET("user/orgs")
    @Headers("${ACCEPT}: ${APPLICATION_JSON}")
    fun organizationsAsync(@Header("Authorization") auth: AuthToken): CompletableFuture<List<GitHubOrganization>>

    // https://developer.github.com/v3/repos/#list-your-repositories
    @GET("user/repos")
    @Headers("${ACCEPT}: ${APPLICATION_JSON}")
    fun reposAsync(@Header("Authorization") auth: AuthToken): CompletableFuture<List<GitHubRepo>>
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


