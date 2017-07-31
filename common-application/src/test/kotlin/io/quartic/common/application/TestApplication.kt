package io.quartic.common.application

import io.dropwizard.auth.Auth
import io.dropwizard.setup.Environment
import io.quartic.common.application.TestApplication.TestConfiguration
import io.quartic.common.auth.User
import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path

class TestApplication : ApplicationBase<TestConfiguration>(true) {
    @Path("/test")
    @PermitAll
    class TestResource {
        @GET fun get(@Auth user: User) = "Hello ${user.name}"
    }

    class TestConfiguration : ConfigurationBase()


    override fun runApplication(configuration: TestConfiguration, environment: Environment) {
        environment.jersey().register(TestResource())
    }
}
