package io.quartic.mgmt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import feign.*
import java.net.URI
import java.net.URLEncoder
import javax.ws.rs.core.MediaType
import feign.FeignException.errorStatus
import feign.codec.ErrorDecoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.quartic.common.client.userAgentFor
import io.quartic.common.serdes.OBJECT_MAPPER
import javax.ws.rs.core.HttpHeaders.USER_AGENT


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

val OAUTH_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"

fun queryParam(key: String, value: String) = "${key}=${URLEncoder.encode(value, Charsets.UTF_8.name())}"

fun oauthUrl(clientId: String, redirectUri: String, scopes: List<String>): URI {
    val queryString = listOf(
        queryParam("client_id", clientId),
        queryParam("redirect_uri", redirectUri),
        queryParam("scopes", scopes.joinToString(" "))
    ).joinToString("&")

    return URI.create("${OAUTH_AUTHORIZE_URL}?${queryString}")
}


