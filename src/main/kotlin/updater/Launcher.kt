package updater

import utils.Paths
import java.io.File

/**
 * Uruchamia zainstalowany JAR w osobnym procesie i kończy bieżący proces.
 * – używa tej samej Javy co obecny proces
 * – jako working dir ustawia bieżący user.dir (żeby działał config.properties)
 */
object Launcher {

    /** Wyszukuje główny JAR klienta w katalogu instalacyjnym. */
    private fun findMainJar(): File? {
        val appDir = Paths.appDir
        if (!appDir.exists() || !appDir.isDirectory) return null

        val jars = appDir.listFiles { f -> f.isFile && f.name.endsWith(".jar", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

        // preferuj cienie (shaded) w stylu ERP-*-all.jar
        val preferred = jars.firstOrNull { it.name.matches(Regex("(?i)erp-.*-all\\.jar")) }
        return preferred ?: jars.firstOrNull()
    }

    /**
     * Próbuje uruchomić zainstalowany JAR, a jeśli się uda – kończy bieżący proces.
     * Zwraca true, gdy wystartowano nowy proces.
     */
    fun launchInstalledAndExitIfFound(): Boolean {
        val jar = findMainJar() ?: run {
            println("⚠️  Launcher: brak zainstalowanego JAR-a do uruchomienia.")
            return false
        }

        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        val workDir = File(System.getProperty("user.dir")) // gdzie leży Twój config.properties

        val cmd = listOf(
            javaBin,
            "-jar",
            jar.absolutePath        // ← poprawka (File → absolutePath)
        )

        println("🚗 Uruchamiam nową instancję: $cmd")
        println("   workingDir: ${workDir.absolutePath}")

        return try {
            ProcessBuilder(cmd)
                .directory(workDir)
                .inheritIO()         // przejmij STDOUT/STDERR do bieżącej konsoli
                .start()

            println("✅ Nowy proces wystartował. Kończę bieżący proces (System.exit(0)).")
            System.exit(0)
            true
        } catch (e: Exception) {
            System.err.println("❌ Błąd uruchamiania nowej instancji: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
