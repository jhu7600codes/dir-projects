package dev.jhu.crossi.generator

import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PatternTest {
    @Test
    fun `patterns follow the nyt rules for every size`() {
        for (size in PatternSpec.SIZES) {
            val spec = PatternSpec.forSize(size)
            val rng = Rng(size.toLong())
            var made = 0
            repeat(100) {
                // a miss is fine now and then, the puzzle generator just tries again
                val shape = PatternGenerator(spec, rng.fork()).generate() ?: return@repeat
                made++
                assertTrue(shape.isSymmetric(), "not symmetric")
                assertTrue(shape.isConnected(), "not connected")
                assertTrue(shape.blockCount in spec.blocks, "block count ${shape.blockCount} for $size")
                for (slot in shape.slots) {
                    assertTrue(slot.length >= 3, "run of ${slot.length}")
                    assertTrue(slot.length <= spec.maxRun, "run of ${slot.length} > ${spec.maxRun}")
                }
            }
            assertTrue(made >= 80, "only $made/100 patterns for $size")
        }
    }

    @Test
    fun `every white cell is in an across and a down word`() {
        val shape = assertNotNull(PatternGenerator(PatternSpec.forSize(15), Rng(3)).generate())
        val across = HashSet<Int>()
        val down = HashSet<Int>()
        for (slot in shape.slots) (if (slot.direction == Direction.ACROSS) across else down).addAll(slot.cells.toList())
        val white = shape.blocks.indices.filter { !shape.blocks[it] }.toSet()
        assertTrue(across == white && down == white)
    }

    @Test
    fun `seeded long word keeps its exact run`() {
        val seed = SeedPlacement("ONOMATOPOEIA", 2, 0)
        val partner = SeedPlacement("PROCRASTINAT", 12, 3)
        val shape = assertNotNull(PatternGenerator(PatternSpec.forSize(15), Rng(9)).generate(listOf(seed, partner)))
        assertTrue(shape.slots.any { it.direction == Direction.ACROSS && it.row == 2 && it.col == 0 && it.length == 12 })
    }
}
