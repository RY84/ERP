package ui

import db.UserDao
import java.awt.*
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class UsersPanel : JPanel(BorderLayout()) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

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

    private val table = JTable(model).apply {
        autoCreateRowSorter = true
        rowHeight = 30
        gridColor = Theme.grid
        showVerticalLines = false
        showHorizontalLines = true
        intercellSpacing = Dimension(0, 1)
    }

    init {
        background = Theme.bg
        border = EmptyBorder(16, 16, 16, 16)
        Theme.styleTable(table)

        val colModel = table.columnModel
        colModel.getColumn(0).preferredWidth = 50
        colModel.getColumn(1).preferredWidth = 180
        colModel.getColumn(2).preferredWidth = 120
        colModel.getColumn(3).preferredWidth = 90
        colModel.getColumn(4).preferredWidth = 180
        colModel.getColumn(5).preferredWidth = 200

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

        colModel.getColumn(0).cellRenderer = centerRenderer
        colModel.getColumn(3).cellRenderer = boolRenderer
        colModel.getColumn(4).cellRenderer = dateRenderer
        colModel.getColumn(5).cellRenderer = dateRenderer

        val scroll = JScrollPane(table).apply {
            border = BorderFactory.createEmptyBorder()
            background = Theme.bg
            viewport.background = Theme.bg
        }

        add(scroll, BorderLayout.CENTER)
        preferredSize = Dimension(950, 520)

        loadData()
    }

    private fun loadData() {
        model.rowCount = 0
        val users = UserDao.getAll()
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
    }
}
