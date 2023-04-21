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
version = "1.7.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies{
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.20")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // https://mvnrepository.com/artifact/io.github.kasukusakura/silk-codec
    implementation("io.github.kasukusakura:silk-codec:0.0.5")

    implementation("net.mamoe.yamlkt:yamlkt-jvm:0.12.0") //yaml
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}
