import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.20"
    application
}

group = "fergusm"
version = "1.0-SNAPSHOT"

val localProperties = Properties().apply {
    File("./local.properties").reader().use { load(it) }
}
val authToken: String by localProperties

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
        credentials {
            username = authToken
        }
    }
}

val ktor_version: String by project
val logback_version : String by project
val mongo_version : String by project

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("org.mongodb:bson-kotlinx:$mongo_version")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongo_version")
    implementation("com.github.fergusmac:cliniko-kt:0.1.1")
    implementation("mysql:mysql-connector-java:8.0.33")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}