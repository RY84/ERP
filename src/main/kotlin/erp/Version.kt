package erp

import java.io.BufferedReader
import java.io.InputStreamReader

object Version {
    val current: String by lazy {
        try {
            Version::class.java.getResourceAsStream("/version.txt")?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readLine().trim()
            } ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }
}
