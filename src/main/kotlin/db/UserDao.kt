package db

import java.sql.ResultSet

data class User(
    val id: Long,
    val username: String,
    val role: String,
    val createdAt: String
)

object UserDao {

    /**
     * Logowanie: sprawdza username + password_hash (tu: proste porównanie).
     * Jeśli w bazie trzymasz prawdziwe hashe (np. bcrypt/scrypt),
     * zamień warunek w SQL lub zweryfikuj w Javie (komentarz niżej).
     */
    fun authenticate(username: String, password: String): User? {
        val sql = """
            SELECT id, username, role, created_at
            FROM users
            WHERE username = ? AND password_hash = ?
            LIMIT 1
        """.trimIndent()

        Database.getConnection().use { con ->
            con.prepareStatement(sql).use { ps ->
                ps.setString(1, username)
                ps.setString(2, password) // <- jeśli trzymasz hashe, podaj tu hash z password
                ps.executeQuery().use { rs ->
                    return if (rs.next()) rs.toUser() else null
                }
            }
        }

        /**
         * Jeśli chcesz weryfikować np. bcrypt:
         * 1) pobierz rekord po username (bez warunku na hasło),
         * 2) sprawdź BCrypt.verifyer().verify(password.toCharArray(), password_hash_from_db),
         * 3) zwróć User przy zgodności.
         */
    }

    /** Lista użytkowników do widoku. */
    fun findAll(): List<User> {
        val sql = """
            SELECT id, username, role, created_at
            FROM users
            ORDER BY username ASC
        """.trimIndent()

        val out = mutableListOf<User>()
        Database.getConnection().use { con ->
            con.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    while (rs.next()) out += rs.toUser()
                }
            }
        }
        return out
    }

    // --- mapper ---
    private fun ResultSet.toUser(): User =
        User(
            id = getLong("id"),
            username = getString("username"),
            role = getString("role") ?: "user",
            createdAt = getString("created_at")
        )
}
