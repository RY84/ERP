package ui

import db.UserDao
import java.awt.*
import java.awt.event.KeyEvent
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.KeyStroke

class LoginFrame : JFrame("Wszystko sam muszę robić...") {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false

        // === 1) Wczytanie obrazu tła ===
        val bgImage = try {
            javaClass.getResourceAsStream("/background.jpg")?.use { ImageIO.read(it) }
                ?: throw IllegalStateException("Brak pliku background.jpg w resources")
        } catch (e: Exception) {
            throw RuntimeException("Nie udało się wczytać tła: ${e.message}", e)
        }

        // ustawiamy rozmiar okna dokładnie jak obraz
        val imgWidth = bgImage.getWidth(null)
        val imgHeight = bgImage.getHeight(null)
        setSize(imgWidth, imgHeight)
        setLocationRelativeTo(null)

        // === 2) Panel z tłem 1:1 ===
        val bgPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                // rysujemy tło dokładnie w oryginalnych wymiarach
                g.drawImage(bgImage, 0, 0, imgWidth, imgHeight, this)
            }
        }.apply {
            layout = GridBagLayout() // do wyśrodkowania formularza
            isOpaque = true
        }
        contentPane = bgPanel

        // === 3) Kolumna centralna ===
        val column = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(520, 420)
        }

        // === 4) Nagłówek ===
        val title = JLabel("Witaj w systemie WSMR!", SwingConstants.CENTER).apply {
            font = font.deriveFont(Font.BOLD, 22f)
            foreground = Color.WHITE
            isOpaque = true
            background = Color(0, 0, 0, 190)
            alignmentX = Component.CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(12, 20, 12, 20)
            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
        }

        // === 5) Pola logowania ===
        val userField = JTextField().apply {
            isOpaque = false
            foreground = Color.WHITE
            caretColor = Color.WHITE
            border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            font = font.deriveFont(14f)
        }

        val passField = JPasswordField().apply {
            isOpaque = false
            foreground = Color.WHITE
            caretColor = Color.WHITE
            border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            font = font.deriveFont(14f)
        }

        // === 6) Rząd z etykietą i polem ===
        fun row(label: String, field: JComponent) = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = Color(0, 0, 0, 180)
            border = BorderFactory.createEmptyBorder(10, 12, 10, 12)

            val lbl = JLabel(label).apply {
                foreground = Color.WHITE
                font = font.deriveFont(Font.BOLD, 14f)
                border = BorderFactory.createEmptyBorder(0, 0, 0, 10)
                preferredSize = Dimension(170, 24)
            }
            add(lbl, BorderLayout.WEST)
            add(field, BorderLayout.CENTER)

            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
            alignmentX = Component.CENTER_ALIGNMENT
        }

        val rowUser = row("Nazwa użytkownika", userField)
        val rowPass = row("Hasło", passField)

        // === 7) Przyciski ===
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 12, 0)).apply {
            isOpaque = false
            maximumSize = Dimension(500, 48)
        }

        val loginBtn = JButton("Zaloguj").apply {
            isOpaque = true
            background = Color(0, 0, 0, 180)
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 14f)
            border = BorderFactory.createEmptyBorder(8, 16, 8, 16)
        }

        val exitBtn = JButton("Wyjdź").apply {
            isOpaque = true
            background = Color(0, 0, 0, 180)
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 14f)
            border = BorderFactory.createEmptyBorder(8, 16, 8, 16)
            addActionListener { dispose() }
        }

        // === 8) Logika logowania ===
        fun doLogin() {
            val u = userField.text.trim()
            val p = String(passField.password)

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this@LoginFrame, "Uzupełnij dane.")
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
            }
        }

        loginBtn.addActionListener { doLogin() }
        rootPane.defaultButton = loginBtn

        buttonPanel.add(loginBtn)
        buttonPanel.add(exitBtn)

        // === 9) Składanie wszystkiego ===
        column.add(title)
        column.add(Box.createVerticalStrut(16))
        column.add(rowUser)
        column.add(Box.createVerticalStrut(12))
        column.add(rowPass)
        column.add(Box.createVerticalStrut(16))
        column.add(buttonPanel)

        val c = GridBagConstraints().apply {
            gridx = 0; gridy = 0
            anchor = GridBagConstraints.CENTER
        }
        bgPanel.add(column, c)

        // === 10) ESC = zamknij ===
        val im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val am = rootPane.actionMap
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close")
        am.put("close", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                dispose()
            }
        })
    }
}
