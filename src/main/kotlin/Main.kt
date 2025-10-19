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
    // Aktualna wersja klienta — MUSI odpowiadać wersji zbudowanego JAR-a
    const val current = "1.0.1"
}

fun main() {
    println("🚀 Startuję WSMR, wersja ${Version.current}")

    // 1) Inicjalizacja bazy danych (schema + seed)
    try {
        Database.ensureSchemaAndSeed()
        println("✅ Baza gotowa (schema + seed).")
    } catch (e: Exception) {
        System.err.println("❌ Błąd inicjalizacji bazy: ${e.message}")
        e.printStackTrace()
    }

    // 2) Upewnij się, że katalogi konfiguracyjne istnieją
    try {
        Paths.ensureDirs()
    } catch (e: Exception) {
        System.err.println("⚠️ Nie udało się utworzyć katalogów: ${e.message}")
        e.printStackTrace()
    }

    // 3) Pobierz metadane aktualizacji (app-version.json)
    UpdateProbe.run()

    // 4) Porównaj wersję lokalną z najnowszą
    UpdateCheck.run(Version.current)

    // 5) Pobierz i zweryfikuj paczkę ZIP (jeśli jest nowa wersja)
    val downloadedZip = DownloadAndVerify.run()

    if (downloadedZip != null) {
        // 6) Zainstaluj pobraną wersję
        Install.installFrom(downloadedZip)

        // 7) Uruchom nową wersję i zakończ bieżący proces
        Launcher.launchInstalledAndExitIfFound()
        return
    }

    // 8) Jeśli nie było aktualizacji, uruchom UI logowania
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {
            // pomijamy błędy L&F
        }

        Theme.applyGlobalUI()
        LoginFrame().isVisible = true
    }
}
