package ui

import db.User
import db.UserDao
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableModel

class UsersPanel : JPanel(BorderLayout()) {

    private val model = object : DefaultTableModel(
        arrayOf("ID", "Login", "Rola", "Utworzony"), 0
    ) {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            0 -> java.lang.Long::class.java
            else -> String::class.java
        }
    }

    private val table = JTable(model).apply {
        rowHeight = 28
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        autoCreateRowSorter = true
    }

    init {
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        add(JScrollPane(table), BorderLayout.CENTER)
        preferredSize = Dimension(800, 500)
        loadData()
    }

    private fun loadData() {
        model.rowCount = 0
        val users: List<User> = UserDao.findAll()
        users.forEach { u ->
            model.addRow(arrayOf(u.id, u.username, u.role, u.createdAt))
        }
    }
}
