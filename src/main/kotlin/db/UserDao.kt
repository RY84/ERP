package db

import at.favre.lib.crypto.bcrypt.BCrypt
import java.sql.ResultSet

data class User(
    val id: Long,
    val username: String,
    val role: String
)

object UserDao {

    /**
     * Tworzy schemat (tabela users) i – jeśli brak użytkowników – seeduje konto admin/admin.
     * Wywołaj to raz na starcie aplikacji.
     */
    fun ensureSchemaAndSeed() {
        Database.getConnection().use { conn ->
            conn.createStatement().use { st ->
                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS users(
                        id SERIAL PRIMARY KEY,
                        username TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'user',
                        created_at TIMESTAMP NOT NULL DEFAULT NOW()
                    );
                    """.trimIndent()
                )
            }

            // jeśli brak rekordu 'admin' – dodaj
            if (findByUsername("admin") == null) {
                createUser("admin", "admin", "admin")
            }
        }
    }

    /**
     * Próbuje zalogować użytkownika. Zwraca User przy sukcesie, null przy błędzie.
     */
    fun authenticate(username: String, rawPassword: String): User? {
        val rec = findWithHash(username) ?: return null
        val verified = BCrypt.verifyer().verify(
            rawPassword.toCharArray(),
            rec.passwordHash
        ).verified
        return if (verified) User(rec.id, rec.username, rec.role) else null
    }

    /**
     * Tworzy użytkownika z hasłem (BCrypt).
     */
    fun createUser(username: String, rawPassword: String, role: String = "user"): User {
        val cleanUser = username.trim()
        require(cleanUser.isNotEmpty()) { "Username cannot be empty." }
        require(rawPassword.isNotEmpty()) { "Password cannot be empty." }

        val hash = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray())
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO users(username, password_hash, role)
                VALUES (?, ?, ?)
                RETURNING id, username, role
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, cleanUser)
                ps.setString(2, hash)
                ps.setString(3, role)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return User(
                        id = rs.getLong("id"),
                        username = rs.getString("username"),
                        role = rs.getString("role")
                    )
                }
            }
        }
    }

    /**
     * Zmienia hasło użytkownika (nadpisuje BCryptem).
     */
    fun changePassword(username: String, newRawPassword: String) {
        val hash = BCrypt.withDefaults().hashToString(12, newRawPassword.toCharArray())
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "UPDATE users SET password_hash = ? WHERE username = ?"
            ).use { ps ->
                ps.setString(1, hash)
                ps.setString(2, username)
                ps.executeUpdate()
            }
        }
    }

    /**
     * Szuka użytkownika (bez hasha).
     */
    fun findByUsername(username: String): User? {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT id, username, role FROM users WHERE username = ?"
            ).use { ps ->
                ps.setString(1, username)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) rsUser(rs) else null
                }
            }
        }
    }

    // --- prywatne / pomocnicze ---

    private data class UserWithHash(
        val id: Long,
        val username: String,
        val role: String,
        val passwordHash: String
    )

    private fun findWithHash(username: String): UserWithHash? {
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT id, username, role, password_hash FROM users WHERE username = ?"
            ).use { ps ->
                ps.setString(1, username)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) {
                        UserWithHash(
                            id = rs.getLong("id"),
                            username = rs.getString("username"),
                            role = rs.getString("role"),
                            passwordHash = rs.getString("password_hash")
                        )
                    } else null
                }
            }
        }
    }

    private fun rsUser(rs: ResultSet) = User(
        id = rs.getLong("id"),
        username = rs.getString("username"),
        role = rs.getString("role")
    )
}
