package io.quartic.mgmt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import feign.Param
import io.quartic.common.serdes.OBJECT_MAPPER
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class Github(val clientId: String, val clientSecret: String, val redirectUri: String) {
    val client = OkHttpClient()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Organization(
        val login: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class User(
        val login: String
    )

    fun user(oauthToken: String): User {
        val request = Request.Builder()
            .url(HttpUrl.Builder()
                .host("api.github.com")
                .scheme("https")
                .encodedPath("/user")
                .build())
            .get()
            .header("Authorization", "token ${oauthToken}")
            .build()
        val response = client.newCall(request).execute().body().string()
        return OBJECT_MAPPER.readValue<User>(response)
    }

    fun organizations(oauthToken: String): List<Organization> {
        val request = Request.Builder()
            .url(HttpUrl.Builder()
                .host("api.github.com")
                .scheme("https")
                .encodedPath("/user/orgs")
                .build())
            .get()
            .header("Authorization", "token ${oauthToken}")
            .build()
        val response = client.newCall(request).execute().body().string()
        return OBJECT_MAPPER.readValue<List<Organization>>(response)
    }

    fun accessToken(code: String): String {
        val body = RequestBody.create(null, byteArrayOf())
        val request = Request.Builder()
            .url(HttpUrl.Builder()
                .host("github.com")
                .scheme("https")
                .encodedPath("/login/oauth/access_token")
                .addEncodedQueryParameter("client_id", clientId)
                .addEncodedQueryParameter("client_secret", clientSecret)
                .addEncodedQueryParameter("redirect_uri", redirectUri)
                .addEncodedQueryParameter("code", code)
                .build())
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val params = response.body().string().split("&")
            .map { s ->
                val parts = s.split("=")
                Pair(parts[0], parts[1])
            }
            .groupBy { p -> p.first }
        if ("access_token" in params) return params["access_token"]!![0].second
        else throw IllegalStateException("no access token returned")
    }
}
