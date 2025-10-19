package erp

import javax.swing.SwingUtilities
import javax.swing.UIManager
import ui.LoginFrame
import db.Database
import ui.Theme
import utils.Paths
import updater.UpdateProbe
import updater.UpdateCheck
import updater.DownloadAndVerify
import updater.Install
import updater.Launcher

object Version {
    const val current = "1.0.1"
}

fun main() {
    println("🚀 Startuję WSMR, wersja ${Version.current}")

    // 1) Inicjalizacja bazy (schema + seed / migracja)
    try {
        Database.ensureSchemaAndSeed()
        println("✅ Baza gotowa (schema + seed).")
    } catch (e: Exception) {
        System.err.println("❌ Błąd inicjalizacji bazy: ${e.message}")
        e.printStackTrace()
    }

    // 2) Katalogi (config/log/tmp/app)
    try {
        Paths.ensureDirs()
    } catch (e: Exception) {
        System.err.println("⚠️ Nie udało się utworzyć katalogów: ${e.message}")
        e.printStackTrace()
    }

    // 3) Sonda: pobierz surowy JSON i wypisz (dla diagnostyki)
    UpdateProbe.run()

    // 4) PORÓWNANIE WERSJI – tylko decyzja (bez pobierania ZIP-a)
    UpdateCheck.run(Version.current)

    // 5) AUTO-UPDATE – pobierz ZIP i zweryfikuj SHA256
    DownloadAndVerify.run()

    // 6) INSTALACJA – rozpakuj ZIP do katalogu aplikacji
    val zipPath = Paths.tmpDir.resolve("client-${Version.current}.zip").toPath()
    Install.installFrom(zipPath)

    // 7) RESTART – uruchom zainstalowany JAR i zakończ bieżący proces (jeśli wszystko poprawne)
    Launcher.launchInstalledAndExitIfFound()

    // 8) UI – start logowania (jeśli nie było potrzeby restartu)
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {
            // pomijamy
        }

        Theme.applyGlobalUI()
        LoginFrame().isVisible = true
    }
}
