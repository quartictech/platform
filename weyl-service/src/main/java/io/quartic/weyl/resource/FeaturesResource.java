package io.quartic.weyl.resource;

import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Path("/features")
public class FeaturesResource {
    private final FeatureStore featureStore;

    public FeaturesResource(FeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Feature> getFeatures(List<FeatureId> featureIds) {
        return featureIds.stream()
                .map(featureStore::get)
                .collect(Collectors.toSet());
    }
}
