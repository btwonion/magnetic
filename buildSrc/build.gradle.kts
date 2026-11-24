plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // On the build classpath here, so mod and Paper targets apply it by id without a version.
    val modPublish = libs.plugins.mod.publish.get()
    implementation("${modPublish.pluginId}:${modPublish.pluginId}.gradle.plugin:${modPublish.version.requiredVersion}")
}
