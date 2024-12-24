import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version("8.1.1")
    id("application")
}

group = "io.goji"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Vert.x core dependencies
    implementation(platform("io.vertx:vertx-stack-depchain:4.5.11"))
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")


    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")

    // serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // Redis client
    implementation("io.lettuce:lettuce-core:6.3.1.RELEASE")

    // Configuration
    implementation("com.typesafe:config:1.4.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging:3.0.5")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.vertx:vertx-junit5")
}
application {
    mainClass.set("MainKt")
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}


