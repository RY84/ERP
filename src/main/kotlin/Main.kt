package erp

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.math.min

fun main() {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {}

        // Wczytaj obraz tła z resources (src/main/resources/background.jpg)
        val imgUrl = Thread.currentThread().contextClassLoader.getResource("background.jpg")
        val bgImage: BufferedImage? = try {
            imgUrl?.let { ImageIO.read(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        // Oblicz docelowy rozmiar okna bazując na obrazie (nie powiększamy ponad ekran)
        val screen = Toolkit.getDefaultToolkit().screenSize
        val (targetW, targetH) = if (bgImage != null) {
            val imgW = bgImage.width.toDouble()
            val imgH = bgImage.height.toDouble()
            val maxW = screen.width * 0.90
            val maxH = screen.height * 0.90
            val scale = min(1.0, min(maxW / imgW, maxH / imgH))
            Pair((imgW * scale).toInt(), (imgH * scale).toInt())
        } else {
            Pair(900, 600)
        }

        // Tytuł okna (tekst na belce)
        val frame = JFrame("Wszystko sam muszę robić...")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isResizable = false

        // Panel tła
        val backgroundPanel = object : JPanel() {
            init {
                isOpaque = true
                layout = BorderLayout()
                preferredSize = Dimension(targetW, targetH)
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2 = g as Graphics2D
                if (bgImage != null) {
                    val imgW = bgImage.width
                    val imgH = bgImage.height
                    val scale = min(1.0, min(width.toDouble() / imgW.toDouble(), height.toDouble() / imgH.toDouble()))
                    val drawW = (imgW * scale).toInt()
                    val drawH = (imgH * scale).toInt()
                    val x = (width - drawW) / 2
                    val y = (height - drawH) / 2
                    val scaled = bgImage.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH)
                    g2.drawImage(scaled, x, y, null)
                } else {
                    g2.color = Color(45, 62, 80)
                    g2.fillRect(0, 0, width, height)
                }

                // overlay (lekko przyciemnia całe tło dla czytelności)
                g2.color = Color(0, 0, 0, 90)
                g2.fillRect(0, 0, width, height)
            }
        }

        // Helper: stwórz holder (półprzezroczyste tło + obramowanie)
        fun createHolder(content: JComponent, paddingTopBottom: Int = 8, paddingLeftRight: Int = 12): JPanel {
            val holder = JPanel(BorderLayout()).apply {
                isOpaque = true
                background = Color(0, 0, 0, 150)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(255, 255, 255, 80)),
                    EmptyBorder(paddingTopBottom, paddingLeftRight, paddingTopBottom, paddingLeftRight)
                )
            }
            val container = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(content, BorderLayout.CENTER)
            }
            holder.add(container, BorderLayout.CENTER)
            return holder
        }

        // --- Tytuł w holderze (WYŚRODKOWANY) ---
        val titleInner = JLabel("WSMR", SwingConstants.CENTER).apply {
            font = Font(font.name, Font.BOLD, 26)
            foreground = Color.WHITE
            isOpaque = false
        }
        val titleHolder = createHolder(titleInner, paddingTopBottom = 6, paddingLeftRight = 20)
        val titlePanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
            isOpaque = false
            border = EmptyBorder(10, 0, 0, 0)
            add(titleHolder)
        }
        backgroundPanel.add(titlePanel, BorderLayout.NORTH)

        // --- Formularz (środek) WYŚRODKOWANY ---
        val formPanel = JPanel(GridBagLayout()).apply {
            isOpaque = false
            border = EmptyBorder(20, 40, 20, 40)
        }

        val gbc = GridBagConstraints().apply {
            gridx = 0; gridy = 0
            insets = Insets(8, 8, 8, 8)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.CENTER
            weightx = 1.0
        }

        fun createFieldHolder(labelText: String, field: JComponent): JPanel {
            val holder = JPanel(BorderLayout()).apply {
                isOpaque = true
                background = Color(0, 0, 0, 150)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color(255, 255, 255, 80)),
                    EmptyBorder(6, 8, 6, 8)
                )
            }
            val lbl = JLabel(labelText).apply {
                foreground = Color(200, 200, 200)
                font = Font(font.name, Font.PLAIN, 12)
                border = EmptyBorder(0, 0, 4, 0)
            }
            val container = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(lbl, BorderLayout.NORTH)
                add(field, BorderLayout.CENTER)
            }
            holder.add(container, BorderLayout.CENTER)
            return holder
        }

        val usernameField = JTextField().apply {
            columns = 22
            isOpaque = false
            foreground = Color.WHITE
            caretColor = Color.WHITE
            font = Font(font.name, Font.PLAIN, 14)
            border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
            background = Color(0, 0, 0, 0)
        }

        val passwordField = JPasswordField().apply {
            columns = 22
            isOpaque = false
            foreground = Color.WHITE
            caretColor = Color.WHITE
            font = Font(font.name, Font.PLAIN, 14)
            border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
            background = Color(0, 0, 0, 0)
        }

        val loginButton = JButton("Zaloguj").apply {
            isOpaque = false
            isContentAreaFilled = false
            foreground = Color.WHITE
            font = Font(font.name, Font.BOLD, 16)
            isFocusPainted = false
            border = BorderFactory.createEmptyBorder(10, 20, 10, 20)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        // Dodaj pola i przycisk
        formPanel.add(createFieldHolder("Nazwa użytkownika", usernameField), gbc)
        gbc.gridy = 1
        formPanel.add(createFieldHolder("Hasło", passwordField), gbc)
        gbc.gridy = 2
        formPanel.add(createHolder(loginButton, paddingTopBottom = 10, paddingLeftRight = 12), gbc)

        val centerWrapper = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(formPanel, BorderLayout.CENTER)
        }
        backgroundPanel.add(centerWrapper, BorderLayout.CENTER)

        // Akcja logowania (demo)
        fun doLogin() {
            val user = usernameField.text.trim()
            val pass = String(passwordField.password)
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Wpisz nazwę użytkownika i hasło", "Błąd", JOptionPane.WARNING_MESSAGE)
                return
            }
            JOptionPane.showMessageDialog(frame, "Zalogowano jako: $user", "Sukces", JOptionPane.INFORMATION_MESSAGE)
        }

        loginButton.addActionListener { doLogin() }

        // Ustawiamy default button - Enter będzie aktywował loginButton
        frame.rootPane.defaultButton = loginButton

        // Dodatkowo: zabezpieczające mapowanie w trybie WHEN_IN_FOCUSED_WINDOW
        val enterKey = "ENTER_LOGIN"
        frame.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), enterKey)
        frame.rootPane.actionMap.put(enterKey, object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) { doLogin() }
        })

        // Finalne przypisanie panelu i pokazanie okna
        frame.contentPane = backgroundPanel
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}
