package io.quartic.glisten.github.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

typealias GitRef = String
typealias GitHash = String

// See https://developer.github.com/v3/activity/events/types/#pushevent (though note it's not correct)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PushEvent(
    val ref: GitRef,
    val after: GitHash,
    val before: GitHash,
    val commits: List<Commit>,
    val organization: Organization?,    // Not present for user repos
    val pusher: Pusher,

    // Common fields (see https://developer.github.com/webhooks/#payloads)
    val sender: Sender,
    val repository: Repository,
    val installation: Installation
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Commit(
    val id: GitHash,
    val message: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", without = arrayOf(JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE))
    val timestamp: OffsetDateTime,
    val author: User,
    val committer: User
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Repository(
    val id: Int,
    val name: String,
    @JsonProperty("full_name")
    val fullName: String,
    val private: Boolean,
    @JsonProperty("clone_url")
    val cloneUrl: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Organization(
    val id: Int,
    val login: String,
    val description: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    val name: String,
    val email: String,
    val username: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Pusher(
    val name: String,
    val email: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sender(
    val id: Int,
    val login: String,
    val type: String    // TODO - is this an enum?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Installation(
    val id: Int
)