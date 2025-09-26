package ui

import java.awt.*
import javax.imageio.ImageIO
import javax.swing.*

class LoginFrame : JFrame("Wszystko sam muszę robić...") {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false

        // 1) Wczytaj obraz tła
        val bgImage = try {
            javaClass.getResourceAsStream("/background.jpg")?.use { ImageIO.read(it) }
        } catch (_: Exception) { null }
            ?: throw IllegalStateException("Brak pliku /background.jpg w resources")

        // 2) Panel z tłem w oryginalnym rozmiarze
        val bgPanel = object : JPanel() {
            override fun getPreferredSize(): Dimension = Dimension(bgImage.width, bgImage.height)
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                g.drawImage(bgImage, 0, 0, bgImage.width, bgImage.height, this)
            }
        }.apply {
            layout = GridBagLayout()
            isOpaque = true
            background = Color(0, 0, 0)
        }
        contentPane = bgPanel

        // 3) Kolumna na elementy logowania
        val column = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(520, 400)
        }

        // Belka powitalna
        val title = JLabel("Witaj w systemie WSMR!", SwingConstants.CENTER).apply {
            font = font.deriveFont(Font.BOLD, 22f)
            foreground = Color.WHITE
            isOpaque = true
            background = Color(0, 0, 0, 190)
            alignmentX = Component.CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(10, 20, 10, 20)
            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
        }
        column.add(title)
        column.add(Box.createVerticalStrut(12))

        // Pola
        val userField = JTextField()
        val passField = JPasswordField()

        column.add(makeStrip("Nazwa użytkownika", userField))
        column.add(Box.createVerticalStrut(8))
        column.add(makeStrip("Hasło", passField))
        column.add(Box.createVerticalStrut(12))

        // Przycisk
        val btnLogin = JButton("Zaloguj").apply {
            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
            alignmentX = Component.CENTER_ALIGNMENT
            isOpaque = true
            background = Color(0, 0, 0, 180)
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 14f)
            border = BorderFactory.createEmptyBorder()

            addActionListener {
                val u = userField.text.trim()
                val p = String(passField.password)
                if (u.isEmpty() || p.isEmpty()) {
                    JOptionPane.showMessageDialog(this@LoginFrame, "Uzupełnij dane.")
                } else {
                    dispose()
                    MainWindow().isVisible = true
                }
            }
        }
        rootPane.defaultButton = btnLogin
        column.add(btnLogin)

        // Dodaj kolumnę na środek
        bgPanel.add(column, GridBagConstraints().apply { anchor = GridBagConstraints.CENTER })

        pack()
        setLocationRelativeTo(null)
    }

    private fun makeStrip(label: String, field: JComponent): JComponent =
        JPanel(BorderLayout()).apply {
            isOpaque = true
            background = Color(0, 0, 0, 180)

            add(JLabel(label).apply {
                foreground = Color.WHITE
                border = BorderFactory.createEmptyBorder(4, 6, 2, 6)
            }, BorderLayout.NORTH)

            if (field is JTextField) {
                field.font = field.font.deriveFont(14f)
                field.isOpaque = false
                field.foreground = Color.WHITE
                field.caretColor = Color.WHITE
                field.border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            }
            if (field is JPasswordField) {
                field.isOpaque = false
                field.foreground = Color.WHITE
                field.caretColor = Color.WHITE
                field.border = BorderFactory.createEmptyBorder(5, 6, 5, 6)
            }

            add(field, BorderLayout.CENTER)

            preferredSize = Dimension(500, 60)
            maximumSize = Dimension(500, 60)
            alignmentX = Component.CENTER_ALIGNMENT
        }
}
