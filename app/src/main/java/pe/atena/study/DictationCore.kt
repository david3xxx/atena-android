package pe.atena.study

import java.text.Normalizer
import java.util.Locale

/** Motor de texto local: sin red, sin API ni dependencias Android. */
object DictationCore {
    fun normalize(raw: String): String = Normalizer.normalize(raw.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun blocks(text: String, groupSize: Int = 3): List<String> {
        require(groupSize > 0)
        return text.trim().split(Regex("\\s+")).filter(String::isNotBlank)
            .chunked(groupSize).map { it.joinToString(" ") }
    }

    /** Distingue una orden de avance de un eco del bloque anterior. */
    fun isEcho(raw: String, lastBlock: String): Boolean {
        val echoed = normalize(raw)
        val last = normalize(lastBlock)
        if (echoed.isBlank() || last.isBlank() || echoed.length < 3) return false
        return last == echoed || last.startsWith("$echoed ") || last.contains(" $echoed ") || last.endsWith(" $echoed")
    }
}
