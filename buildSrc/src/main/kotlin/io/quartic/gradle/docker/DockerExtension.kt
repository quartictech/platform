package io.quartic.gradle.docker

import org.gradle.api.file.CopySpec

open class DockerExtension {
    var image: String? = null
    var content: CopySpec? = null
}
