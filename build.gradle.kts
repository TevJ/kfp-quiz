import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "com.techtev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}


dependencies {
    testImplementation(kotlin("test"))
    implementation(platform("io.arrow-kt:arrow-stack:2.0.0-SNAPSHOT"))
    implementation("io.arrow-kt:arrow-core")
    implementation("io.arrow-kt:arrow-fx-coroutines")
    implementation("io.arrow-kt:arrow-fx-stm")

    implementation(platform("org.http4k:http4k-bom:4.40.2.0"))
    implementation("org.http4k:http4k-client-jetty")
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-contract")
    implementation("org.http4k:http4k-format-kotlinx-serialization")
    implementation("org.http4k:http4k-format-jackson")
    implementation("org.http4k:http4k-server-jetty")

    implementation(platform("org.jetbrains.exposed:exposed-bom:0.40.1"))
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.jetbrains.exposed:exposed-jdbc")

    implementation("org.mindrot:jbcrypt:0.4")

    runtimeOnly("com.h2database:h2:2.1.214")

    implementation("ch.qos.logback:logback-classic:1.4.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.languageVersion = "1.8"
}