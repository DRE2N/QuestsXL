import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.3.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

val papyrusVersion = "26.1.2-SNAPSHOT"

dependencies {
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }
    compileOnly("de.erethon.aergia:Aergia:1.0.1") { isTransitive = false }
    compileOnly("de.erethon.hephaestus:Hephaestus:1.0.5-SNAPSHOT")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.3.0") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.3.0") { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    compileOnly("de.erethon.hecate:Hecate:1.2-SNAPSHOT")
    compileOnly("de.erethon.tyche:Tyche:1.0-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "de.erethon.questsxl.tool.DocGenerator",
                "paperweight-mappings-namespace" to "mojang"
            )
        }
    }

    shadowJar {
        archiveBaseName.set("QuestsXL")
        dependencies {
            include(dependency("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r"))
        }
        relocate("org.eclipse.jgit", "de.erethon.questsxl.jgit")
    }

    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar")
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-26.1.2.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        runDirectory.set(file("C:\\Dev\\Erethon"))
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    bukkit {
        load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
        main = "de.erethon.questsxl.QuestsXL"
        apiVersion = "1.21"
        name = "QuestsXL"
        authors = listOf("Malfrador", "Fyreum")
        depend = listOf("Hecate", "Aergia")
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

            artifact(tasks.named("shadowJar").get()) {
                classifier = ""
            }
        }
    }
}