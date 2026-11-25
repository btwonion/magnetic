plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
