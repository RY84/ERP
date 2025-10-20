package updater

import utils.Paths
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Rozpakowuje ZIP do katalogu aplikacji.
 * onPhase: komunikaty etapów
 * onProgressFiles: (extractedCount, totalCount)
 */
object Install {

    fun installFrom(
        zipPath: Path,
        onPhase: (String) -> Unit = {},
        onProgressFiles: (done: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val targetDir = Paths.appDir.toPath()
        Files.createDirectories(targetDir)

        onPhase("Przygotowanie instalacji…")

        // 1) policz wpisy (szybki przebieg)
        val total = countEntries(zipPath)
        onPhase("Plików do rozpakowania: $total")

        // 2) właściwy ekstrakt z progressem
        var done = 0
        ZipInputStream(BufferedInputStream(Files.newInputStream(zipPath))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val e = entry
                if (!e.isDirectory) {
                    val fileName = e.name.substringAfterLast('/')

                    val out = targetDir.resolve(fileName)
                    // upewnij się że katalog istnieje
                    Files.createDirectories(out.parent)

                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING)
                    done++
                    onProgressFiles(done, total)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        onPhase("Instalacja zakończona")
    }

    private fun countEntries(zip: Path): Int {
        var total = 0
        ZipInputStream(BufferedInputStream(Files.newInputStream(zip))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) total++
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return total
    }
}
