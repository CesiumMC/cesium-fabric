import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("fabric-loom") version "1.9.2"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

base.archivesName.set("${project.property("archives_base_name")}+${project.property("minecraft_version")}")

version = project.property("mod_version").toString()
group = project.property("maven_group").toString()

repositories {
    mavenCentral()
}

loom {
    accessWidenerPath = file("src/main/resources/cesium.accesswidener")
}

dependencies {
    // Declare Minecraft version and use Mojang's mappings
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    // Fabric stuff
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Declare dependencies
    implementation(project.property("lib_zstd").toString())
    implementation(project.property("lib_lmdb").toString())

    // Include dependencies and transitives in jar file, good luck keeping those versions up to date <3
    include(project.property("lib_zstd").toString())
    include(project.property("lib_lmdb").toString())

    include("com.github.jnr:jffi:1.3.12")
    include("com.github.jnr:jffi:1.3.12:native")
    include("com.github.jnr:jnr-a64asm:1.0.0")
    include("com.github.jnr:jnr-constants:0.10.4")
    include("com.github.jnr:jnr-ffi:2.2.15")
    include("com.github.jnr:jnr-x86asm:1.0.2")
}

tasks.processResources {
    inputs.property("version", project.property("mod_version"))

    filesMatching("fabric.mod.json") {
        expand("version" to project.property("mod_version"))
    }
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<RemapJarTask> {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}