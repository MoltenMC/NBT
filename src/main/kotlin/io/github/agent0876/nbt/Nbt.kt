package io.github.agent0876.nbt

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * Reads and writes Minecraft NBT binary format.
 *
 * Java Edition uses big-endian byte order with gzip compression at the file level.
 * Bedrock Edition uses little-endian byte order with no compression.
 *
 * Use [NbtIo] for file-level I/O (gzip, path helpers).
 */
object Nbt {

    /** Reads a root compound from a Java Edition NBT byte array (big-endian). */
    fun readJava(bytes: ByteArray): NbtCompound = read(bytes, littleEndian = false)

    /** Reads a root compound from a Bedrock Edition NBT byte array (little-endian). */
    fun readBedrock(bytes: ByteArray): NbtCompound = read(bytes, littleEndian = true)

    /** Reads a root compound. The root name is skipped (usually empty). */
    fun read(bytes: ByteArray, littleEndian: Boolean = false): NbtCompound {
        val buf = ByteBuffer.wrap(bytes)
            .order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        return NbtReader(buf).readRoot()
    }

    /** Reads a root compound and returns the root name alongside it. */
    fun readNamed(bytes: ByteArray, littleEndian: Boolean = false): Pair<String, NbtCompound> {
        val buf = ByteBuffer.wrap(bytes)
            .order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        return NbtReader(buf).readRootNamed()
    }

    /** Writes a compound as a Java Edition NBT byte array (big-endian, empty root name). */
    fun writeJava(compound: NbtCompound, rootName: String = ""): ByteArray =
        write(compound, rootName, littleEndian = false)

    /** Writes a compound as a Bedrock Edition NBT byte array (little-endian, empty root name). */
    fun writeBedrock(compound: NbtCompound, rootName: String = ""): ByteArray =
        write(compound, rootName, littleEndian = true)

    /** Writes a compound to a byte array. */
    fun write(compound: NbtCompound, rootName: String = "", littleEndian: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        NbtWriter(out, littleEndian).writeRoot(compound, rootName)
        return out.toByteArray()
    }
}

// ── Reader ────────────────────────────────────────────────────────────────────

private class NbtReader(private val buf: ByteBuffer) {

    fun readRoot(): NbtCompound {
        val typeId = buf.get()
        if (typeId != NbtTagId.COMPOUND) throw IOException("Root tag must be TAG_Compound, got $typeId")
        readString() // skip root name
        return readCompound()
    }

    fun readRootNamed(): Pair<String, NbtCompound> {
        val typeId = buf.get()
        if (typeId != NbtTagId.COMPOUND) throw IOException("Root tag must be TAG_Compound, got $typeId")
        val name = readString()
        return name to readCompound()
    }

    private fun readTag(typeId: Byte): NbtTag = when (typeId) {
        NbtTagId.END -> NbtEnd
        NbtTagId.BYTE -> NbtByte(buf.get())
        NbtTagId.SHORT -> NbtShort(buf.getShort())
        NbtTagId.INT -> NbtInt(buf.getInt())
        NbtTagId.LONG -> NbtLong(buf.getLong())
        NbtTagId.FLOAT -> NbtFloat(buf.getFloat())
        NbtTagId.DOUBLE -> NbtDouble(buf.getDouble())
        NbtTagId.BYTE_ARRAY -> {
            val len = buf.getInt()
            NbtByteArray(ByteArray(len).also { buf.get(it) })
        }
        NbtTagId.STRING -> NbtString(readString())
        NbtTagId.LIST -> readList()
        NbtTagId.COMPOUND -> readCompound()
        NbtTagId.INT_ARRAY -> {
            val len = buf.getInt()
            NbtIntArray(IntArray(len) { buf.getInt() })
        }
        NbtTagId.LONG_ARRAY -> {
            val len = buf.getInt()
            NbtLongArray(LongArray(len) { buf.getLong() })
        }
        else -> throw IOException("Unknown NBT tag type: $typeId")
    }

    private fun readCompound(): NbtCompound {
        val compound = NbtCompound()
        while (buf.hasRemaining()) {
            val typeId = buf.get()
            if (typeId == NbtTagId.END) break
            compound.tags[readString()] = readTag(typeId)
        }
        return compound
    }

    private fun readList(): NbtList {
        val elementType = buf.get()
        val count = buf.getInt()
        val list = NbtList(elementType)
        repeat(count) { list.tags.add(readTag(elementType)) }
        return list
    }

    private fun readString(): String {
        val len = buf.getShort().toInt() and 0xFFFF
        if (len == 0) return ""
        val bytes = ByteArray(len).also { buf.get(it) }
        return String(bytes, StandardCharsets.UTF_8)
    }
}

// ── Writer ────────────────────────────────────────────────────────────────────

private class NbtWriter(private val out: ByteArrayOutputStream, private val littleEndian: Boolean) {

    private val scratch = ByteBuffer.allocate(8)
        .also { if (littleEndian) it.order(ByteOrder.LITTLE_ENDIAN) }

    fun writeRoot(compound: NbtCompound, rootName: String) {
        out.write(NbtTagId.COMPOUND.toInt())
        writeString(rootName)
        writeCompoundPayload(compound)
    }

    private fun writeTag(tag: NbtTag) {
        when (tag) {
            is NbtEnd -> {}
            is NbtByte -> out.write(tag.value.toInt())
            is NbtShort -> writeShort(tag.value)
            is NbtInt -> writeInt(tag.value)
            is NbtLong -> writeLong(tag.value)
            is NbtFloat -> writeFloat(tag.value)
            is NbtDouble -> writeDouble(tag.value)
            is NbtByteArray -> { writeInt(tag.value.size); out.write(tag.value) }
            is NbtString -> writeString(tag.value)
            is NbtList -> writeListPayload(tag)
            is NbtCompound -> writeCompoundPayload(tag)
            is NbtIntArray -> { writeInt(tag.value.size); tag.value.forEach { writeInt(it) } }
            is NbtLongArray -> { writeInt(tag.value.size); tag.value.forEach { writeLong(it) } }
        }
    }

    private fun writeCompoundPayload(compound: NbtCompound) {
        for ((name, tag) in compound.tags) {
            out.write(NbtTagId.of(tag).toInt())
            writeString(name)
            writeTag(tag)
        }
        out.write(NbtTagId.END.toInt())
    }

    private fun writeListPayload(list: NbtList) {
        out.write(list.elementType.toInt())
        writeInt(list.tags.size)
        list.tags.forEach { writeTag(it) }
    }

    private fun writeString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeShort(bytes.size.toShort())
        out.write(bytes)
    }

    private fun writeShort(value: Short) {
        scratch.clear(); scratch.putShort(value); out.write(scratch.array(), 0, 2)
    }

    private fun writeInt(value: Int) {
        scratch.clear(); scratch.putInt(value); out.write(scratch.array(), 0, 4)
    }

    private fun writeLong(value: Long) {
        scratch.clear(); scratch.putLong(value); out.write(scratch.array(), 0, 8)
    }

    private fun writeFloat(value: Float) {
        scratch.clear(); scratch.putFloat(value); out.write(scratch.array(), 0, 4)
    }

    private fun writeDouble(value: Double) {
        scratch.clear(); scratch.putDouble(value); out.write(scratch.array(), 0, 8)
    }
}
