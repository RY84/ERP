package updater

import utils.Paths
import java.io.File

/**
 * Uruchamia zainstalowany JAR w osobnym procesie i kończy bieżący proces.
 * - używa tej samej Javy co obecny proces
 * - jako working dir ustawia bieżący user.dir (żeby działał config.properties)
 */
object Launcher {

    fun launchInstalledAndExitIfFound(): Boolean {
        val jar = Install.findMainJar() ?: run {
            println("ℹ️ Launcher: brak zainstalowanego JAR-a do uruchomienia.")
            return false
        }

        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        val workDir = File(System.getProperty("user.dir")) // gdzie leży Twój config.properties

        val cmd = listOf(
            javaBin,
            "-jar",
            jar.toAbsolutePath().toString()
        )

        println("🚗 Uruchamiam nową instancję: $cmd")
        println("   workingDir: ${workDir.absolutePath}")

        try {
            ProcessBuilder(cmd)
                .directory(workDir)
                .inheritIO()      // przekazuje IO do konsoli
                .start()
            println("✅ Nowy proces wystartował. Kończę bieżący proces.")
            // eleganckie zamknięcie
            Runtime.getRuntime().halt(0) // bez wywoływania shutdown hooków z IDE
            @Suppress("UNREACHABLE_CODE")
            return true
        } catch (e: Exception) {
            System.err.println("❌ Launcher: nie udało się uruchomić nowej instancji: ${e.message}")
            return false
        }
    }
}
