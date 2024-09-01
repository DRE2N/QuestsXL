import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

repositories {
    mavenLocal()
    maven("https://erethon.de/repo")
    maven("https://jitpack.io")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
    maven("https://papermc.io/repo/repository/maven-public/")
    mavenCentral()
}
plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5" // Use fork until shadow has updated to Java 21
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon.questsxl"
version = "1.0.0-SNAPSHOT"
description = "Quest & World event plugin for Erethon"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val papyrusVersion = "1.21.1-R0.1-SNAPSHOT"

dependencies {
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }
    compileOnly("de.erethon.aether:Aether:1.0.0-SNAPSHOT")
    compileOnly("de.erethon.aergia:Aergia:1.0.0-SNAPSHOT") { isTransitive = false }
    compileOnly("de.fyreum:JobsXL:1.0-SNAPSHOT") { isTransitive = false }
    // Why... just why.
   /* compileOnly("de.erethon.dungeonsxl:dungeonsxl-api:0.18-PRE-02") {
        exclude(group = "net.kyori.adventure.text", module = "minimessage")
    }*/
    implementation("de.erethon:bedrock:1.4.0") { isTransitive = false }
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.3.0") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.3.0") { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "${project.group}"
            artifactId = "QuestsXL"
            version = "${project.version}"
            from(components["java"])
        }
    }
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
        dependsOn(shadowJar)
    }

    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-mojmap.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(21)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    /*
    reobfJar {
      // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
      // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
      outputJar.set(layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar"))
    }
     */

    shadowJar {
        dependencies {
            include(dependency("de.erethon:bedrock:1.4.0"))
            include(dependency("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r"))
        }
        relocate("de.erethon.bedrock", "de.erethon.questsxl.bedrock")
        relocate("org.eclipse.jgit", "de.erethon.questsxl.jgit")
    }
    bukkit {
        load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
        main = "de.erethon.questsxl.QuestsXL"
        apiVersion = "1.21"
        authors = listOf("Malfrador", "Fyreum")
        commands {
            register("quests") {
                description = "Main command for QXL"
                aliases = listOf("q", "qxl")
                permission = "qxl.cmd"
                usage = "/qxl help"
            }
        }
        softDepend = listOf("Aether", "Aergia")
    }
}
