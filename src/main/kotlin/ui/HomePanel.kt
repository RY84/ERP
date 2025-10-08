package ui

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class HomePanel : JPanel(BorderLayout()) {

    init {
        // Tło i margines
        background = Color(20, 20, 20)
        border = EmptyBorder(32, 32, 32, 32)

        // Kolumna centralna (bez logiki — tylko statyczny ekran startowy)
        val centerBox = Box.createVerticalBox().apply {
            alignmentX = Component.CENTER_ALIGNMENT

            val title = JLabel("Ekran startowy").apply {
                foreground = Color(235, 235, 235)
                font = font.deriveFont(Font.BOLD, 28f)
                alignmentX = Component.CENTER_ALIGNMENT
            }

            val subtitle = JLabel("Witaj w systemie WSMR!").apply {
                foreground = Color(200, 200, 200)
                font = font.deriveFont(Font.PLAIN, 16f)
                alignmentX = Component.CENTER_ALIGNMENT
            }

            val sep = JSeparator().apply {
                foreground = Color(70, 70, 70)
                maximumSize = Dimension(520, 1)
                alignmentX = Component.CENTER_ALIGNMENT
            }

            val hint = JLabel("Tu pojawią się szybkie skróty i podsumowania.").apply {
                foreground = Color(180, 180, 180)
                font = font.deriveFont(14f)
                alignmentX = Component.CENTER_ALIGNMENT
            }

            add(title)
            add(Box.createVerticalStrut(8))
            add(subtitle)
            add(Box.createVerticalStrut(18))
            add(sep)
            add(Box.createVerticalStrut(18))
            add(hint)
        }

        // Wyśrodkowanie w pionie/poziomie
        val centerWrap = JPanel(GridBagLayout()).apply {
            isOpaque = false
            add(centerBox)
        }

        add(centerWrap, BorderLayout.CENTER)
    }
}
