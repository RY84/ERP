plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    // jeśli Twój plik to src/main/kotlin/erp/Main.kt -> erp.MainKt
    mainClass.set("erp.MainKt")
}
