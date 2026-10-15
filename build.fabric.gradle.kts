import io.github.p03w.machete.tasks.OptimizeJarsTask
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    // This plugin applies the correct loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
    id("org.redlance.dima_dencep.gradle.machete")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

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
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions
    loomx.applyMojangMappings()

    // Use `mod{dependency type}` even on 26.1+ - loom-back-compat converts them
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Optional integrations, never bundled
    modCompileOnly("maven.modrinth:modmenu:${property("deps.modmenu")}")
    // Dev runs only, to open the config screen: never bundled or required
    modLocalRuntime("maven.modrinth:modmenu:${property("deps.modmenu")}")
    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    sc.properties.getOrNull<String>("deps.supplementaries")?.let {
        modCompileOnly("maven.modrinth:supplementaries:$it")
        modCompileOnly("maven.modrinth:moonlight:${property("deps.moonlight")}")
        // Dev runs with `-PwithSupplementaries` only, for testing the compat: never bundled or required.
        // Fabric dev runs don't load jars nested in mods (e.g. Moonlight's CodecUI), so those are unpacked and added too.
        if (providers.gradleProperty("withSupplementaries").isPresent) {
            val mods = listOf("maven.modrinth:supplementaries:$it", "maven.modrinth:moonlight:${property("deps.moonlight")}")
            mods.forEach { mod -> modLocalRuntime(mod) }
            val devMods = configurations.create("devSupplementaries") { isTransitive = false }
            mods.forEach { mod -> devMods.dependencies.add(project.dependencies.create(mod)) }
            val archives = serviceOf<ArchiveOperations>() // the configuration cache can't store script references
            modLocalRuntime(files(devMods.elements.map { jars -> jars.flatMap { jar -> archives.zipTree(jar.asFile).matching { include("META-INF/jars/*.jar") }.files } }))
        }
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run") // Shares the run directory between versions
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
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
    test {
        useJUnitPlatform()
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
            register("loader", Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml", "pack.mcmeta")
    }

    // Includes the license file in the built mod
    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    // Shrinks the release jar losslessly (minified JSON/TOML, recompressed PNG); buildAndCollect ships it
    val optimizeJar = register<OptimizeJarsTask>("optimizeJar") {
        input = loomx.modJar.flatMap { it.archiveFile }
        output = layout.buildDirectory.dir("libs/optimized").zip(input) { dir, jar -> dir.file(jar.asFile.name) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        // loomx.mod(Sources)Jar returns the jar task for the applied loom variant
        from(optimizeJar.flatMap { it.output }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
