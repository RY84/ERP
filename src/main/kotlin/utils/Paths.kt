package utils

import java.io.File

object Paths {

    val os = System.getProperty("os.name").lowercase()

    // Główne ścieżki zależne od systemu
    val baseDir: File by lazy {
        when {
            os.contains("win") -> File(System.getenv("APPDATA"), "WSMR")
            os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/WSMR")
            else -> File(System.getenv("XDG_CONFIG_HOME") ?: "${System.getProperty("user.home")}/.config", "wsmr")
        }
    }

    val logsDir: File by lazy {
        if (os.contains("mac")) File(System.getProperty("user.home"), "Library/Logs/WSMR")
        else File(baseDir, "logs")
    }

    val tmpDir: File by lazy {
        if (os.contains("mac")) File(System.getProperty("user.home"), "Library/Caches/WSMR")
        else File(baseDir, "tmp")
    }

    val appDir: File by lazy {
        when {
            os.contains("win") -> File(System.getenv("LOCALAPPDATA"), "Programs/WSMR/app")
            os.contains("mac") -> File(System.getProperty("user.home"), "Applications/WSMR")
            else -> File(System.getProperty("user.home"), ".local/share/wsmr/app")
        }
    }

    fun ensureDirs() {
        listOf(baseDir, logsDir, tmpDir, appDir).forEach {
            if (!it.exists()) it.mkdirs()
        }
        println("📂 WSMR directories verified:")
        println("  Base:  ${baseDir.absolutePath}")
        println("  Logs:  ${logsDir.absolutePath}")
        println("  Temp:  ${tmpDir.absolutePath}")
        println("  App:   ${appDir.absolutePath}")
    }
}
