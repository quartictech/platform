package io.quartic.mgmt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import feign.Headers
import feign.Param
import feign.RequestLine
import java.net.URI
import java.net.URLEncoder
import javax.inject.Named
import javax.ws.rs.core.MediaType

@JsonIgnoreProperties(ignoreUnknown = true)
data class Organization(
    val login: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    val login: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessToken(
    @JsonProperty("access_token")
    val accessToken: String
)

interface GitHubOAuth {
    @RequestLine("POST /login/oauth/access_token?client_id={client_id}&client_secret={client_secret}&redirect_uri={redirect_uri}&code={code}")
    @Headers("Accept: ${MediaType.APPLICATION_JSON}")
    fun accessToken(@Param("client_id") clientId: String,
                    @Param("client_secret") clientSecret: String,
                    @Param("redirect_uri") redirectUri: String,
                    @Param("code") code: String): AccessToken
}

interface GitHub {
    @RequestLine("GET /user")
    @Headers("Authorization: token {oauthToken}")
    fun user(@Param("oauthToken") oauthToken: String): User

    @RequestLine("GET /user/orgs")
    @Headers("Authorization: token {oauthToken}")
    fun organizations(@Param("oauthToken") oauthToken: String): List<Organization>
}

val OAUTH_AUTHORIZE_URl = "https://github.com/login/oauth/authorize"
val OAUTH_BASE_URL = "https://github.com"
val API_BASE_URL = "https://api.github.com"

fun oauthUrl(clientId: String, redirectUri: String, scopes: List<String>): URI {
    val scopes = URLEncoder.encode(scopes.joinToString(" "), "UTF-8")
    return URI.create("${OAUTH_AUTHORIZE_URl}?client_id=${clientId}&redirect_uri=${redirectUri}&scope=${scopes}")
}
