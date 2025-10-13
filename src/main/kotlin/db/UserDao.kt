package db

import at.favre.lib.crypto.bcrypt.BCrypt
import java.sql.ResultSet

data class User(
    val id: Long,
    val username: String,
    val role: String,
    val createdAt: java.time.LocalDateTime
)

object UserDao {

    private fun map(rs: ResultSet): User =
        User(
            id = rs.getLong("id"),
            username = rs.getString("username"),
            role = rs.getString("role"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime()
        )

    // LISTA
    fun getAll(search: String? = null, limit: Int = 200, offset: Int = 0): List<User> {
        val sql = buildString {
            append("SELECT id, username, role, created_at FROM users ")
            if (!search.isNullOrBlank()) append("WHERE username ILIKE ? OR role ILIKE ? ")
            append("ORDER BY id ASC LIMIT ? OFFSET ?")
        }
        Database.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                var i = 1
                if (!search.isNullOrBlank()) {
                    st.setString(i++, "%$search%")
                    st.setString(i++, "%$search%")
                }
                st.setInt(i++, limit)
                st.setInt(i, offset)
                st.executeQuery().use { rs ->
                    val out = mutableListOf<User>()
                    while (rs.next()) out += map(rs)
                    return out
                }
            }
        }
    }

    fun count(search: String? = null): Int {
        val sql = if (search.isNullOrBlank())
            "SELECT COUNT(*) FROM users"
        else
            "SELECT COUNT(*) FROM users WHERE username ILIKE ? OR role ILIKE ?"
        Database.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                if (!search.isNullOrBlank()) {
                    st.setString(1, "%$search%"); st.setString(2, "%$search%")
                }
                st.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
            }
        }
    }

    fun existsUsername(username: String): Boolean {
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT 1 FROM users WHERE username = ?").use { st ->
                st.setString(1, username)
                st.executeQuery().use { rs -> return rs.next() }
            }
        }
    }

    // CREATE
    fun create(username: String, rawPassword: String, role: String): Long {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(rawPassword.isNotBlank()) { "Password cannot be blank" }
        if (existsUsername(username)) error("Użytkownik o takiej nazwie już istnieje")

        val hash = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray())
        val sql = """
            INSERT INTO users(username, password_hash, role, created_at)
            VALUES (?, ?, ?, now())
            RETURNING id
        """.trimIndent()
        Database.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setString(1, username)
                st.setString(2, hash)
                st.setString(3, role)
                st.executeQuery().use { rs -> rs.next(); return rs.getLong(1) }
            }
        }
    }

    // UPDATE
    fun update(id: Long, username: String, role: String, newRawPassword: String? = null) {
        require(id > 0) { "Invalid id" }
        require(username.isNotBlank()) { "Username cannot be blank" }

        val (sql, binder) =
            if (newRawPassword.isNullOrBlank()) {
                "UPDATE users SET username = ?, role = ? WHERE id = ?" to { st: java.sql.PreparedStatement ->
                    st.setString(1, username); st.setString(2, role); st.setLong(3, id)
                }
            } else {
                val hash = BCrypt.withDefaults().hashToString(12, newRawPassword.toCharArray())
                "UPDATE users SET username = ?, role = ?, password_hash = ? WHERE id = ?" to { st: java.sql.PreparedStatement ->
                    st.setString(1, username); st.setString(2, role); st.setString(3, hash); st.setLong(4, id)
                }
            }

        Database.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st -> binder(st); st.executeUpdate() }
        }
    }

    // DELETE
    fun delete(id: Long) {
        require(id > 0) { "Invalid id" }
        Database.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM users WHERE id = ?").use { st ->
                st.setLong(1, id); st.executeUpdate()
            }
        }
    }

    // RESET HASŁA
    fun resetPassword(id: Long, newRawPassword: String) {
        require(id > 0) { "Invalid id" }
        require(newRawPassword.isNotBlank()) { "Password cannot be blank" }
        val hash = BCrypt.withDefaults().hashToString(12, newRawPassword.toCharArray())
        Database.getConnection().use { conn ->
            conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?").use { st ->
                st.setString(1, hash); st.setLong(2, id); st.executeUpdate()
            }
        }
    }

    // AUTH
    fun authenticate(username: String, rawPassword: String): User? {
        val sql = """
            SELECT id, username, role, created_at, password_hash
            FROM users
            WHERE username = ?
            LIMIT 1
        """.trimIndent()

        Database.getConnection().use { conn ->
            conn.prepareStatement(sql).use { st ->
                st.setString(1, username)
                st.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    val hash = rs.getString("password_hash") ?: return null
                    val ok = try {
                        BCrypt.verifyer().verify(rawPassword.toCharArray(), hash.toCharArray()).verified
                    } catch (_: Exception) { false }
                    return if (ok) {
                        User(
                            id = rs.getLong("id"),
                            username = rs.getString("username"),
                            role = rs.getString("role"),
                            createdAt = rs.getTimestamp("created_at").toLocalDateTime()
                        )
                    } else null
                }
            }
        }
    }
}
