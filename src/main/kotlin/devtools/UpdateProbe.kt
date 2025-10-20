package updater

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Prosty „probe”: pobiera app-version.json, zapisuje w katalogu TMP i wypisuje treść.
 * Jeżeli URL nieosiągalny – ciche pominięcie (krótki log).
 * Źródło URL:
 *  1) config.properties → key: update.json.url
 *  2) fallback: RAW GitHub (domyślny)
 */
object UpdateProbe {

    private const val DEFAULT_URL =
        "https://raw.githubusercontent.com/RY84/ERP/main/app-version.json"

    fun run() {
        val url = readUpdateUrlFromConfig() ?: DEFAULT_URL
        try {
            val tmp = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve("wsmr-app-version.json")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            val code = conn.responseCode
            if (code != 200) {
                System.err.println("ℹ️ UpdateProbe: HTTP $code — pomijam probe.")
                return
            }
            conn.inputStream.use { input ->
                Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING)
            }
            val text = Files.readString(tmp)
            println("⬇️  UpdateProbe: zapisano app-version.json (${text.length} B) → ${tmp.toAbsolutePath()}")
        } catch (_: Exception) {
            // ciche pominięcie; jeden krótki log wystarczy
            System.err.println("ℹ️ UpdateProbe: brak dostępu do URL — pomijam.")
        }
    }

    private fun readUpdateUrlFromConfig(): String? {
        return try {
            val cfg = File("config.properties")
            if (!cfg.exists()) return null
            val props = java.util.Properties().apply { cfg.inputStream().use { load(it) } }
            props.getProperty("update.json.url")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
