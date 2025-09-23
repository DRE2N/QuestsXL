plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("https://repo.erethon.de/snapshots")
    maven("https://jitpack.io")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

allprojects {
    group = "de.erethon.questsxl"
    version = "1.0.5-SNAPSHOT"

    repositories {
        mavenLocal()
        maven("https://repo.erethon.de/snapshots")
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }

    apply(plugin = "java-library")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }
}

val papyrusVersion = "1.21.8-R0.1-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")

    tasks.register<Copy>("deployToSharedServer") {
        doNotTrackState("")
        group = "Erethon"
        description = "Used for deploying the plugin to the shared server. runServer will do this automatically." +
                "This task is only for manual deployment when running runServer from another plugin."
        dependsOn(":plugin:shadowJar")
        from(project(":plugin").layout.buildDirectory.file("libs/QuestsXL-$version-all.jar"))
        into("C:\\Dev\\Erethon\\plugins")
    }
}