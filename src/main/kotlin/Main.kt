package erp

import javax.swing.SwingUtilities
import javax.swing.UIManager
import ui.LoginFrame
import db.Database
import ui.Theme

fun main() {
    // 1) Inicjalizacja bazy (schema + seed / migracja)
    try {
        Database.ensureSchemaAndSeed()
        println("✅ Baza gotowa (schema + seed).")
    } catch (e: Exception) {
        System.err.println("❌ Błąd inicjalizacji bazy: ${e.message}")
        e.printStackTrace()
    }

    // 2) Start UI (Look&Feel + Theme)
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) { /* ok */ }

        // 🔹 Globalne kolory/typografia – uruchom przed tworzeniem pierwszych komponentów
        Theme.applyGlobalUI()

        LoginFrame().isVisible = true
    }
}
