package io.quartic.gradle.service

public class ServiceExtension {
    String mainClassName
    String memory
    String dockerBaseImage = "openjdk:8u92-jre-alpine"
    boolean withConfigFile = true
}
