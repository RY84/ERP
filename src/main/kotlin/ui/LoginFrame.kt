package ui

import db.UserDao
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.KeyStroke

class LoginFrame(private val prefillUsername: String? = null) : JFrame("Wszystko sam muszę robić...") {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false

        // === 1) Tło (obraz) ===
        val bgImage = try {
            javaClass.getResourceAsStream("/background.jpg")?.use { ImageIO.read(it) }
                ?: throw IllegalStateException("Brak pliku background.jpg w resources")
        } catch (e: Exception) {
            throw RuntimeException("Nie udało się wczytać tła: ${e.message}", e)
        }

        val imgWidth = bgImage.getWidth(null)
        val imgHeight = bgImage.getHeight(null)
        setSize(imgWidth, imgHeight)
        setLocationRelativeTo(null)

        val bgPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                g.drawImage(bgImage, 0, 0, imgWidth, imgHeight, this)
            }
        }.apply {
            layout = GridBagLayout()
            isOpaque = true
        }
        contentPane = bgPanel

        // === 2) Kolumna centralna ===
        val column = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(520, 420)
        }

        // === 3) Nagłówek (półprzezroczysty panel + tytuł z Theme) ===
        val titleWrap = Theme.overlayPanel(alpha = 190, padding = 12).apply {
            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
            add(
                Theme.labelTitle("Witaj w systemie WSMR!").apply {
                    font = font.deriveFont(Font.BOLD, 22f)
                    horizontalAlignment = SwingConstants.CENTER
                },
                BorderLayout.CENTER
            )
        }

        // === 4) Pola logowania ===
        val userField = JTextField().apply {
            isOpaque = false
            foreground = Theme.textPrimary
            caretColor = Theme.textPrimary
            border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            font = font.deriveFont(14f)
        }
        val passField = JPasswordField().apply {
            isOpaque = false
            foreground = Theme.textPrimary
            caretColor = Theme.textPrimary
            border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            font = font.deriveFont(14f)
        }

        // Prefill TYLKO jeśli wracamy z wylogowania (sesyjnie)
        if (!prefillUsername.isNullOrBlank()) {
            userField.text = prefillUsername
            passField.requestFocusInWindow()
            passField.selectAll()
        } else {
            userField.requestFocusInWindow()
        }

        // Zaznacz całe hasło przy wejściu fokusa (TAB/klik)
        passField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                SwingUtilities.invokeLater { passField.selectAll() }
            }
        })
        // (opcjonalnie) zaznacz cały login przy wejściu fokusa, jeśli niepusty
        userField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                if (userField.text.isNotEmpty()) {
                    SwingUtilities.invokeLater { userField.selectAll() }
                }
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

        val rowUser = row("Nazwa użytkownika", userField)
        val rowPass = row("Hasło", passField)

        // === 5) Przyciski ===
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

        val buttonPanel = Theme.overlayPanel(alpha = 180, padding = 8).apply {
            preferredSize = Dimension(500, 48)
            maximumSize = Dimension(500, 48)
            val inner = JPanel(FlowLayout(FlowLayout.CENTER, 12, 0)).apply {
                isOpaque = false
                add(loginBtn)
                add(exitBtn)
            }
            add(inner, BorderLayout.CENTER)
        }

        // === 6) Logika logowania ===
        fun doLogin() {
            val u = userField.text.trim()
            val p = String(passField.password)
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this@LoginFrame, "Uzupełnij dane.")
                // wygoda: po błędzie od razu zaznacz hasło
                passField.requestFocusInWindow()
                passField.selectAll()
                return
            }
            val user = try {
                UserDao.authenticate(u, p)
            } catch (ex: Exception) {
                JOptionPane.showMessageDialog(
                    this@LoginFrame,
                    "Błąd połączenia z bazą: ${ex.message}",
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE
                )
                null
            }
            if (user != null) {
                dispose()
                MainWindow(user.username).isVisible = true
            } else {
                JOptionPane.showMessageDialog(
                    this@LoginFrame,
                    "Nieprawidłowa nazwa użytkownika lub hasło.",
                    "Logowanie nieudane",
                    JOptionPane.WARNING_MESSAGE
                )
                // po nieudanym logowaniu — zaznacz hasło, by nadpisać bez kasowania
                passField.requestFocusInWindow()
                passField.selectAll()
            }
        }
        loginBtn.addActionListener { doLogin() }
        rootPane.defaultButton = loginBtn

        // === 7) Złożenie widoku ===
        column.add(titleWrap)
        column.add(Box.createVerticalStrut(16))
        column.add(rowUser)
        column.add(Box.createVerticalStrut(12))
        column.add(rowPass)
        column.add(Box.createVerticalStrut(16))
        column.add(buttonPanel)

        val c = GridBagConstraints().apply { gridx = 0; gridy = 0; anchor = GridBagConstraints.CENTER }
        bgPanel.add(column, c)

        // === 8) ESC = zamknij ===
        val im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val am = rootPane.actionMap
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close")
        am.put("close", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) = dispose()
        })
    }
}
