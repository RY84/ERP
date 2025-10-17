package ui

import java.awt.*
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

        // ===== GÓRNY PASEK =====
        val topBar = TopBar(
            username = username,
            onHome = { showHome() },
            onUsers = { showUsers() },
            onLogout = { onLogout() }
        )

        centerHost.background = Theme.bg

        root.add(topBar, BorderLayout.NORTH)
        root.add(centerHost, BorderLayout.CENTER)

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

    /** Wylogowanie — wracamy do LoginFrame z prefillem aktualnego użytkownika (sesyjnie). */
    private fun onLogout() {
        dispose()
        LoginFrame(prefillUsername = username).isVisible = true
    }
}
