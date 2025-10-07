package db

import java.io.FileInputStream
import java.util.Properties
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object Database {
    private val props = Properties()

    init {
        try {
            FileInputStream("config.properties").use { fis ->
                props.load(fis)
            }
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

    /** UTWÓRZ SCHEMAT + SEED: users + konto admin/admin (jeśli brak) */
    fun ensureSchemaAndSeed() {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { st ->
                    st.execute(
                        """
                        CREATE TABLE IF NOT EXISTS users (
                            id          SERIAL PRIMARY KEY,
                            username    TEXT NOT NULL UNIQUE,
                            password    TEXT NOT NULL,
                            role        TEXT NOT NULL DEFAULT 'user',
                            active      BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        );
                        """.trimIndent()
                    )

                    val rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin';")
                    rs.next()
                    val adminExists = rs.getInt(1) > 0

                    if (!adminExists) {
                        st.executeUpdate(
                            """
                            INSERT INTO users (username, password, role, active)
                            VALUES ('admin', 'admin', 'admin', TRUE);
                            """.trimIndent()
                        )
                        println("👤 Dodano domyślnego użytkownika: admin / admin")
                    } else {
                        println("ℹ️ Konto admin już istnieje — pomijam seed.")
                    }
                }
            }
        } catch (ex: Exception) {
            System.err.println("❌ ensureSchemaAndSeed() błąd: ${ex.message}")
        }
    }
}
