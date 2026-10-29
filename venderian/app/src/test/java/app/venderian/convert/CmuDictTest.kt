package app.venderian.convert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CmuDictTest {
    @Test
    fun `binary search over a small dict`() {
        val dict = CmuDict("aa AA1\nabc EY1 B IY1 S IY1\nb B IY1\nzz Z IY1 Z IY1\n".toByteArray())
        assertEquals(4, dict.size)
        assertEquals(listOf("AA1"), dict.lookup("aa"))
        assertEquals(listOf("Z", "IY1", "Z", "IY1"), dict.lookup("zz"))
        assertEquals(listOf("B", "IY1"), dict.lookup("B"))
        assertNull(dict.lookup("ab"))
        assertNull(dict.lookup("abcd"))
        assertNull(dict.lookup("a"))
    }

    @Test
    fun `the bundled cmudict loads`() {
        val dict = CmuDict.fromGzip(File("src/main/assets/cmudict.txt.gz").inputStream())
        assertTrue(dict.size > 100_000)
        assertEquals(listOf("HH", "AH0", "L", "OW1"), dict.lookup("hello"))
        assertEquals(listOf("D", "OW1", "N", "T"), dict.lookup("don't"))
        assertNull(dict.lookup("venderianish"))
    }
}
