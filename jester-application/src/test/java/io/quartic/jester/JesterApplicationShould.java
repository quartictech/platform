package io.quartic.jester;

import com.palantir.remoting1.jaxrs.JaxRsClient;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.quartic.jester.api.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Collection;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class JesterApplicationShould {
    @ClassRule
    public static final DropwizardAppRule<JesterConfiguration> RULE =
            new DropwizardAppRule<>(JesterApplication.class, resourceFilePath("jester.yml"));
    private final JesterService jester = JaxRsClient.builder()
                .build(JesterService.class, "test", "http://localhost:8090/api");;

    @Test
    public void foo() throws Exception {
        final DatasetConfig config = DatasetConfig.of(
                DatasetMetadata.of("Foo", "Bar", "Arlo"),
                JdbcDatasetSource.of("a", "b", "c", "d", "e")
        );

        jester.registerDataset(config);
        final Collection<DatasetId> dids = jester.listDatasets();

        assertThat(jester.getDataset(dids.iterator().next().uid()), equalTo(config));
    }
}
