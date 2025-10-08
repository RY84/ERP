package ui

import ui.TopBar
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder

class MainWindow(private val username: String) : JFrame("Wszystko sam muszę robić...") {

    // kontener na środek – tutaj podmieniamy widoki (HomePanel, UsersPanel, ...)
    private val centerHost = JPanel(BorderLayout())

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 700)
        setLocationRelativeTo(null)

        val root = JPanel(BorderLayout())
        contentPane = root

        // ===== GÓRNY PASEK (nowy komponent) =====
        val topBar = TopBar(
            username = username,
            onHome = { showHome() },
            onUsers = { showUsers() },
            onLogout = { onLogout() }
        )

        // ===== ŚRODEK =====
        centerHost.background = Color(20, 20, 20)

        // ===== GŁÓWNY LAYOUT =====
        root.add(topBar, BorderLayout.NORTH)
        root.add(centerHost, BorderLayout.CENTER)

        // Start od HomePanel
        showHome()

        pack()
        isVisible = true
    }

    /** Wstawia HomePanel do części centralnej. */
    private fun showHome() {
        centerHost.removeAll()
        centerHost.add(HomePanel(), BorderLayout.CENTER)
        centerHost.revalidate()
        centerHost.repaint()
    }

    /** Lista użytkowników. */
    private fun showUsers() {
        centerHost.removeAll()
        centerHost.add(UsersPanel(), BorderLayout.CENTER)
        centerHost.revalidate()
        centerHost.repaint()
    }

    private fun onLogout() {
        dispose()
        LoginFrame().isVisible = true
    }

    // (opcjonalnie — jeśli gdzieś używasz jeszcze ikon-etykiet w środku)
    private fun iconLabel(iconPath: String, tooltip: String, onClick: () -> Unit): JLabel? {
        val icon = loadIconScaled(iconPath, 22, 22) ?: return null
        val label = JLabel(icon).apply {
            isOpaque = true
            background = Color(0, 0, 0, 0)
            border = EmptyBorder(6, 6, 6, 6)
            toolTipText = tooltip
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
        val normalBg = Color(0, 0, 0, 0)
        val hoverBg = Color(50, 50, 50, 90)
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
