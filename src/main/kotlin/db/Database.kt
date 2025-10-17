package db

import at.favre.lib.crypto.bcrypt.BCrypt
import java.io.File
import java.io.FileInputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

object Database {
    private val props = Properties()

    init {
        try {
            // 🔸 DIAGNOSTYKA KONFIGU
            println("CONFIG: user.dir = " + System.getProperty("user.dir"))
            val cfgFile = File("config.properties")
            println("CONFIG: looking for file = " + cfgFile.absolutePath)
            if (!cfgFile.exists()) {
                throw IllegalStateException("Plik config.properties nie istnieje w tej ścieżce!")
            }
            FileInputStream(cfgFile).use { props.load(it) }
            println(
                "CONFIG: loaded host=${props["db.host"]}, port=${props["db.port"]}, " +
                        "db=${props["db.name"]}, user=${props["db.user"]}"
            )
        } catch (ex: Exception) {
            throw IllegalStateException("❌ Nie mogę załadować pliku config.properties: ${ex.message}")
        }

        try {
            getConnection().use { _ ->
                println(
                    "✅ Połączono z PostgreSQL @ ${props["db.host"]}:${props["db.port"]}/${props["db.name"]} " +
                            "jako ${props["db.user"]}"
                )
            }
        } catch (ex: SQLException) {
            System.err.println("❌ Błąd połączenia z bazą: ${ex.message}")
        }
    }

    fun getConnection(): Connection {
        val url = "jdbc:postgresql://${props["db.host"]}:${props["db.port"]}/${props["db.name"]}"
        val user = props["db.user"] as String
        val pass = props["db.pass"] as String
        return DriverManager.getConnection(url, user, pass)
    }

    /** Tworzy schemat i seeduje admina; wykonuje też migrację password -> password_hash z haszowaniem. */
    fun ensureSchemaAndSeed() {
        try {
            getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    // 1) Tabela users w docelowym kształcie
                    conn.createStatement().use { st ->
                        st.execute(
                            """
                            CREATE TABLE IF NOT EXISTS users (
                                id          SERIAL PRIMARY KEY,
                                username    TEXT NOT NULL,
                                password_hash TEXT NOT NULL,
                                role        TEXT NOT NULL DEFAULT 'user',
                                active      BOOLEAN NOT NULL DEFAULT TRUE,
                                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                            );
                            """.trimIndent()
                        )
                        // Unikalność username
                        st.execute(
                            """
                            DO $$
                            BEGIN
                                IF NOT EXISTS (
                                    SELECT 1 FROM pg_indexes 
                                    WHERE schemaname = 'public' AND indexname = 'users_username_uq'
                                ) THEN
                                    CREATE UNIQUE INDEX users_username_uq ON users(username);
                                END IF;
                            END$$;
                            """.trimIndent()
                        )
                    }

                    // 2) MIGRACJA: jeśli istnieje kolumna 'password' (stare instalacje), to ją przenosimy do password_hash (z BCrypt)
                    val hasLegacy = conn.prepareStatement(
                        """
                        SELECT 1 
                        FROM information_schema.columns 
                        WHERE table_schema='public' AND table_name='users' AND column_name='password'
                        """.trimIndent()
                    ).use { pst -> pst.executeQuery().use { it.next() } }

                    val hasHash = conn.prepareStatement(
                        """
                        SELECT 1 
                        FROM information_schema.columns 
                        WHERE table_schema='public' AND table_name='users' AND column_name='password_hash'
                        """.trimIndent()
                    ).use { pst -> pst.executeQuery().use { it.next() } }

                    if (hasLegacy && hasHash) {
                        // a) pobierz stare hasła w plaintext
                        val rows = mutableListOf<Pair<Long, String>>()
                        conn.prepareStatement("SELECT id, password FROM users WHERE password IS NOT NULL").use { pst ->
                            pst.executeQuery().use { rs ->
                                while (rs.next()) {
                                    rows += rs.getLong("id") to rs.getString("password")
                                }
                            }
                        }
                        // b) przepisz do password_hash jako BCrypt
                        conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?").use { up ->
                            for ((id, plain) in rows) {
                                val hash = BCrypt.withDefaults().hashToString(12, plain.toCharArray())
                                up.setString(1, hash)
                                up.setLong(2, id)
                                up.addBatch()
                            }
                            up.executeBatch()
                        }
                        // c) usuń starą kolumnę
                        conn.createStatement().use { st -> st.execute("ALTER TABLE users DROP COLUMN password") }
                        println("🔄 Migracja: password → password_hash (BCrypt) zakończona.")
                    } else if (hasLegacy && !hasHash) {
                        // awaryjnie – gdyby ktoś usunął docelową kolumnę
                        conn.createStatement().use { st -> st.execute("ALTER TABLE users ADD COLUMN password_hash TEXT NOT NULL DEFAULT ''") }
                        println("ℹ️ Dodano brakującą kolumnę password_hash – uruchom ponownie, aby dokończyć migrację.")
                    }

                    // 3) SEED: admin/admin z BCrypt (jeśli nie istnieje)
                    val adminExists = conn.prepareStatement(
                        "SELECT 1 FROM users WHERE username = 'admin' LIMIT 1"
                    ).use { pst -> pst.executeQuery().use { it.next() } }

                    if (!adminExists) {
                        val adminHash = BCrypt.withDefaults().hashToString(12, "admin".toCharArray())
                        conn.prepareStatement(
                            """
                            INSERT INTO users(username, password_hash, role, active, created_at)
                            VALUES ('admin', ?, 'admin', TRUE, CURRENT_TIMESTAMP)
                            """.trimIndent()
                        ).use { pst ->
                            pst.setString(1, adminHash)
                            pst.executeUpdate()
                        }
                        println("👤 Dodano domyślnego użytkownika: admin / admin (BCrypt).")
                    } else {
                        println("ℹ️ Konto admin już istnieje — pomijam seed.")
                    }

                    conn.commit()
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                } finally {
                    conn.autoCommit = true
                }
            }
        } catch (ex: Exception) {
            System.err.println("❌ ensureSchemaAndSeed() błąd: ${ex.message}")
            ex.printStackTrace()
        }
    }
}
