plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    val modPublish = libs.plugins.mod.publish.get()
    implementation("${modPublish.pluginId}:${modPublish.pluginId}.gradle.plugin:${modPublish.version.requiredVersion}")
}
