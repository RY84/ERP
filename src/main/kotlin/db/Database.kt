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
            getConnection().use { conn ->
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
}
