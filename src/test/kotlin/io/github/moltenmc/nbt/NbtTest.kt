package io.github.moltenmc.nbt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NbtTest {

    @Test
    fun `round-trip java edition big-endian`() {
        val original = nbtCompound {
            putByte("b", 42)
            putShort("s", 1000)
            putInt("i", 100_000)
            putLong("l", Long.MAX_VALUE)
            putFloat("f", 3.14f)
            putDouble("d", 2.718281828)
            putString("str", "hello world")
            putBoolean("flag", true)
            putByteArray("bytes", byteArrayOf(1, 2, 3))
            putIntArray("ints", intArrayOf(10, 20, 30))
            putLongArray("longs", longArrayOf(100L, 200L))
            putCompound("nested") {
                putString("key", "value")
                putInt("num", 99)
            }
        }

        val bytes = Nbt.writeJava(original)
        val restored = Nbt.readJava(bytes)

        assertEquals(42.toByte(), restored.getByte("b"))
        assertEquals(1000.toShort(), restored.getShort("s"))
        assertEquals(100_000, restored.getInt("i"))
        assertEquals(Long.MAX_VALUE, restored.getLong("l"))
        assertEquals(3.14f, restored.getFloat("f"))
        assertEquals(2.718281828, restored.getDouble("d"))
        assertEquals("hello world", restored.getString("str"))
        assertTrue(restored.getBoolean("flag"))
        assertArrayEquals(byteArrayOf(1, 2, 3), restored.getByteArray("bytes"))
        assertArrayEquals(intArrayOf(10, 20, 30), restored.getIntArray("ints"))
        assertArrayEquals(longArrayOf(100L, 200L), restored.getLongArray("longs"))

        val nested = restored.getCompound("nested")!!
        assertEquals("value", nested.getString("key"))
        assertEquals(99, nested.getInt("num"))
    }

    @Test
    fun `round-trip bedrock edition little-endian`() {
        val original = nbtCompound {
            putInt("StorageVersion", 9)
            putString("LevelName", "My World")
            putLong("RandomSeed", 12345678L)
        }

        val bytes = Nbt.writeBedrock(original)
        val restored = Nbt.readBedrock(bytes)

        assertEquals(9, restored.getInt("StorageVersion"))
        assertEquals("My World", restored.getString("LevelName"))
        assertEquals(12345678L, restored.getLong("RandomSeed"))
    }

    @Test
    fun `round-trip list tag`() {
        val original = nbtCompound {
            putList("items", NbtTagId.STRING) {
                add(NbtString("alpha"))
                add(NbtString("beta"))
                add(NbtString("gamma"))
            }
            putList("numbers", NbtTagId.INT) {
                add(NbtInt(1))
                add(NbtInt(2))
                add(NbtInt(3))
            }
        }

        val bytes = Nbt.writeJava(original)
        val restored = Nbt.readJava(bytes)

        val items = restored.getList("items")!!
        assertEquals(3, items.size)
        assertEquals("alpha", (items[0] as NbtString).value)
        assertEquals("gamma", (items[2] as NbtString).value)

        val numbers = restored.getList("numbers")!!
        assertEquals(3, numbers.size)
        assertEquals(2, (numbers[1] as NbtInt).value)
    }

    @Test
    fun `root name is preserved in named read`() {
        val compound = nbtCompound { putString("x", "y") }
        val bytes = Nbt.writeJava(compound, rootName = "Level")
        val (name, restored) = Nbt.readNamed(bytes)
        assertEquals("Level", name)
        assertEquals("y", restored.getString("x"))
    }

    @Test
    fun `gzip compressed round-trip via NbtIo`() {
        val original = nbtCompound { putInt("version", 26) }
        val compressed = NbtIo.writeCompressed(original)
        val restored = NbtIo.readCompressed(compressed)
        assertEquals(26, restored.getInt("version"))
    }

    @Test
    fun `java and bedrock parse their own bytes correctly`() {
        val compound = nbtCompound { putInt("value", 0x01020304) }
        assertEquals(0x01020304, Nbt.readJava(Nbt.writeJava(compound)).getInt("value"))
        assertEquals(0x01020304, Nbt.readBedrock(Nbt.writeBedrock(compound)).getInt("value"))
    }

    @Test
    fun `nested compound list round-trip`() {
        val original = nbtCompound {
            putList("sections", NbtTagId.COMPOUND) {
                add(nbtCompound { putInt("Y", 0); putString("name", "section0") })
                add(nbtCompound { putInt("Y", 1); putString("name", "section1") })
            }
        }

        val bytes = Nbt.writeJava(original)
        val restored = Nbt.readJava(bytes)
        val sections = restored.getList("sections")!!
        assertEquals(2, sections.size)
        assertEquals(0, (sections[0] as NbtCompound).getInt("Y"))
        assertEquals("section1", (sections[1] as NbtCompound).getString("name"))
    }
}
