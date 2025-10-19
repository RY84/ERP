package updater

import utils.Paths
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Prosty instalator paczki ZIP:
 * - rozpakowuje ZIP do katalogu aplikacji (Paths.appDir)
 * - spłaszcza ścieżki w ZIP (ignoruje katalogi typu `build/libs/`) i zapisuje same pliki
 * - zwraca ścieżkę do potencjalnego pliku JAR (jeśli znajdzie)
 *
 * Uwaga: ten etap NIE restartuje aplikacji i nie ubija działającego procesu.
 * To zrobimy w następnym kroku.
 */
object Install {

    /**
     * Rozpakowuje paczkę ZIP do katalogu aplikacji. Zwraca ścieżkę do „głównego” JAR-a
     * jeśli udało się ją zidentyfikować, inaczej null.
     */
    fun installFrom(zipPath: Path): Path? {
        require(Files.exists(zipPath)) { "Brak pliku ZIP: $zipPath" }

        // Docelowy katalog aplikacji
        val appDir = Paths.appDir.toPath()
        Files.createDirectories(appDir)

        // Katalog tymczasowy na rozpakowane pliki (żeby podmiana była bardziej atomowa)
        val tmpExtractDir = Paths.tmpDir.resolve("extract").toPath()
        if (Files.exists(tmpExtractDir)) {
            tmpExtractDir.toFile().deleteRecursively()
        }
        Files.createDirectories(tmpExtractDir)

        println("📦 Instalacja paczki do: ${appDir.toAbsolutePath()}")
        println("   ZIP: $zipPath")

        val extractedFiles = mutableListOf<Path>()
        ZipInputStream(BufferedInputStream(Files.newInputStream(zipPath))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // Bierzemy tylko nazwę pliku (ostatni segment), ignorujemy strukturę katalogów w ZIP
                    val baseName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (baseName.isNotBlank()) {
                        val outPath = tmpExtractDir.resolve(baseName)
                        // Upewnij się, że katalog istnieje
                        Files.createDirectories(outPath.parent)
                        BufferedOutputStream(FileOutputStream(outPath.toFile())).use { bos ->
                            zis.copyTo(bos)
                        }
                        extractedFiles.add(outPath)
                        println("   • wyodrębniono: $baseName")
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (extractedFiles.isEmpty()) {
            println("⚠️  Paczka nie zawierała plików do wyodrębnienia.")
            return null
        }

        // Przenieś wyodrębnione pliki do katalogu aplikacji (nadpisz istniejące)
        var detectedJar: Path? = null
        for (extracted in extractedFiles) {
            val target = appDir.resolve(extracted.fileName.toString())
            Files.move(extracted, target, StandardCopyOption.REPLACE_EXISTING)
            // heurystyka: zapamiętaj JAR-a (preferuj *-all.jar)
            val name = target.fileName.toString()
            if (name.endsWith(".jar")) {
                if (detectedJar == null || name.contains("-all")) {
                    detectedJar = target
                }
                // spróbuj ustawić bit wykonywalny (nie szkodzi, jeśli się nie uda)
                try { target.toFile().setExecutable(true) } catch (_: Exception) {}
            }
        }

        // Posprzątaj katalog tymczasowy
        try { tmpExtractDir.toFile().deleteRecursively() } catch (_: Exception) {}

        println("✅ Pliki zainstalowane w: ${appDir.toAbsolutePath()}")
        detectedJar?.let { println("   Główny JAR: $it") }
        return detectedJar
    }

    /**
     * (Opcjonalne narzędzie) Znajdź „najlepszy” JAR w katalogu aplikacji – przyda się przy starcie.
     */
    fun findMainJar(): Path? {
        val appDir = Paths.appDir
        if (!appDir.exists()) return null
        val jars = appDir.listFiles { f: File -> f.isFile && f.name.endsWith(".jar") }?.toList().orEmpty()
        if (jars.isEmpty()) return null
        // preferuj *-all.jar, inaczej pierwszy z brzegu
        val preferred = jars.firstOrNull { it.name.contains("-all") } ?: jars.first()
        return preferred.toPath()
    }
}
