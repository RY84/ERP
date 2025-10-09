plugins {
    kotlin("jvm") version "1.9.24"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // fat JAR
    // id("org.beryx.jlink") version "2.26.0"              // (na przyszłość)
}

group = "erp"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("at.favre.lib:bcrypt:0.10.2")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("erp.MainKt")
}

/** Manifest Main-Class dla wszystkich zadań Jar (obejmuje też shadowJar) */
tasks.withType(org.gradle.jvm.tasks.Jar::class.java).configureEach {
    manifest {
        attributes(mutableMapOf("Main-Class" to application.mainClass.get()))
    }
}

/** Kopiuje config.example.properties -> config.properties (jeśli brak) */
tasks.register("copyConfigExample", Copy::class.java) {
    description = "Kopiuje config.example.properties do config.properties jeśli nie istnieje"
    from(layout.projectDirectory.file("config.example.properties"))
    into(layout.projectDirectory)
    rename { "config.properties" }
    onlyIf { !layout.projectDirectory.file("config.properties").asFile.exists() }
}

/** Zależności pomocnicze */
tasks.named("run").configure {
    dependsOn("copyConfigExample")
}
tasks.named("shadowJar").configure {
    dependsOn("copyConfigExample")
}

/*
==================== jlink + jpackage (na przyszłość) ====================
Odkomentujemy później, żeby budować instalatory (.msi/.dmg/.deb)
jlink {
    // ...
}
tasks.register("buildInstaller") {
    group = "distribution"
    dependsOn("jpackage")
}
==========================================================================
*/
