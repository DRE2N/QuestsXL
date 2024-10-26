plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("https://erethon.de/repo")
    maven("https://jitpack.io")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    mavenCentral()
}

allprojects {
    group = "de.erethon.questsxl"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenLocal()
        maven("https://erethon.de/repo")
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://papermc.io/repo/repository/maven-public/")
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

val papyrusVersion = "1.21.1-R0.1-SNAPSHOT"


dependencies {
    annotationProcessor("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    annotationProcessor("de.erethon:bedrock:1.4.0") { isTransitive = false }
    annotationProcessor("de.erethon.aether:Aether:1.0.0-SNAPSHOT")
    annotationProcessor("de.erethon.aergia:Aergia:1.0.0-SNAPSHOT") { isTransitive = false }
    annotationProcessor("de.fyreum:JobsXL:1.0-SNAPSHOT") { isTransitive = false }
    annotationProcessor("de.erethon.hephaestus:Hephaestus:1.0-SNAPSHOT")
    annotationProcessor("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    annotationProcessor("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.3.0") { isTransitive = false }
    annotationProcessor("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.3.0") { isTransitive = false }
    annotationProcessor("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
}

tasks.register<JavaCompile>("runAnnotationProcessor") {
    dependsOn(":doc-gen:classes", ":plugin:classes") // Ensure doc-gen and plugin are compiled first
    source = fileTree("plugin/src/main/java")
    classpath = files(
        sourceSets["main"].runtimeClasspath,
        project(":doc-gen").sourceSets["main"].output,
        project(":plugin").sourceSets["main"].output,
        configurations.runtimeClasspath
    )
    destinationDirectory.set(file("plugin/build/classes/java/main"))
    options.annotationProcessorPath = files(
        project(":doc-gen").sourceSets["main"].output,
        project(":plugin").sourceSets["main"].output,
        configurations["annotationProcessor"]
    )
    options.compilerArgs.add("-processor")
    options.compilerArgs.add("de.erethon.questsxl.QDocGenerator")
    options.encoding = "UTF-8"
    options.release.set(21)
}

subprojects {
    apply(plugin = "java-library")

    tasks.withType<JavaCompile> {
        options.annotationProcessorPath = configurations["annotationProcessor"]
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}