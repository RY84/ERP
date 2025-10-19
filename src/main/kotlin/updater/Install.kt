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
 * - spłaszcza ścieżki w ZIP (ignoruje katalogi) i zapisuje same pliki
 * - ignoruje duplikaty nazw po spłaszczeniu (np. gdy ZIP ma plik w root i w podkatalogu)
 * - zwraca ścieżkę do potencjalnego JAR-a (jeśli znajdzie)
 */
object Install {

    fun installFrom(zipPath: Path): Path? {
        require(Files.exists(zipPath)) { "Brak pliku ZIP: $zipPath" }

        val appDir = Paths.appDir.toPath()
        Files.createDirectories(appDir)

        val tmpExtractDir = Paths.tmpDir.resolve("extract").toPath()
        if (Files.exists(tmpExtractDir)) {
            tmpExtractDir.toFile().deleteRecursively()
        }
        Files.createDirectories(tmpExtractDir)

        println("📦 Instalacja paczki do: ${appDir.toAbsolutePath()}")
        println("   ZIP: $zipPath")

        val extractedFiles = mutableListOf<Path>()
        val seenNames = mutableSetOf<String>() // do wykrywania duplikatów po spłaszczeniu

        ZipInputStream(BufferedInputStream(Files.newInputStream(zipPath))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val baseName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (baseName.isNotBlank()) {
                        if (!seenNames.add(baseName)) {
                            // już był plik o tej nazwie – ignorujemy duplikat
                            println("   • pomijam duplikat: $baseName")
                        } else {
                            val outPath = tmpExtractDir.resolve(baseName)
                            Files.createDirectories(outPath.parent)
                            BufferedOutputStream(FileOutputStream(outPath.toFile())).use { bos ->
                                zis.copyTo(bos)
                            }
                            extractedFiles.add(outPath)
                            println("   • wyodrębniono: $baseName")
                        }
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

        var detectedJar: Path? = null
        for (extracted in extractedFiles) {
            val target = appDir.resolve(extracted.fileName.toString())
            // przenieś, nadpisując
            Files.move(extracted, target, StandardCopyOption.REPLACE_EXISTING)
            val name = target.fileName.toString()
            if (name.endsWith(".jar")) {
                if (detectedJar == null || name.contains("-all")) {
                    detectedJar = target
                }
                try { target.toFile().setExecutable(true) } catch (_: Exception) {}
            }
        }

        // posprzątaj katalog tymczasowy
        try { tmpExtractDir.toFile().deleteRecursively() } catch (_: Exception) {}

        println("✅ Pliki zainstalowane w: ${appDir.toAbsolutePath()}")
        detectedJar?.let { println("   Główny JAR: $it") }
        return detectedJar
    }

    fun findMainJar(): Path? {
        val appDir = Paths.appDir
        if (!appDir.exists()) return null
        val jars = appDir.listFiles { f: File -> f.isFile && f.name.endsWith(".jar") }?.toList().orEmpty()
        if (jars.isEmpty()) return null
        val preferred = jars.firstOrNull { it.name.contains("-all") } ?: jars.first()
        return preferred.toPath()
    }
}
