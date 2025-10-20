package erp

import javax.swing.SwingUtilities
import ui.LoginFrame
import db.Database
import utils.Paths
import updater.UpdateProbe
import updater.UpdateCheck
import updater.DownloadAndVerify
import updater.Install
import java.nio.file.Files
import java.nio.file.Path

fun main() {
    println("🚀 Startuję WSMR, wersja ${Version.current}")

    // Start UI (preloader)
    val frame = LoginFrame()
    frame.isVisible = true

    // Preflight w tle – bez twardych stopów i bez restartu
    Thread {
        try {
            try {
                Database.ensureSchemaAndSeed()
                println("✅ Baza gotowa (schema + seed).")
            } catch (e: Exception) {
                System.err.println("❌ Błąd inicjalizacji bazy: ${e.message}")
                e.printStackTrace()
            }

            try {
                Paths.ensureDirs()
            } catch (e: Exception) {
                System.err.println("⚠️ Nie udało się utworzyć katalogów: ${e.message}")
            }

            UpdateProbe.run()
            UpdateCheck.run(Version.current)
            val zip = DownloadAndVerify.run()
            if (zip != null) {
                Install.installFrom(zip)
                // 🔕 Restart wyłączony – chcemy ZAWSZE przejść do logowania.
                // if (Launcher.launchInstalledAndExitIfFound()) return@Thread
            }
        } finally {
            SwingUtilities.invokeLater {
                // drobna pauza dla płynności wrażenia
                javax.swing.Timer(250) {
                    frame.showLogin()
                }.apply {
                    isRepeats = false
                    start()
                }
            }
        }
    }.start()
}

/* poniżej helpers – bez zmian */
private fun compareVersions(a: String, b: String): Int {
    fun parse(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
    val aa = parse(a)
    val bb = parse(b)
    val max = maxOf(aa.size, bb.size)
    for (i in 0 until max) {
        val x = if (i < aa.size) aa[i] else 0
        val y = if (i < bb.size) bb[i] else 0
        if (x != y) return x - y
    }
    return 0
}

private fun readJsonField(path: Path, key: String): String? {
    return try {
        val txt = Files.readString(path)
        val re = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        re.find(txt)?.groupValues?.get(1)
    } catch (_: Exception) {
        null
    }
}
