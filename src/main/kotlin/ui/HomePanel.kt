package ui

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class HomePanel : JPanel(BorderLayout()) {

    init {
        // Tło i marginesy z motywu
        background = Theme.bg
        border = EmptyBorder(32, 32, 32, 32)

        // Kolumna centralna
        val centerBox = Box.createVerticalBox().apply {
            alignmentX = Component.CENTER_ALIGNMENT

            val title = Theme.labelTitle("Ekran startowy").apply {
                alignmentX = Component.CENTER_ALIGNMENT
            }

            val subtitle = Theme.labelSubtitle("Witaj w systemie WSMR!").apply {
                alignmentX = Component.CENTER_ALIGNMENT
            }

            val sep = JSeparator().apply {
                foreground = Theme.grid
                maximumSize = Dimension(520, 1)
                alignmentX = Component.CENTER_ALIGNMENT
            }

            val hint = Theme.labelSubtitle("Tu pojawią się szybkie skróty i podsumowania.").apply {
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
