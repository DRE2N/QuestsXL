import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev")
    id("com.gradleup.shadow")
    id("net.minecrell.plugin-yml.bukkit")
}

val papyrusVersion = "26.1.2-SNAPSHOT"
val hermesWebImage = providers.gradleProperty("hermesWebImage")
    .orElse(providers.environmentVariable("HERMES_WEB_IMAGE"))
    .orElse("ghcr.io/malfrador/hermes-web:latest")

dependencies {
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }
    compileOnly(project(":plugin"))
    compileOnly("de.erethon.hecate:Hecate:1.3-SNAPSHOT")
    compileOnly("de.erethon.aether:Aether:1.0.2-SNAPSHOT")
    compileOnly("de.erethon.hephaestus:Hephaestus:26.1-SNAPSHOT")
    compileOnly("de.erethon.factions:Factions:1.0-SNAPSHOT")
    compileOnly("de.erethon:Daedalus:1.4-SNAPSHOT")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.3.0") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.3.0") { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks {
    val webEditorDir = layout.projectDirectory.dir("web")
    val deployDir = layout.projectDirectory.dir("deploy")
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    val installWebEditor by registering(Exec::class) {
        group = "build"
        description = "Installs Hermes web editor dependencies with Bun."
        workingDir = webEditorDir.asFile
        commandLine(if (isWindows) listOf("cmd", "/c", "bun", "install", "--frozen-lockfile") else listOf("bun", "install", "--frozen-lockfile"))
        inputs.file(webEditorDir.file("package.json"))
        inputs.file(webEditorDir.file("bun.lock"))
        outputs.dir(webEditorDir.dir("node_modules"))
    }

    val buildWebEditor by registering(Exec::class) {
        group = "build"
        description = "Builds the standalone Hermes web editor."
        dependsOn(installWebEditor)
        workingDir = webEditorDir.asFile
        commandLine(if (isWindows) listOf("cmd", "/c", "bun", "run", "build") else listOf("bun", "run", "build"))
        inputs.dir(webEditorDir.dir("src"))
        inputs.file(webEditorDir.file("index.html"))
        inputs.file(webEditorDir.file("package.json"))
        inputs.file(webEditorDir.file("vite.config.ts"))
        outputs.dir(webEditorDir.dir("dist"))
    }

    val dockerBuildWeb by registering(Exec::class) {
        group = "docker"
        description = "Builds the standalone Hermes web/controller Docker image."
        workingDir = webEditorDir.asFile
        commandLine(if (isWindows) {
            listOf("cmd", "/c", "docker", "build", "-f", "..\\deploy\\Dockerfile", "-t", hermesWebImage.get(), ".")
        } else {
            listOf("docker", "build", "-f", "../deploy/Dockerfile", "-t", hermesWebImage.get(), ".")
        })
        inputs.file(deployDir.file("Dockerfile"))
        inputs.file(webEditorDir.file("package.json"))
        inputs.file(webEditorDir.file("bun.lock"))
        inputs.file(webEditorDir.file("index.html"))
        inputs.file(webEditorDir.file("tsconfig.json"))
        inputs.file(webEditorDir.file("vite.config.ts"))
        inputs.file(webEditorDir.file("server.ts"))
        inputs.dir(webEditorDir.dir("src"))
        doNotTrackState("Docker image builds are side effects outside Gradle's output tracking.")
    }

    val dockerPushWeb by registering(Exec::class) {
        group = "docker"
        description = "Pushes the standalone Hermes web/controller Docker image."
        dependsOn(dockerBuildWeb)
        commandLine(if (isWindows) {
            listOf("cmd", "/c", "docker", "push", hermesWebImage.get())
        } else {
            listOf("docker", "push", hermesWebImage.get())
        })
        doNotTrackState("Docker image pushes are remote side effects outside Gradle's output tracking.")
    }

    assemble {
        dependsOn(shadowJar)
    }

    named("build") {
        dependsOn(dockerPushWeb)
    }

    shadowJar {
        archiveBaseName.set("Hermes")
        dependencies {
            include(dependency("com.google.code.gson:gson:2.11.0"))
            include(dependency("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r"))
        }
        relocate("org.eclipse.jgit", "de.erethon.hermes.jgit")
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    register<Copy>("deployToSharedServer") {
        doNotTrackState("")
        group = "Erethon"
        description = "Deploys Hermes to the shared server."
        dependsOn(shadowJar)
        from(layout.buildDirectory.file("libs/Hermes-$version-all.jar"))
        into("C:\\Dev\\Erethon\\plugins")
    }

    bukkit {
        load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
        main = "de.erethon.hermes.Hermes"
        apiVersion = "1.21"
        name = "Hermes"
        authors = listOf("Malfrador", "Fyreum")
        depend = listOf("Hecate", "QuestsXL")
        softDepend = listOf("Aether", "Hephaestus", "Spellbook", "Factions", "Daedalus")
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
            artifactId = "Hermes"
            version = "${project.version}"

            artifact(tasks.named("shadowJar").get()) {
                classifier = ""
            }
        }
    }
}
