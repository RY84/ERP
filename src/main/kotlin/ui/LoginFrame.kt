package ui

import db.UserDao
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.KeyStroke
import javax.swing.border.EmptyBorder

class LoginFrame(
    private val prefillUsername: String? = null,
    private val showPreloader: Boolean = true
) : JFrame("Wszystko sam muszę robić...") {

    private val host = CrossfadePanel()

    // 🔹 trzymamy referencję do pola loginu, żeby pewnie ustawić fokus
    private var loginUserField: JTextField? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false

        // TŁO
        val bgImage = javaClass.getResourceAsStream("/background.jpg")?.use { ImageIO.read(it) }
            ?: error("Brak pliku background.jpg w resources")

        setSize(bgImage.getWidth(null), bgImage.getHeight(null))
        setLocationRelativeTo(null)

        val bgPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                g.drawImage(bgImage, 0, 0, width, height, this)
            }
        }.apply {
            layout = GridBagLayout()
            isOpaque = true
        }
        contentPane = bgPanel

        val centerWrap = JPanel(GridBagLayout()).apply {
            isOpaque = false
            add(host)
        }
        host.preferredSize = Dimension(520, 420)
        host.isOpaque = false

        val c = GridBagConstraints().apply { gridx = 0; gridy = 0; anchor = GridBagConstraints.CENTER }
        bgPanel.add(centerWrap, c)

        // ➜ Gdy start z main(): preloader; po wylogowaniu: od razu login
        if (showPreloader) {
            host.showInitial(preloaderPanel())
        } else {
            host.showInitial(loginPanel())
            // spróbuj ustawić fokus po zbudowaniu loginu
            requestFocusOnLoginField()
        }

        // ESC = zamknij
        val im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val am = rootPane.actionMap
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "close")
        am.put("close", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) = dispose()
        })
    }

    /** Ekran „Przygotowanie…” z KnightRiderBar. */
    private fun preloaderPanel(): JComponent {
        val column = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(520, 420)
        }

        val titleWrap = Theme.overlayPanel(alpha = 190, padding = 12).apply {
            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
            add(Theme.labelTitle("Przygotowanie...").apply {
                font = font.deriveFont(Font.BOLD, 22f)
                horizontalAlignment = SwingConstants.CENTER
            }, BorderLayout.CENTER)
        }

        val details = JTextArea().apply {
            isOpaque = false
            foreground = Theme.textSecondary
            font = font.deriveFont(12f)
            border = EmptyBorder(0, 2, 0, 2)
            text = "Sprawdzam wersję i środowisko..."
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }

        val sub = Theme.overlayPanel(alpha = 180, padding = 12).apply {
            preferredSize = Dimension(500, 140)
            maximumSize = Dimension(500, 140)

            val box = Box.createVerticalBox()
            val bar = KnightRiderBar(barWidth = 460, barHeight = 16, speedPxPerFrame = 10)
            box.add(bar)
            box.add(Box.createVerticalStrut(10))
            box.add(details)
            add(box, BorderLayout.CENTER)
        }

        column.add(titleWrap)
        column.add(Box.createVerticalStrut(16))
        column.add(sub)
        return column
    }

    /** Ekran logowania. */
    private fun loginPanel(): JComponent {
        val column = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(520, 420)
        }

        val titleWrap = Theme.overlayPanel(alpha = 190, padding = 12).apply {
            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
            add(Theme.labelTitle("Witaj w systemie WSMR!").apply {
                font = font.deriveFont(Font.BOLD, 22f)
                horizontalAlignment = SwingConstants.CENTER
            }, BorderLayout.CENTER)
        }

        val userField = JTextField().apply {
            isOpaque = false
            foreground = Theme.textPrimary
            caretColor = Theme.textPrimary
            border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            font = font.deriveFont(14f)
        }
        // zapamiętaj referencję do ustawiania fokusu
        loginUserField = userField

        val passField = JPasswordField().apply {
            isOpaque = false
            foreground = Theme.textPrimary
            caretColor = Theme.textPrimary
            border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            font = font.deriveFont(14f)
        }

        // Prefill tylko treść – fokus i tak ustawiamy programowo na login
        prefillUsername?.let { userField.text = it }

        passField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) { SwingUtilities.invokeLater { passField.selectAll() } }
        })
        userField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                if (userField.text.isNotEmpty()) SwingUtilities.invokeLater { userField.selectAll() }
            }
        })

        fun row(label: String, field: JComponent) = Theme.overlayPanel(alpha = 180, padding = 10).apply {
            val lbl = JLabel(label).apply {
                foreground = Theme.textPrimary
                font = font.deriveFont(Font.BOLD, 14f)
                border = BorderFactory.createEmptyBorder(0, 0, 0, 10)
                preferredSize = Dimension(170, 24)
            }
            add(lbl, BorderLayout.WEST)
            add(field, BorderLayout.CENTER)
            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
        }

        val loginBtn = JButton("Zaloguj").apply {
            font = font.deriveFont(Font.BOLD, 14f)
            background = Theme.panel
            foreground = Theme.textPrimary
            border = BorderFactory.createEmptyBorder(8, 16, 8, 16)
        }
        val exitBtn = JButton("Wyjdź").apply {
            font = font.deriveFont(Font.BOLD, 14f)
            background = Theme.panel
            foreground = Theme.textPrimary
            border = BorderFactory.createEmptyBorder(8, 16, 8, 16)
            addActionListener { dispose() }
        }
        val buttons = Theme.overlayPanel(alpha = 180, padding = 8).apply {
            preferredSize = Dimension(500, 48)
            maximumSize = Dimension(500, 48)
            val inner = JPanel(FlowLayout(FlowLayout.CENTER, 12, 0)).apply {
                isOpaque = false
                add(loginBtn); add(exitBtn)
            }
            add(inner, BorderLayout.CENTER)
        }

        fun doLogin() {
            val u = userField.text.trim()
            val p = String(passField.password)
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this@LoginFrame, "Uzupełnij dane.")
                passField.requestFocusInWindow(); passField.selectAll()
                return
            }
            val user = try { UserDao.authenticate(u, p) } catch (ex: Exception) {
                JOptionPane.showMessageDialog(this@LoginFrame, "Błąd połączenia z bazą: ${ex.message}", "Błąd", JOptionPane.ERROR_MESSAGE); null
            }
            if (user != null) { dispose(); MainWindow(user.username).isVisible = true }
            else {
                JOptionPane.showMessageDialog(this@LoginFrame, "Nieprawidłowa nazwa użytkownika lub hasło.", "Logowanie nieudane", JOptionPane.WARNING_MESSAGE)
                passField.requestFocusInWindow(); passField.selectAll()
            }
        }

        // ENTER działa + defaultButton
        passField.addActionListener { doLogin() }
        userField.addActionListener { passField.requestFocusInWindow() }
        loginBtn.addActionListener { doLogin() }
        rootPane.defaultButton = loginBtn

        column.add(titleWrap)
        column.add(Box.createVerticalStrut(16))
        column.add(row("Nazwa użytkownika", userField))
        column.add(Box.createVerticalStrut(12))
        column.add(row("Hasło", passField))
        column.add(Box.createVerticalStrut(16))
        column.add(buttons)

        return column
    }

    /** Płynne przejście do logowania; po animacji wymuszamy fokus na polu loginu. */
    fun showLogin() {
        host.crossfadeTo(loginPanel(), durationMs = 420, fps = 60)
        // po crossfade – kilka prób ustawienia fokusu (UI potrafi „połknąć” pierwsze żądanie)
        requestFocusOnLoginField(delayMs = 80)
    }

    /** Ustawia fokus na polu loginu z kilkoma próbami (asekuracyjnie). */
    private fun requestFocusOnLoginField(delayMs: Int = 0) {
        val maxTries = 15
        var tries = 0
        val t = javax.swing.Timer(60) {
            tries++
            if (!isVisible) return@Timer
            this@LoginFrame.toFront()
            this@LoginFrame.requestFocus()
            rootPane.requestFocusInWindow()
            val ok = loginUserField?.requestFocusInWindow() ?: false
            if (ok || tries >= maxTries) (it.source as javax.swing.Timer).stop()
        }
        t.isRepeats = true
        if (delayMs > 0) {
            javax.swing.Timer(delayMs) { t.start() }.apply { isRepeats = false; start() }
        } else {
            t.start()
        }
    }
}
