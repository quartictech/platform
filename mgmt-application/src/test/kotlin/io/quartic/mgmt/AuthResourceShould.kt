package io.quartic.mgmt

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.auth.TokenGenerator
import org.junit.Test
import org.junit.Assert.*
import org.mockito.ArgumentCaptor
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.core.Response
import org.hamcrest.Matchers.*
import java.time.Duration

class AuthResourceShould {
    private val gitHubConfiguration = GithubConfiguration(
        clientId = "foo",
        clientSecret = "bar",
        allowedOrganisations = setOf("quartictech"),
        trampolineUrl = "noob",
        useSecureCookies = true,
        scopes = listOf("user"),
        redirectHost = "wat",
        cookieMaxAgeSeconds = 0
    )
    val tokenGenerator = TokenGenerator(
        KEY,
        Duration.ofMinutes(100)
    )
    private val gitHubOAuth = mock<GitHubOAuth>()
    private val gitHub = mock<GitHub>()
    private val resource = AuthResource(gitHubConfiguration, tokenGenerator, gitHubOAuth, gitHub)

    fun accessToken() = AccessToken("sweet", null, null)


    @Test
    fun reject_non_whitelisted_organisations() {
        whenever(gitHub.organizations(any())).thenReturn(listOf(Organization("noob")))
        whenever(gitHubOAuth.accessToken(any(), any(), any(), any())).thenReturn(accessToken())
        val asyncResponse = mock<AsyncResponse>()
        resource.githubComplete("noob", "localhost", asyncResponse)
        val argumentCaptor = ArgumentCaptor.forClass(Response::class.java)
        verify(asyncResponse, timeout(1000)).resume(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().getStatus(), equalTo(401))
    }

    @Test
    fun accept_whitelisted_organisations() {
        whenever(gitHub.organizations(any())).thenReturn(listOf(Organization("quartictech")))
        whenever(gitHub.user(any())).thenReturn(User("anoob"))
        whenever(gitHubOAuth.accessToken(any(), any(), any(), any())).thenReturn(accessToken())
        val asyncResponse = mock<AsyncResponse>()
        resource.githubComplete("noob", "localhost", asyncResponse)
        val argumentCaptor = ArgumentCaptor.forClass(Response::class.java)
        verify(asyncResponse, timeout(1000)).resume(argumentCaptor.capture())
        assertThat(argumentCaptor.getValue().getStatus(), equalTo(200))
    }

    companion object {
        private val KEY = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="
    }

}
