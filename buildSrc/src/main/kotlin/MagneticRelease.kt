import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven

object Magnetic {
    const val GITHUB_REPO = "btwonion/magnetic"
    const val MODRINTH_PROJECT = "LLfA8jAD"
    const val MAVEN_REPO = "https://repo.nyon.dev/releases"
}

val Project.betaNumber: Int?
    get() = property("beta").toString().toIntOrNull()

val Project.featureVersion: String
    get() = property("featureVersion").toString() + (betaNumber?.let { "-beta$it" } ?: "")

val Project.supportedMcVersions: List<String>
    get() = property("vers.supportedMcVersions").toString().split(',').map(String::trim).filter(String::isNotEmpty)

fun Project.useMagneticVersion(platform: String) {
    group = "dev.nyon"
    version = "$featureVersion-${property("vers.versionName")}+$platform"
}

/**
 * Publishes [jar] for [loaders] to Modrinth, GitHub and the nyon Maven repository and registers the
 * target's `release` task. [configure] adds platform-specific settings, such as CurseForge or
 * Modrinth dependencies. Requires the mod-publish and maven-publish plugins.
 */
fun Project.configureMagneticRelease(
    jar: Provider<RegularFile>,
    loaders: List<String>,
    configure: ModPublishExtension.() -> Unit = {}
) {
    extensions.configure<ModPublishExtension> {
        displayName.set("v${project.version}")
        file.set(jar)
        changelog.set("# v${project.version}\n" + rootProject.file("changelog.md").readText())
        type.set(if (betaNumber != null) ReleaseType.BETA else ReleaseType.STABLE)
        modLoaders.addAll(loaders)

        modrinth {
            projectId.set(Magnetic.MODRINTH_PROJECT)
            accessToken.set(providers.environmentVariable("MODRINTH_API_KEY"))
            minecraftVersions.addAll(supportedMcVersions)
        }

        github {
            repository.set(Magnetic.GITHUB_REPO)
            accessToken.set(providers.environmentVariable("GITHUB_TOKEN"))
            commitish.set("master")
        }

        configure()
    }

    extensions.configure<PublishingExtension> {
        repositories.maven(Magnetic.MAVEN_REPO) {
            name = "nyon"
            credentials {
                username = providers.environmentVariable("NYON_USERNAME").orNull
                password = providers.environmentVariable("NYON_PASSWORD").orNull
            }
        }
        publications.create<MavenPublication>("maven") {
            groupId = "dev.nyon"
            artifactId = "magnetic"
            version = project.version.toString()
            from(components["java"])
        }
    }

    tasks.register("release") {
        group = "publishing"
        description = "Publish this target to Modrinth, GitHub and Maven"
        dependsOn("publishMods", "publish")
    }
}
