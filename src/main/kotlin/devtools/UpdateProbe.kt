package updater

import utils.Paths
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Prosty „probe”: pobiera app-version.json z GitHuba, zapisuje w katalogu TMP
 * i wypisuje treść w konsoli. Na razie brak logiki aktualizacji.
 */
object UpdateProbe {
    // Surowy link (RAW) do Twojego pliku z wersją
    private const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/RY84/ERP/main/app-version.json"

    /** Pobiera app-version.json do katalogu tymczasowego i wypisuje jego treść. */
    fun run() {
        try {
            val tmpFile = Paths.tmpDir.resolve("app-version.json").toPath()
            val conn = URL(UPDATE_JSON_URL).openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.getInputStream().use { input ->
                Files.createDirectories(tmpFile.parent)
                Files.copy(input, tmpFile, StandardCopyOption.REPLACE_EXISTING)
            }
            val text = Files.readString(tmpFile)
            println("⬇️  Zapisano app-version.json: ${tmpFile.toAbsolutePath()}")
            println(text)
        } catch (e: Exception) {
            System.err.println("⚠️ Nie udało się pobrać app-version.json: ${e.message}")
        }
    }
}
