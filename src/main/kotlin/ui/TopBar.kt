package ui

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder

class TopBar(
    username: String,
    private val onHome: () -> Unit,
    private val onUsers: () -> Unit,
    private val onLogout: () -> Unit
) : JPanel(BorderLayout()) {

    private val barHeight = 56
    private val iconSize = 44
    private val iconPad = 6
    private val accentGreen = Color(0, 205, 0)

    init {
        isOpaque = true
        background = Theme.panel
        border = EmptyBorder(1, 12, 1, 12)

        val left = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply {
            isOpaque = false
            alignmentY = Component.CENTER_ALIGNMENT
        }
        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0)).apply {
            isOpaque = false
            alignmentY = Component.CENTER_ALIGNMENT
        }

        // --- HOME ---
        loadIconScaled("/home.png", iconSize, iconSize)?.let {
            left.add(iconButton(it, "Strona główna") { onHome() })
        }

        // --- Username + Ikony po prawej ---
        right.add(JLabel(username).apply {
            foreground = accentGreen
            font = font.deriveFont(Font.BOLD, 16f)
            isOpaque = false
            toolTipText = "Zalogowano jako $username"
        })

        loadIconScaled("/user.png", iconSize, iconSize)?.let {
            right.add(iconButton(it, "Użytkownicy") { onUsers() })
        }
        loadIconScaled("/logout.png", iconSize, iconSize)?.let {
            right.add(iconButton(it, "Wyloguj") { onLogout() })
        }

        add(left, BorderLayout.WEST)
        add(right, BorderLayout.EAST)

        preferredSize = Dimension(1000, barHeight)
        minimumSize = preferredSize
        maximumSize = Dimension(Int.MAX_VALUE, barHeight)
    }

    private fun iconButton(icon: Icon, tooltip: String, onClick: () -> Unit): JLabel =
        JLabel(icon).apply {
            isOpaque = true
            background = Theme.panel
            toolTipText = tooltip
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = EmptyBorder(iconPad, iconPad, iconPad, iconPad)
            val side = iconSize + iconPad * 2
            preferredSize = Dimension(side, side)
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
            alignmentY = Component.CENTER_ALIGNMENT

            val hoverColor = Color(40, 40, 40)
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = hoverColor
                }

                override fun mouseExited(e: MouseEvent) {
                    background = Theme.panel
                }

                override fun mouseClicked(e: MouseEvent) {
                    onClick()
                }
            })
        }

    private fun loadIconScaled(path: String, w: Int, h: Int): Icon? {
        return try {
            val img = javaClass.getResourceAsStream(path)?.use { ImageIO.read(it) } ?: return null
            val scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH)
            ImageIcon(scaled)
        } catch (_: Exception) {
            null
        }
    }
}
