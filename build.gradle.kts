import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"

    id("fabric-loom") version "1.11.7"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.17"
    id("dev.kikugie.stonecutter") version "0.7.10"
}

base.archivesName.set("${project.property("archives_base_name")}+${stonecutter.current.project}")

version = project.property("mod_version").toString()
group = project.property("maven_group").toString()

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Kikugie"
        url = uri("https://maven.kikugie.dev/snapshots/");
    }
}

dependencies {
    // Declare Minecraft version and use Mojang's mappings
    minecraft("com.mojang:minecraft:${stonecutter.current.project}")
    mappings(loom.officialMojangMappings())

    // Fabric stuff
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Declare dependencies
    implementation(project.property("lib_zstd").toString())
    implementation(project.property("lib_lmdb").toString())

    // Include dependencies and transitives in jar file, good luck keeping those versions up to date <3
    include(project.property("lib_zstd").toString())
    include(project.property("lib_lmdb").toString())

    include("com.github.jnr:jffi:1.3.13")
    include("com.github.jnr:jffi:1.3.13:native")
    include("com.github.jnr:jnr-a64asm:1.0.0")
    include("com.github.jnr:jnr-constants:0.10.4")
    include("com.github.jnr:jnr-ffi:2.2.17")
    include("com.github.jnr:jnr-x86asm:1.0.2")
}

val accessWidenerFile = when {
    stonecutter.eval(stonecutter.current.version, ">=1.20.6") -> "1.20.6"
    else -> "1.20.1"
} + ".aw"

loom {
    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "../../run"
    }

    accessWidenerPath = file("../../src/main/resources/cesium/$accessWidenerFile")
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "cesium.mixins.json")
    }
}

tasks.processResources {
    inputs.property("version", project.property("mod_version"))

    val properties = mapOf(
        "v_minecraft" to stonecutter.current.project,
        "v_mod" to project.property("mod_version"),
        "v_fabric" to project.property("loader_version"),
        "aw_file" to accessWidenerFile
    )

    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}


val java = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5"))
    JavaVersion.VERSION_21 else JavaVersion.VERSION_17

java {
    withSourcesJar()

    targetCompatibility = java
    sourceCompatibility = java
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(Integer.parseInt(java.majorVersion))
}

tasks.withType<RemapJarTask> {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}