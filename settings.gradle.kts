import org.gradle.kotlin.dsl.maven

pluginManagement {
    repositories {
        gradlePluginPortal()

        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }

        maven {
            name = "Kikugie"
            url = uri("https://maven.kikugie.dev/snapshots/");
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
    create(rootProject) {
        versions(
            "1.21.1", "1.21.4", "1.21.8",
            "1.20.1", "1.20.4", "1.20.6"
        )
        vcsVersion = "1.21.8"
    }
}