package io.quartic.gradle.docker

import org.gradle.api.file.CopySpec

public class DockerExtension {
  Closure     image
  CopySpec    content

  void setImage(Closure image) {
    this.image = image
  }

  void setImage(String image) {
    setImage { image }
  }
}
