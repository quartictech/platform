package io.quartic.catalogue;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CatalogueConfiguration extends Configuration {
   @Valid
   @NotNull
   private StorageBackendConfig backend;

   public StorageBackend getBackend() {
      return backend.build();
   }
}
