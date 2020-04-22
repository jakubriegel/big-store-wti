plugins {
    application
    kotlin("jvm") version "1.3.71"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "eu.jrie.put.wti.bigstore"
version = "1.0"

application {
    mainClassName = "eu.jrie.put.wti.bigstore.AppKt"
}

repositories {
    mavenCentral()
}

val kotlinVersion = "1.3.71"
val ktorVersion = "1.3.1"

dependencies {
    // kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // ktor
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-metrics:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")

    // redis
    implementation("io.lettuce:lettuce-core:5.2.0.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.3.5")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")

    // cassandra
    implementation("com.datastax.cassandra:cassandra-driver-core:4.0.0")

    // jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.3")

    // util
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // test
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.1")
    testImplementation("io.mockk:mockk:1.9.3")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        useJUnitPlatform()
    }
    shadowJar {
        archiveAppendix.set("")
        archiveClassifier.set("")
    }
}