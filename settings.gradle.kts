pluginManagement {
    repositories {
        gradlePluginPortal()

        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.6.2"
}

stonecutter {
    create(rootProject) {
        versions(
            "1.21.1", "1.21.4", "1.21.6",
            "1.20.1", "1.20.4", "1.20.6"
        )
        vcsVersion = "1.21.6"
    }
}