package ui

import db.UserDao
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class UsersPanel : JPanel(BorderLayout()) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // ===== MODEL =====
    private val model = object : DefaultTableModel(
        arrayOf("ID", "Login", "Rola", "Aktywny", "Utworzony", "Ostatnie logowanie"), 0
    ) {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            0 -> java.lang.Long::class.java
            3 -> java.lang.Boolean::class.java
            else -> String::class.java
        }
    }

    // ===== TABELA =====
    private val table = JTable(model).apply {
        autoCreateRowSorter = true
        rowHeight = 30
        gridColor = Theme.grid
        showVerticalLines = false
        showHorizontalLines = true
        intercellSpacing = Dimension(0, 1)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    // ===== WYSZUKIWARKA + PRZYCISKI (ikony) =====
    private val searchField = JTextField().apply {
        preferredSize = Dimension(260, 34)
        background = Theme.panel
        foreground = Theme.textPrimary
        caretColor = Theme.textPrimary
        border = EmptyBorder(8, 10, 8, 10)
        toolTipText = "Szukaj po loginie lub roli (Enter)"
    }

    private val searchBtn = iconButton("/search.png", "Szukaj") {
        refresh(searchField.text.trim())
    }

    private val addBtn = iconButton("/add.png", "Dodaj użytkownika") {
        // TODO: otwórz dialog „Dodaj użytkownika”
        JOptionPane.showMessageDialog(this@UsersPanel, "TODO: Dodaj użytkownika")
    }

    private val editBtn = iconButton("/edit.png", "Edytuj użytkownika") {
        val id = selectedUserId() ?: return@iconButton
        // TODO: otwórz dialog „Edytuj użytkownika”
        JOptionPane.showMessageDialog(this@UsersPanel, "TODO: Edytuj użytkownika ID=$id")
    }.apply { isEnabled = false }

    private val delBtn = iconButton("/delete.png", "Usuń użytkownika") {
        val id = selectedUserId() ?: return@iconButton
        // TODO: potwierdzenie i usunięcie
        JOptionPane.showMessageDialog(this@UsersPanel, "TODO: Usuń użytkownika ID=$id")
    }.apply { isEnabled = false }

    init {
        background = Theme.bg
        border = EmptyBorder(16, 16, 16, 16)

        // Styl ciemny tabeli + paskowanie
        Theme.styleTable(table)

        // Renderery kolumn
        val centerRenderer = DefaultTableCellRenderer().apply {
            horizontalAlignment = SwingConstants.CENTER
            foreground = Theme.textPrimary
            background = Theme.panel
        }
        val boolRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean,
                hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
                label.horizontalAlignment = SwingConstants.CENTER
                label.text = if (value == true) "✅" else "❌"
                label.foreground = Theme.textPrimary
                label.background = if (isSelected) Theme.selection else if (row % 2 == 0) Theme.rowEven else Theme.rowOdd
                return label
            }
        }
        val dateRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean,
                hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
                label.horizontalAlignment = SwingConstants.CENTER
                label.text = value?.toString() ?: "—"
                return label
            }
        }

        val cols = table.columnModel
        cols.getColumn(0).preferredWidth = 60
        cols.getColumn(1).preferredWidth = 220
        cols.getColumn(2).preferredWidth = 120
        cols.getColumn(3).preferredWidth = 90
        cols.getColumn(4).preferredWidth = 180
        cols.getColumn(5).preferredWidth = 200

        cols.getColumn(0).cellRenderer = centerRenderer
        cols.getColumn(3).cellRenderer = boolRenderer
        cols.getColumn(4).cellRenderer = dateRenderer
        cols.getColumn(5).cellRenderer = dateRenderer

        // Scroll
        val scroll = JScrollPane(table).apply {
            border = BorderFactory.createEmptyBorder()
            background = Theme.bg
            viewport.background = Theme.bg
        }

        // Górny toolbar
        val toolbar = JPanel(BorderLayout()).apply {
            background = Theme.panel
            border = EmptyBorder(12, 12, 12, 12)

            val left = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                isOpaque = false
                add(JLabel("Szukaj:").apply {
                    foreground = Theme.textSecondary
                    font = font.deriveFont(Font.PLAIN, 13f)
                })
                add(searchField)
                add(searchBtn)
            }
            val right = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
                isOpaque = false
                add(addBtn)
                add(editBtn)
                add(delBtn)
            }

            add(left, BorderLayout.WEST)
            add(right, BorderLayout.EAST)
        }

        add(toolbar, BorderLayout.NORTH)
        add(scroll, BorderLayout.CENTER)
        preferredSize = Dimension(1000, 560)

        // Zdarzenia
        hookEvents()

        // Pierwsze ładowanie
        refresh()
    }

    // ====== Helpery GUI ======

    private fun hookEvents() {
        // Enter w polu szukania
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) refresh(searchField.text.trim())
            }
        })
        // Live search (delikatne opóźnienie)
        val timer = Timer(220) { refresh(searchField.text.trim()) }
        timer.isRepeats = false
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = timer.restart()
            override fun removeUpdate(e: DocumentEvent) = timer.restart()
            override fun changedUpdate(e: DocumentEvent) = timer.restart()
        })

        // Aktywacja Edytuj/Usuń po zaznaczeniu
        table.selectionModel.addListSelectionListener {
            val hasSel = table.selectedRow >= 0
            editBtn.isEnabled = hasSel
            delBtn.isEnabled = hasSel
        }
    }

    private fun refresh(search: String? = null) {
        model.rowCount = 0
        val users = UserDao.getAll(search = search)
        users.forEach { u ->
            model.addRow(
                arrayOf(
                    u.id,
                    u.username,
                    u.role,
                    u.active,
                    u.createdAt.format(formatter),
                    u.lastLogin?.format(formatter) ?: "—"
                )
            )
        }
        if (table.rowCount > 0) table.setRowSelectionInterval(0, 0)
    }

    private fun selectedUserId(): Long? {
        val viewIdx = table.selectedRow
        if (viewIdx < 0) return null
        val modelIdx = table.convertRowIndexToModel(viewIdx)
        val idVal = model.getValueAt(modelIdx, 0)
        return (idVal as? Number)?.toLong()
    }

    /** Tworzy spójny graficzny przycisk z ikoną z /resources (PNG). */
    private fun iconButton(path: String, tooltip: String, onClick: () -> Unit): JButton {
        val icon = loadIcon(path, 28, 28)
        return if (icon != null) {
            JButton(icon)
        } else {
            JButton(tooltip) // awaryjnie, gdyby zasobu brakło
        }.apply {
            background = Theme.panel
            foreground = Theme.textPrimary
            border = EmptyBorder(6, 10, 6, 10)
            toolTipText = tooltip
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { onClick() }
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) { background = Color(40, 40, 40) }
                override fun mouseExited(e: MouseEvent) { background = Theme.panel }
            })
        }
    }

    private fun loadIcon(path: String, w: Int, h: Int): Icon? {
        return try {
            val stream = javaClass.getResourceAsStream(path) ?: return null
            val img = ImageIO.read(stream)
            val scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH)
            ImageIcon(scaled)
        } catch (_: Exception) {
            null
        }
    }
}
