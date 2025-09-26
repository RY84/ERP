package erp

import javax.swing.SwingUtilities
import javax.swing.UIManager
import ui.LoginFrame
import db.UserDao

fun main() {
    // 1) Inicjalizacja bazy: tabela users + seed admin/admin (jeśli brak)
    try {
        UserDao.ensureSchemaAndSeed()
        println("✅ Baza gotowa (schema + seed).")
    } catch (e: Exception) {
        System.err.println("❌ Błąd inicjalizacji bazy: ${e.message}")
        e.printStackTrace()
    }

    // 2) Start UI
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) { /* ok */ }

        LoginFrame().isVisible = true
    }
}
