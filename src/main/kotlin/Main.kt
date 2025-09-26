import javax.swing.SwingUtilities
import javax.swing.UIManager
import ui.LoginFrame

fun main() {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) { /* ok */ }

        LoginFrame().isVisible = true
    }
}
