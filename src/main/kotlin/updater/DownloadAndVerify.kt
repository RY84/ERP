package updater

import utils.Paths
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Pobiera meta (app-version.json), ściąga ZIP, liczy SHA256 i porównuje.
 * Zwraca ścieżkę do poprawnie zweryfikowanego pliku ZIP (albo null przy błędzie).
 */
object DownloadAndVerify {

    private const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/RY84/ERP/main/app-version.json"

    data class Meta(
        val latest: String,
        val minRequired: String,
        val downloadUrl: String,
        val sha256: String
    )

    /** Główna procedura: pobierz ZIP i zweryfikuj SHA256. Zwraca Path do ZIP-a albo null. */
    fun run(): Path? {
        val meta = fetchMeta() ?: run {
            println("⚠️  Brak metadanych aktualizacji – przerywam.")
            return null
        }

        println("⬇️  Przygotowanie do pobrania ZIP:")
        println("    latest=${meta.latest}")
        println("    url=${meta.downloadUrl}")
        println("    sha256(meta)=${meta.sha256}")

        val zipPath = Paths.tmpDir.resolve("client-${meta.latest}.zip").toPath()
        return try {
            // Pobierz ZIP do katalogu tymczasowego
            URL(meta.downloadUrl).openConnection().apply {
                connectTimeout = 10_000
                readTimeout = 60_000
            }.getInputStream().use { input ->
                Files.createDirectories(zipPath.parent)
                Files.copy(input, zipPath, StandardCopyOption.REPLACE_EXISTING)
            }
            println("✅ ZIP pobrany: $zipPath (${Files.size(zipPath)} B)")

            // Policz SHA256 lokalnie
            val localSha = sha256Of(Files.newInputStream(zipPath))
            println("🔎 SHA256(local)=$localSha")

            // Porównanie
            if (localSha.equals(meta.sha256, ignoreCase = true)) {
                println("🟢 Weryfikacja SHA256 OK — paczka jest autentyczna i nienaruszona.")
                zipPath
            } else {
                println("🔴 BŁĄD weryfikacji SHA256! Oczekiwano ${meta.sha256}, ale otrzymano $localSha")
                null
            }
        } catch (e: Exception) {
            System.err.println("❌ Błąd pobierania/weryfikacji ZIP: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // --- helpers ---

    private fun fetchMeta(): Meta? {
        return try {
            val tmp = Paths.tmpDir.resolve("app-version.json").toPath()
            URL(UPDATE_JSON_URL).openConnection().apply {
                connectTimeout = 5_000
                readTimeout = 5_000
            }.getInputStream().use { input ->
                Files.createDirectories(tmp.parent)
                Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING)
            }
            val txt = Files.readString(tmp)

            fun find(key: String): String? {
                val re = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
                return re.find(txt)?.groupValues?.get(1)
            }

            val latest = find("latest_client_version") ?: return null
            val minReq = find("min_required_client_version") ?: latest
            val url = find("download_url") ?: return null
            val sha = find("sha256") ?: return null

            Meta(latest, minReq, url, sha)
        } catch (e: Exception) {
            System.err.println("⚠️  fetchMeta błąd: ${e.message}")
            null
        }
    }

    private fun sha256Of(input: InputStream): String {
        input.use { ins ->
            val md = MessageDigest.getInstance("SHA-256")
            val buf = ByteArray(8192)
            while (true) {
                val r = ins.read(buf)
                if (r <= 0) break
                md.update(buf, 0, r)
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
