package updater

import utils.Paths
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Sprawdza wersję z app-version.json i porównuje z wersją lokalną.
 * Na tym etapie TYLKO wypisuje decyzję (bez pobierania ZIPa).
 */
object UpdateCheck {

    private const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/RY84/ERP/main/app-version.json"

    data class Meta(
        val latest: String,
        val minRequired: String,
        val downloadUrl: String?,
        val sha256: String?
    )

    fun run(localVersion: String) {
        val meta = fetchMeta() ?: run {
            println("ℹ️ UpdateCheck: pomijam (brak metadanych).")
            return
        }

        println("🆚 Porównanie wersji: lokalna=$localVersion, latest=${meta.latest}, minRequired=${meta.minRequired}")

        when {
            isNewer(meta.minRequired, localVersion) -> {
                println("⛔ Wersja lokalna jest niższa niż wymagane minimum → należałoby wykonać AUTO-UPDATE przed logowaniem.")
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

    private fun fetchMeta(): Meta? {
        return try {
            val tmp = Paths.tmpDir.resolve("app-version.json").toPath()
            val conn = URL(UPDATE_JSON_URL).openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.getInputStream().use { input ->
                Files.createDirectories(tmp.parent)
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
        } catch (e: Exception) {
            System.err.println("⚠️ UpdateCheck.fetchMeta błąd: ${e.message}")
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
}
