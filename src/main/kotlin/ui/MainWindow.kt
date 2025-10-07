package ui

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*

class MainWindow(private val username: String) : JFrame("Wszystko sam muszę robić...") {

    // kontener na środek, żeby łatwo podmieniać widoki
    private val centerHost = JPanel(BorderLayout())

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 700)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        // ===== Pasek =====
        val topBar = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = Color(15, 15, 15)
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color(35, 35, 35))
            preferredSize = Dimension(0, 64)
        }

        val lblTitle = JLabel(
            "<html><span style='color:#00C800;'>Zalogowano do systemu jako:</span> " +
                    "<span style='color:#00FF00; text-decoration:underline;'>$username</span></html>"
        ).apply {
            font = font.deriveFont(Font.BOLD, 13f)
            horizontalAlignment = SwingConstants.LEFT
            verticalAlignment = SwingConstants.TOP
            border = BorderFactory.createEmptyBorder(6, 8, 0, 0)
        }

        val leftWrapper = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(lblTitle, BorderLayout.NORTH)
        }
        topBar.add(leftWrapper, BorderLayout.WEST)

        val rightBox = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0)).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
        }

        val userIcon = iconLabel("/user.png", "Użytkownicy", 44) {
            showUsers()
        }
        val powerIcon = iconLabel("/logout.png", "Wyloguj", 44) {
            dispose()
            LoginFrame().isVisible = true
        }

        rightBox.add(userIcon)
        rightBox.add(powerIcon)
        topBar.add(rightBox, BorderLayout.EAST)
        add(topBar, BorderLayout.NORTH)

        // ===== Środek (domyślny ekran powitalny) =====
        centerHost.add(JLabel("Witaj w systemie WSMR!", SwingConstants.CENTER), BorderLayout.CENTER)
        add(centerHost, BorderLayout.CENTER)
    }

    private fun showUsers() {
        centerHost.removeAll()
        centerHost.add(UsersPanel(), BorderLayout.CENTER)
        centerHost.revalidate()
        centerHost.repaint()
    }

    private fun iconLabel(iconPath: String, tooltip: String, size: Int, onClick: () -> Unit): JComponent {
        val icon = loadIconScaled(iconPath, size - 8, size - 8)
        val label = JLabel(icon).apply {
            this.toolTipText = tooltip
            isOpaque = true
            background = Color(15, 15, 15)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
            preferredSize = Dimension(size, size)
            minimumSize = Dimension(size, size)
            maximumSize = Dimension(size, size)
            border = BorderFactory.createEmptyBorder()
        }
        val hoverBg = Color(28, 28, 28)
        val normalBg = label.background
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { label.background = hoverBg }
            override fun mouseExited(e: MouseEvent) { label.background = normalBg }
            override fun mouseClicked(e: MouseEvent) { onClick() }
        })
        return label
    }

    private fun loadIconScaled(path: String, w: Int, h: Int): Icon? {
        return try {
            val img = javaClass.getResourceAsStream(path)?.use { ImageIO.read(it) } ?: return null
            val scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH)
            ImageIcon(scaled)
        } catch (_: Exception) { null }
    }
}
