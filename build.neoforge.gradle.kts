import io.github.p03w.machete.tasks.OptimizeJarsTask

plugins {
    id("net.neoforged.moddev") version "2.0.147"
    id("neoforge-mutex")
    id("org.redlance.dima_dencep.gradle.machete")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-neoforge"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Optional integrations, never bundled
    sc.properties.getOrNull<String>("deps.supplementaries")?.let {
        compileOnly("maven.modrinth:supplementaries:$it")
        compileOnly("maven.modrinth:moonlight:${property("deps.moonlight")}")
        // Dev runs with `-PwithSupplementaries` only, for testing the compat: never bundled or required
        if (providers.gradleProperty("withSupplementaries").isPresent) {
            runtimeOnly("maven.modrinth:supplementaries:$it")
            runtimeOnly("maven.modrinth:moonlight:${property("deps.moonlight")}")
        }
    }
}

neoForge {
    version = property("deps.neo_loader") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = file("../../run/")
            client()
        }

        register("server") {
            gameDirectory = file("../../run/")
            server()
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    // The config test is loader-independent and runs on the Fabric projects
    test {
        enabled = false
    }

    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            // NeoForge 26.2+ deprecates logoFile in favour of iconFile (square) / bannerFile
            register("icon_key", if (sc.current.parsed >= "26.2") "iconFile" else "logoFile")
        }

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json", "META-INF/mods.toml", "pack.mcmeta")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // Includes the license file in the built mod
    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    // Shrinks the release jar losslessly (minified JSON/TOML, recompressed PNG); buildAndCollect ships it
    val optimizeJar = register<OptimizeJarsTask>("optimizeJar") {
        input = jar.flatMap { it.archiveFile }
        output = layout.buildDirectory.dir("libs/optimized").zip(input) { dir, jar -> dir.file(jar.asFile.name) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(optimizeJar.flatMap { it.output }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
