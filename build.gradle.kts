plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    // PostgreSQL JDBC driver
    implementation("org.postgresql:postgresql:42.7.4")
    // BCrypt do haszowania/ weryfikacji haseł
    implementation("at.favre.lib:bcrypt:0.10.2")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    // jeśli Twój plik to src/main/kotlin/erp/Main.kt -> erp.MainKt
    mainClass.set("erp.MainKt")
}
