package updater

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Sprawdza wersję z app-version.json i porównuje z wersją lokalną.
 * Źródło URL:
 *  1) config.properties → key: update.json.url
 *  2) fallback: RAW GitHub (domyślny)
 *
 * Na błędach sieci zwraca ciszę (krótki, pojedynczy log) i NIE przerywa aplikacji.
 */
object UpdateCheck {

    private const val DEFAULT_URL =
        "https://raw.githubusercontent.com/RY84/ERP/main/app-version.json"

    data class Meta(
        val latest: String,
        val minRequired: String,
        val downloadUrl: String?,
        val sha256: String?
    )

    fun run(localVersion: String) {
        val url = readUpdateUrlFromConfig() ?: DEFAULT_URL
        val meta = fetchMeta(url)
        if (meta == null) {
            println("ℹ️ UpdateCheck: brak metadanych (offline/URL niedostępny) — pomijam sprawdzanie.")
            return
        }

        println("🆚 Porównanie wersji: lokalna=$localVersion, latest=${meta.latest}, minRequired=${meta.minRequired}")

        when {
            isNewer(meta.minRequired, localVersion) -> {
                println("⛔ Wersja lokalna < wymagane minimum → należałoby wykonać AUTO-UPDATE przed logowaniem.")
                println("   (download_url=${meta.downloadUrl}, sha256=${meta.sha256})")
            }
            isNewer(meta.latest, localVersion) -> {
                println("🔔 Dostępna nowsza wersja → należałoby wykonać AUTO-UPDATE przed logowaniem.")
                println("   (download_url=${meta.downloadUrl}, sha256=${meta.sha256})")
            }
            else -> {
                println("✅ Klient jest aktualny — można przejść do logowania.")
            }
        }
    }

    private fun fetchMeta(updateJsonUrl: String): Meta? {
        return try {
            val tmp = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve("wsmr-app-version.json")
            val conn = (URL(updateJsonUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            val code = conn.responseCode
            if (code != 200) {
                // cicho i kulturalnie:
                System.err.println("ℹ️ UpdateCheck: serwer zwrócił HTTP $code — pomijam.")
                return null
            }

            conn.inputStream.use { input ->
                Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING)
            }
            val txt = Files.readString(tmp)

            fun findString(key: String): String? {
                val re = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
                return re.find(txt)?.groupValues?.get(1)
            }

            val latest = findString("latest_client_version") ?: return null
            val minReq = findString("min_required_client_version") ?: latest
            val url = findString("download_url")
            val sha = findString("sha256")
            Meta(latest, minReq, url, sha)
        } catch (_: Exception) {
            // bez stack trace – po prostu pomijamy
            null
        }
    }

    /** true jeśli a > b, np. 1.2.10 > 1.2.9 */
    private fun isNewer(a: String, b: String): Boolean {
        val aa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val bb = b.split('.').map { it.toIntOrNull() ?: 0 }
        val n = maxOf(aa.size, bb.size)
        for (i in 0 until n) {
            val x = aa.getOrElse(i) { 0 }
            val y = bb.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** próba odczytu `update.json.url` z config.properties obok pliku wykonywalnego */
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
