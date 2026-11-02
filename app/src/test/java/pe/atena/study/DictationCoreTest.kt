package pe.atena.study

import org.junit.Assert.*
import org.junit.Test

class DictationCoreTest {
    @Test fun threeWordsAndLastShort() {
        assertEquals(listOf("uno dos tres", "cuatro cinco"), DictationCore.blocks("uno dos tres cuatro cinco"))
        assertEquals(emptyList<String>(), DictationCore.blocks("   "))
    }
    @Test fun normalizeVoiceCommands() {
        assertEquals("yaya", DictationCore.normalize("¡YAYÁ! "))
        assertEquals("no entendi", DictationCore.normalize("No entendí"))
    }
    @Test fun incompleteEchoRepetition() {
        assertTrue(DictationCore.isEcho("La tensión", "Disminuye la tensión"))
        assertTrue(DictationCore.isEcho("Disminuye la", "Disminuye la tensión"))
        assertFalse(DictationCore.isEcho("YAYA", "Disminuye la tensión"))
    }
}
