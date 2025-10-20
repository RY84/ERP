package ui

import java.awt.*
import javax.swing.*

class MainWindow(private val username: String) : JFrame("Wszystko sam muszę robić...") {

    private val centerHost = JPanel(BorderLayout())

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 700)
        setLocationRelativeTo(null)

        val root = JPanel(BorderLayout())
        contentPane = root

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

    private fun showHome() {
        centerHost.removeAll()
        centerHost.add(HomePanel(), BorderLayout.CENTER)
        centerHost.revalidate()
        centerHost.repaint()
    }

    private fun showUsers() {
        centerHost.removeAll()
        centerHost.add(UsersPanel(), BorderLayout.CENTER)
        centerHost.revalidate()
        centerHost.repaint()
    }

    /** Wylogowanie — wracamy do LoginFrame bez preloadera. */
    private fun onLogout() {
        dispose()
        LoginFrame(prefillUsername = username, showPreloader = false).isVisible = true
    }
}
