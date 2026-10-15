import io.github.p03w.machete.tasks.OptimizeJarsTask
import io.github.pacifistmc.forgix.Forgix

plugins {
    id("dev.kikugie.stonecutter")
    id("io.github.pacifistmc.forgix") version "2.0.0" apply false // only its loader merge API is used
    id("org.redlance.dima_dencep.gradle.machete") version "2.0.0"
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    // Adds constants to Stonecutter comments (i.e. for `//? if fabric {...`)
    constants {
        match(loader, "fabric", "forge", "neoforge")
    }

    // Supplementaries only exists for some versions; its compat mixin compiles only there
    constants["supplementaries"] = properties.getOrNull<String>("deps.supplementaries") != null

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}

// One jar per Minecraft version holding all its loaders: Forgix merges them, Machete optimizes the result.
// Run `./gradlew mergedJars`; the jars land in `build/libs/`.
val modVersion = Regex("""mod\.version = "(.+)"""").find(file("stonecutter.properties.toml").readText())!!.groupValues[1]

val mergedJars = tasks.register("mergedJars") {
    group = "build"
    description = "Builds one optimized jar per Minecraft version, holding all its loaders, into `build/libs/`"
}

stonecutter.tree.nodes.map { it.project }.groupBy { it.name.substringBefore('-') }.forEach { (mc, projects) ->
    val name = mc.replace('.', '_')
    val merge = tasks.register("mergeLoaders_$name") {
        dependsOn(projects.map { "${it.path}:optimizeJar" })
        val jars = projects.associate { project ->
            val loader = project.name.substringAfter('-')
            loader to project.layout.buildDirectory.file("libs/optimized/chickensshed-$loader-$modVersion+$mc.jar")
        }
        val output = layout.buildDirectory.file("forgix/chickensshed-$modVersion+$mc.jar")
        inputs.files(jars.values)
        outputs.file(output)
        doLast { Forgix.mergeLoaders(jars.entries.associate { (loader, jar) -> jar.get().asFile to loader }, output.get().asFile, true) }
    }
    val optimize = tasks.register<OptimizeJarsTask>("optimizeMerged_$name") {
        input.fileProvider(merge.map { it.outputs.files.singleFile })
        output = layout.buildDirectory.file("libs/chickensshed-$modVersion+$mc.jar")
    }
    mergedJars { dependsOn(optimize) }
}
