package io.quartic.terminator;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class TerminatorApplicationShould {
    @ClassRule
    public static final DropwizardAppRule<TerminatorConfiguration> RULE =
            new DropwizardAppRule<>(TerminatorApplication.class, resourceFilePath("terminator.yml"));


    @Test
    public void do_something() throws Exception {
        // TODO
    }
}
