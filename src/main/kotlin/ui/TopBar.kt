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

    // Pixel-perfect proporcje
    private val barHeight = 56          // wysokość paska
    private val iconSize = 48           // rozmiar grafiki
    private val iconPad  = 3            // padding wokół grafiki (góra/dół/lewo/prawo)
    private val accentGreen = Color(0, 205, 0)

    init {
        isOpaque = true
        background = Color(25, 25, 25)
        // 56 - (48 + 3 + 3) = 2 -> po 1 px na górze i dole
        border = EmptyBorder(1, 12, 1, 12)

        // Lewa/prawa część — smukłe odstępy, centrowanie pionowe
        val left = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply { isOpaque = false; alignmentY = Component.CENTER_ALIGNMENT }
        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0)).apply { isOpaque = false; alignmentY = Component.CENTER_ALIGNMENT }

        // --- HOME (po lewej) ---
        loadIconScaled("/home.png", iconSize, iconSize)?.let {
            left.add(iconButton(it, "Strona główna") { onHome() })
        }

        // --- Prawa strona: login (zielony), Użytkownicy, Wyloguj ---
        right.add(JLabel(username).apply {
            foreground = accentGreen
            font = font.deriveFont(Font.BOLD, 16f)
            isOpaque = false
            toolTipText = "Zalogowano jako $username"
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
            preferredSize = Dimension(preferredSize.width, iconSize + iconPad * 2) // 54 px -> równa wysokość jak ikony
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

    /** Ikonowy „guzik” (bez hovera), idealnie centrowany, 54×54 px. */
    private fun iconButton(icon: Icon, tooltip: String, onClick: () -> Unit): JLabel =
        JLabel(icon).apply {
            isOpaque = false
            toolTipText = tooltip
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = EmptyBorder(iconPad, iconPad, iconPad, iconPad)
            val side = iconSize + iconPad * 2 // 54 px
            preferredSize = Dimension(side, side)
            minimumSize = preferredSize
            maximumSize = preferredSize
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
            alignmentY = Component.CENTER_ALIGNMENT
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { onClick() }
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
