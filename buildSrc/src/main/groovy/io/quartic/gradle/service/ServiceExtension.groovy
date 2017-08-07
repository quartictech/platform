package io.quartic.gradle.service

public class ServiceExtension {
    String simpleName
    String mainClassName
    String memory
    String dockerBaseImage = "openjdk:8u92-jre-alpine"
}
