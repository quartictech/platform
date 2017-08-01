package io.quartic.mgmt

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.auth.TokenGenerator
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Duration
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.core.Response

class AuthResourceShould {
    private val tokenGenerator = TokenGenerator(KEY, Duration.ofMinutes(100))
    private val gitHubOAuth = mock<GitHubOAuth>()
    private val gitHub = mock<GitHub>()
    private val resource = AuthResource(
        GithubConfiguration(
            clientId = "foo",
            clientSecret = "bar",
            allowedOrganisations = setOf("quartictech"),
            trampolineUrl = "noob",
            scopes = listOf("user"),
            redirectHost = "wat"
        ),
        CookiesConfiguration(
            secure = true,
            maxAgeSeconds = 30
        ),
        tokenGenerator, gitHubOAuth, gitHub
    )
    private val accessToken = AccessToken("sweet", null, null)

    @Test
    fun reject_non_whitelisted_organisations() {
        whenever(gitHub.organizations(any())).thenReturn(listOf(Organization("noob")))
        whenever(gitHubOAuth.accessToken(any(), any(), any(), any())).thenReturn(accessToken)

        val asyncResponse = mock<AsyncResponse>()
        resource.githubComplete("noob", "TODO", "TODO", "localhost", asyncResponse)

        val captor = argumentCaptor<Response>()
        verify(asyncResponse, timeout(1000)).resume(captor.capture())
        assertThat(captor.firstValue.status, equalTo(401))
    }

    @Test
    fun accept_whitelisted_organisations() {
        whenever(gitHub.organizations(any())).thenReturn(listOf(Organization("quartictech")))
        whenever(gitHub.user(any())).thenReturn(User("anoob"))
        whenever(gitHubOAuth.accessToken(any(), any(), any(), any())).thenReturn(accessToken)

        val asyncResponse = mock<AsyncResponse>()
        resource.githubComplete("noob", "TODO", "TODO", "localhost", asyncResponse)

        val captor = argumentCaptor<Response>()
        verify(asyncResponse, timeout(1000)).resume(captor.capture())
        assertThat(captor.firstValue.status, equalTo(200))
    }

    companion object {
        private val KEY = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="
    }
}
