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
        // Konfiguracja z pliku config.properties (w katalogu roboczym)
        try {
            println("CONFIG: user.dir = ${System.getProperty("user.dir")}")
            val cfgFile = File("config.properties")
            println("CONFIG: looking for file = ${cfgFile.absolutePath}")
            if (!cfgFile.exists()) throw IllegalStateException("Brak pliku config.properties!")

            FileInputStream(cfgFile).use { props.load(it) }
            println("CONFIG: loaded host=${props["db.host"]}, port=${props["db.port"]}, db=${props["db.name"]}, user=${props["db.user"]}")
        } catch (ex: Exception) {
            throw IllegalStateException("❌ Nie mogę załadować config.properties: ${ex.message}")
        }

        // Szybki test połączenia (tylko log)
        try {
            getConnection().use {
                println("✅ Połączono z PostgreSQL @ ${props["db.host"]}:${props["db.port"]}/${props["db.name"]} jako ${props["db.user"]}")
            }
        } catch (ex: SQLException) {
            System.err.println("❌ Błąd połączenia z bazą: ${ex.message}")
        }
    }

    fun getConnection(): Connection {
        val url = "jdbc:postgresql://${props["db.host"]}:${props["db.port"]}/${props["db.name"]}"
        return DriverManager.getConnection(url, props["db.user"] as String, props["db.pass"] as String)
    }

    /**
     * Minimalna, trwała wersja:
     * - tworzy tabelę users (jeśli brak),
     * - zapewnia istnienie kolumn: password_hash, active, last_login,
     * - dba o unikalność username,
     * - seeduje admin/admin (BCrypt) gdy brak.
     */
    fun ensureSchemaAndSeed() {
        try {
            getConnection().use { conn ->
                conn.autoCommit = false
                try {
                    // 1) Tabela w docelowym kształcie (bez destrukcyjnych zmian)
                    conn.createStatement().use { st ->
                        st.execute(
                            """
                            CREATE TABLE IF NOT EXISTS users (
                                id             SERIAL PRIMARY KEY,
                                username       TEXT NOT NULL,
                                password_hash  TEXT NOT NULL,
                                role           TEXT NOT NULL DEFAULT 'user',
                                active         BOOLEAN NOT NULL DEFAULT TRUE,
                                created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                last_login     TIMESTAMP NULL
                            );
                            """.trimIndent()
                        )

                        // 2) Unikalność username (idempotentnie)
                        st.execute(
                            """
                            DO $$
                            BEGIN
                                IF NOT EXISTS (
                                    SELECT 1 FROM pg_indexes 
                                    WHERE schemaname='public' AND indexname='users_username_uq'
                                ) THEN
                                    CREATE UNIQUE INDEX users_username_uq ON users(username);
                                END IF;
                            END$$;
                            """.trimIndent()
                        )

                        // 3) Gwarancja brakujących kolumn (idempotentnie)
                        st.execute(
                            """
                            DO $$
                            BEGIN
                                IF NOT EXISTS (
                                    SELECT 1 FROM information_schema.columns
                                    WHERE table_schema='public' AND table_name='users' AND column_name='password_hash'
                                ) THEN
                                    ALTER TABLE users ADD COLUMN password_hash TEXT NOT NULL DEFAULT '';
                                END IF;

                                IF NOT EXISTS (
                                    SELECT 1 FROM information_schema.columns
                                    WHERE table_schema='public' AND table_name='users' AND column_name='active'
                                ) THEN
                                    ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
                                END IF;

                                IF NOT EXISTS (
                                    SELECT 1 FROM information_schema.columns
                                    WHERE table_schema='public' AND table_name='users' AND column_name='last_login'
                                ) THEN
                                    ALTER TABLE users ADD COLUMN last_login TIMESTAMP NULL;
                                END IF;
                            END$$;
                            """.trimIndent()
                        )
                    }

                    // 4) Seed admin/admin (BCrypt), jeśli nie istnieje
                    val adminExists = conn.prepareStatement(
                        "SELECT 1 FROM users WHERE username='admin' LIMIT 1"
                    ).use { pst -> pst.executeQuery().use { it.next() } }

                    if (!adminExists) {
                        val adminHash = BCrypt.withDefaults().hashToString(12, "admin".toCharArray())
                        conn.prepareStatement(
                            "INSERT INTO users(username, password_hash, role, active, created_at) VALUES ('admin', ?, 'admin', TRUE, CURRENT_TIMESTAMP)"
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
