package ui

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer

/**
 * Jeden, spójny theme dla całej aplikacji (dark).
 * Używamy go przez:
 *   1) Theme.applyGlobalUI() – globalne kolory/typografia
 *   2) Theme.styleTable(table) – ciemne tabele z paskami
 *   3) Theme.panel(padding) – gotowy panel o tle Theme.bg
 *   4) Theme.overlayPanel(alpha) – półprzezroczysty panel (np. Login)
 *   5) Theme.labelTitle(...) / labelSubtitle(...) – presety nagłówków
 */
object Theme {

    // ===== Paleta =====
    val bg            = Color(0x14, 0x14, 0x14)            // #141414
    val panel         = Color(0x18, 0x18, 0x18)            // #181818
    val panelOverlay  = Color(0, 0, 0, 180)                 // półprzezroczyste
    val textPrimary   = Color(0xEB, 0xEB, 0xEB)            // #EBEBEB
    val textSecondary = Color(0xB4, 0xB4, 0xB4)            // #B4B4B4
    val grid          = Color(0x30, 0x30, 0x30)            // #303030
    val accent        = Color(0, 0xCD, 0)                  // #00CD00
    val selection     = Color(0x22, 0x4A, 0x24)            // #224A24 (ciemna zieleń)
    val rowEven       = Color(0x18, 0x18, 0x18)            // #181818
    val rowOdd        = Color(0x20, 0x20, 0x20)            // #202020
    val headerBg      = Color(0x1E, 0x1E, 0x1E)            // #1E1E1E

    // Typografia
    val fontBase: Font = Font("SansSerif", Font.PLAIN, 14)
    val fontMono: Font = Font("Monospaced", Font.PLAIN, 13)

    /**
     * Ustawienia globalne UIManager – wywołaj raz po ustawieniu Look&Feel,
     * zanim utworzysz pierwsze komponenty (np. w main() tuż przed LoginFrame()).
     */
    fun applyGlobalUI() {
        // Panele/okna
        UIManager.put("Panel.background", bg)
        UIManager.put("OptionPane.background", panel)
        UIManager.put("OptionPane.messageForeground", textPrimary)
        UIManager.put("OptionPane.foreground", textPrimary)

        // Etykiety / ToolTip
        UIManager.put("Label.foreground", textPrimary)
        UIManager.put("ToolTip.background", Color(35, 35, 35))
        UIManager.put("ToolTip.foreground", textPrimary)
        UIManager.put("ToolTip.font", fontBase.deriveFont(13f))

        // Przyciski
        UIManager.put("Button.background", panel)
        UIManager.put("Button.foreground", textPrimary)
        UIManager.put("Button.font", fontBase)
        UIManager.put("Button.select", selection)

        // Pola tekstowe
        UIManager.put("TextField.background", panel)
        UIManager.put("TextField.foreground", textPrimary)
        UIManager.put("TextField.caretForeground", textPrimary)
        UIManager.put("TextField.selectionBackground", selection)
        UIManager.put("TextField.selectionForeground", textPrimary)
        UIManager.put("PasswordField.background", panel)
        UIManager.put("PasswordField.foreground", textPrimary)
        UIManager.put("PasswordField.caretForeground", textPrimary)
        UIManager.put("PasswordField.selectionBackground", selection)
        UIManager.put("PasswordField.selectionForeground", textPrimary)

        // ScrollPane / ScrollBar (częściowo zależne od LAF)
        UIManager.put("ScrollPane.background", bg)
        UIManager.put("Viewport.background", bg)

        // Tabele (część globalnie, reszta w styleTable)
        UIManager.put("Table.background", panel)
        UIManager.put("Table.foreground", textPrimary)
        UIManager.put("Table.gridColor", grid)
        UIManager.put("Table.selectionBackground", selection)
        UIManager.put("Table.selectionForeground", textPrimary)
        UIManager.put("Table.font", fontBase)

        // Nagłówek tabeli
        UIManager.put("TableHeader.background", headerBg)
        UIManager.put("TableHeader.foreground", textPrimary)
        UIManager.put("TableHeader.font", fontBase.deriveFont(Font.BOLD, 13f))
    }

    /**
     * Styl dla JTable – naprawia kolory, ustawia „zebrowanie” wierszy,
     * nagłówek, rozmiar wiersza, wypełnianie viewportu.
     */
    fun styleTable(table: JTable) {
        table.background = panel
        table.foreground = textPrimary
        table.gridColor = grid
        table.selectionBackground = selection
        table.selectionForeground = textPrimary
        table.font = fontBase
        table.rowHeight = 28
        table.setShowGrid(true)
        table.fillsViewportHeight = true

        // Nagłówek
        table.tableHeader.background = headerBg
        table.tableHeader.foreground = textPrimary
        table.tableHeader.font = fontBase.deriveFont(Font.BOLD, 13f)
        table.tableHeader.reorderingAllowed = true

        // Renderer paskowany (zachowuje się poprawnie przy zaznaczaniu)
        val striped = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                tbl: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column)
                if (!isSelected) {
                    background = if (row % 2 == 0) rowEven else rowOdd
                    foreground = textPrimary
                } else {
                    background = selection
                    foreground = textPrimary
                }
                if (c is JComponent) {
                    c.border = null
                }
                return c
            }
        }
        table.setDefaultRenderer(Any::class.java, striped)
    }

    /** Panel o tle Theme.bg i domyślnym paddingu. */
    fun panel(padding: Int = 12): JPanel =
        JPanel().apply {
            background = bg
            border = EmptyBorder(padding, padding, padding, padding)
        }

    /** Półprzezroczysty panel (np. w LoginFrame dla wierszy formularza). */
    fun overlayPanel(alpha: Int = 180, padding: Int = 10): JPanel =
        object : JPanel() {
            override fun isOpaque() = false
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.color = Color(0, 0, 0, alpha.coerceIn(0, 255))
                g2.fillRect(0, 0, width, height)
                g2.dispose()
                super.paintComponent(g)
            }
        }.apply {
            layout = BorderLayout()
            border = EmptyBorder(padding, padding, padding, padding)
            foreground = textPrimary
        }

    /** Nagłówek tytułowy. */
    fun labelTitle(text: String): JLabel =
        JLabel(text).apply {
            foreground = textPrimary
            font = fontBase.deriveFont(Font.BOLD, 28f)
        }

    /** Podtytuł / opis. */
    fun labelSubtitle(text: String): JLabel =
        JLabel(text).apply {
            foreground = textSecondary
            font = fontBase.deriveFont(16f)
        }
}
