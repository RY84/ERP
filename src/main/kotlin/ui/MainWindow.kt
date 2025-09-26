package ui

import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants

class MainWindow : JFrame("ERP — okno robocze") {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 700)
        setLocationRelativeTo(null)

        add(JLabel("Witaj w aplikacji ERP!", SwingConstants.CENTER))
    }
}
