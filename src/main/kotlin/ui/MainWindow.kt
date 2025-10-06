package ui

import java.awt.*
import javax.swing.*

class MainWindow : JFrame("Wszystko sam muszę robić...") {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 700)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        // ===== Pasek u góry — styl jak w LoginFrame (ciemny, półprzezroczysty) =====
        val topBar = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = Color(0, 0, 0, 180)     // półprzezroczyste tło
            border = BorderFactory.createEmptyBorder(10, 12, 10, 12) // padding
            preferredSize = Dimension(0, 60)     // docelowa wysokość ~60 px
        }

        // Prawa strona paska: przycisk WYLOGUJ
        val rightBox = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            isOpaque = false
        }

        val btnLogout = JButton("Wyloguj").apply {
            isOpaque = true
            background = Color(0, 0, 0, 180)     // ten sam ciemny „pasek” pod guzikiem
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 14f)
            border = BorderFactory.createEmptyBorder(10, 16, 10, 16)
            isFocusable = false
            addActionListener {
                dispose()
                LoginFrame().isVisible = true
            }
        }
        rightBox.add(btnLogout)

        // (Opcjonalnie) lewa etykieta tytułu sekcji — spójna z LoginFrame
        val lblTitle = JLabel("ERP — okno robocze").apply {
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 16f)
        }

        topBar.add(lblTitle, BorderLayout.WEST)
        topBar.add(rightBox, BorderLayout.EAST)
        add(topBar, BorderLayout.NORTH)
        // ==========================================================================

        // Środek – bez zmian
        add(JLabel("Witaj w aplikacji ERP!", SwingConstants.CENTER), BorderLayout.CENTER)
    }
}
