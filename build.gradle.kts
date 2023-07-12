plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.14.0"
}

mirai {
    this.jvmTarget = JavaVersion.VERSION_11
}

group = "com.pigeonyuze"
version = "2.0.0"


repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.20")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // https://mvnrepository.com/artifact/io.github.kasukusakura/silk-codec
    implementation("io.github.kasukusakura:silk-codec:0.0.5")

    /* For multithreaded functions by annotations */
    implementation("net.jcip:jcip-annotations:1.0")

    /* From yamlkt to hoplite(from: 2.0.0) */
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.4")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.4")
}