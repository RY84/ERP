package updater

import utils.Paths
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

/**
 * Pobiera paczkę ZIP i weryfikuje SHA256. Zwraca ścieżkę do ZIP albo null gdy błąd.
 * onPhase: komunikaty etapów
 * onProgress: (downloadedBytes, totalBytes lub -1 gdy nieznany)
 */
object DownloadAndVerify {

    // Prosty parser meta (tak jak wcześniej używamy regexów)
    private fun readJsonText(path: Path): String = java.nio.file.Files.readString(path)

    private fun field(json: String, key: String): String? {
        val re = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        return re.find(json)?.groupValues?.get(1)
    }

    private fun humanMB(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }

    fun run(
        onPhase: (String) -> Unit = {},
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Path? {
        return try {
            val metaPath = Paths.tmpDir.resolve("app-version.json").toPath()
            val metaText = readJsonText(metaPath)
            val url = field(metaText, "download_url") ?: return null
            val sha256Expected = field(metaText, "sha256")?.lowercase() ?: return null

            // Docelowa nazwa w TMP (stała), nadpisujemy
            val outZip = Paths.tmpDir.resolve("client-latest.zip").toPath()

            onPhase("Pobieranie: łączenie z serwerem…")
            val u = URL(url)
            val conn = (u.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                requestMethod = "GET"
            }
            val total = conn.contentLengthLong.let { if (it > 0) it else -1L }

            onPhase(
                if (total > 0) "Pobieranie: ${humanMB(total)} do pobrania"
                else "Pobieranie: rozmiar nieznany"
            )

            conn.inputStream.use { input ->
                BufferedInputStream(input).use { bis ->
                    FileOutputStream(outZip.toFile()).use { fos ->
                        val buf = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastUi = System.nanoTime()

                        while (true) {
                            val r = bis.read(buf)
                            if (r == -1) break
                            fos.write(buf, 0, r)
                            downloaded += r

                            // throttling aktualizacji UI (~ co 120 ms)
                            val now = System.nanoTime()
                            if ((now - lastUi) >= 120_000_000) {
                                onProgress(downloaded, total)
                                lastUi = now
                            }
                        }
                        fos.flush()
                        // finalny „tick”
                        onProgress(downloaded, total)
                    }
                }
            }

            onPhase("Weryfikacja SHA256…")
            val sha = sha256Of(outZip)
            if (sha != sha256Expected) {
                onPhase("BŁĄD weryfikacji SHA256")
                System.err.println("🔴 BŁĄD weryfikacji SHA256! oczekiwano $sha256Expected, otrzymano $sha")
                return null
            }

            onPhase("Weryfikacja OK")
            outZip
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sha256Of(path: Path): String {
        val md = MessageDigest.getInstance("SHA-256")
        java.nio.file.Files.newInputStream(path).use { fis ->
            val buf = ByteArray(128 * 1024)
            while (true) {
                val r = fis.read(buf)
                if (r == -1) break
                md.update(buf, 0, r)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
