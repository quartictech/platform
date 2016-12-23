package io.quartic.catalogue;

import io.dropwizard.Configuration;
import io.quartic.catalogue.io.quartic.catalogue.datastore.GoogleDatastoreBackend;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CatalogueConfiguration extends Configuration {
   private enum Backend {
      IN_MEMORY,
      GOOGLE_DATASTORE
   }

   @Valid
   @NotNull
   private Backend backend;

   public StorageBackend getBackend() {
       switch (backend) {
          case IN_MEMORY:
             return new InMemoryStorageBackend();
          case GOOGLE_DATASTORE:
             return GoogleDatastoreBackend.remote("quartictech");
          default:
             throw new RuntimeException("invalid backend: " + backend);
       }
   }
}
