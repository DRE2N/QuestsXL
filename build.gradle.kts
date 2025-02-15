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
    version = "1.0.0-SNAPSHOT"

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

val papyrusVersion = "1.21.4-R0.1-SNAPSHOT"
val pluginVersion = "1.0.0-SNAPSHOT"


dependencies {
    annotationProcessor("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
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
    dependsOn(":doc-gen:classes", ":plugin:classes")
    group = "QuestsXL"
    source = fileTree("plugin/src/main/java")
    classpath = files(
        sourceSets["main"].runtimeClasspath,
        project(":doc-gen").sourceSets["main"].output,
        project(":plugin").sourceSets["main"].output,
        configurations.runtimeClasspath,
        project(":plugin").configurations["compileClasspath"],
        project(":plugin").configurations["runtimeClasspath"]
    )
    destinationDirectory.set(file("plugin/build/classes/java/main"))
    options.annotationProcessorPath = files(
        project(":doc-gen").sourceSets["main"].output,
        configurations["annotationProcessor"],
        project(":plugin").sourceSets["main"].output
    )
    options.compilerArgs.add("-processor")
    options.compilerArgs.add("de.erethon.questsxl.QDocGenerator")
    options.compilerArgs.add("-AdocOutputDir=${project.rootDir}/docs")
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.register<Javadoc>("generatePluginJavadocs") {
    source = fileTree("plugin/src/main/java")
    classpath = files(
        project(":plugin").sourceSets["main"].runtimeClasspath,
        project(":plugin").sourceSets["main"].output,
        project(":plugin").configurations["compileClasspath"]
    )
    setDestinationDir(file("plugin/build/docs/javadoc"))
    options.encoding = "UTF-8"
}

tasks.register<Jar>("javadocJar") {
    group = "QuestsXL"
    dependsOn("generatePluginJavadocs")
    archiveClassifier.set("javadoc")
    from(tasks.named<Javadoc>("generatePluginJavadocs").get().destinationDir)
}

subprojects {
    apply(plugin = "java-library")

    tasks.withType<JavaCompile> {
        options.annotationProcessorPath = configurations["annotationProcessor"]
    }
    tasks.register<Copy>("deployToSharedServer") {
        group = "Erethon"
        description = "Used for deploying the plugin to the shared server. runServer will do this automatically." +
                "This task is only for manual deployment when running runServer from another plugin."
        dependsOn(":plugin:shadowJar")
        from(project(":plugin").layout.buildDirectory.file("libs/QuestsXL-$pluginVersion-all.jar"))
        into("C:\\Dev\\Erethon\\plugins")
    }
}

    publishing {
        repositories {
            maven {
                name = "erethon"
                url = uri("https://repo.erethon.de/snapshots")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "${project.group}"
                artifactId = "QuestsXL"
                version = "${project.version}"

                from(components["java"])
                artifact(tasks["javadocJar"])
            }
        }
    }