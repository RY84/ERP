package erp

import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("ERP — okno startowe")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.minimumSize = Dimension(800, 600)

        // Pasek menu
        val menuBar = JMenuBar()
        val fileMenu = JMenu("Plik")
        val exitItem = JMenuItem(object : AbstractAction("Wyjście") {
            override fun actionPerformed(e: ActionEvent?) {
                frame.dispose()
            }
        })
        fileMenu.add(exitItem)
        menuBar.add(fileMenu)
        frame.jMenuBar = menuBar

        // Treść
        val label = JLabel("Witaj w ERP 👋", SwingConstants.CENTER)
        frame.contentPane.add(label)

        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}
