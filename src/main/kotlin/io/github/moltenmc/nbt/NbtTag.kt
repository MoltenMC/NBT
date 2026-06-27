package io.github.moltenmc.nbt

sealed class NbtTag

object NbtEnd : NbtTag()

data class NbtByte(val value: Byte) : NbtTag()
data class NbtShort(val value: Short) : NbtTag()
data class NbtInt(val value: Int) : NbtTag()
data class NbtLong(val value: Long) : NbtTag()
data class NbtFloat(val value: Float) : NbtTag()
data class NbtDouble(val value: Double) : NbtTag()

class NbtByteArray(val value: ByteArray) : NbtTag() {
    override fun equals(other: Any?) = other is NbtByteArray && value.contentEquals(other.value)
    override fun hashCode() = value.contentHashCode()
}

data class NbtString(val value: String) : NbtTag()

class NbtList(val elementType: Byte = NbtTagId.END) : NbtTag() {
    val tags: MutableList<NbtTag> = mutableListOf()
    val size get() = tags.size
    operator fun get(index: Int): NbtTag = tags[index]
    fun add(tag: NbtTag) = tags.add(tag)
}

class NbtCompound : NbtTag() {
    val tags: LinkedHashMap<String, NbtTag> = LinkedHashMap()

    operator fun contains(key: String) = key in tags
    operator fun get(key: String): NbtTag? = tags[key]
    operator fun set(key: String, tag: NbtTag) { tags[key] = tag }

    fun getByte(key: String): Byte = (tags[key] as? NbtByte)?.value ?: 0
    fun getShort(key: String): Short = (tags[key] as? NbtShort)?.value ?: 0
    fun getInt(key: String): Int = (tags[key] as? NbtInt)?.value ?: 0
    fun getLong(key: String): Long = (tags[key] as? NbtLong)?.value ?: 0L
    fun getFloat(key: String): Float = (tags[key] as? NbtFloat)?.value ?: 0f
    fun getDouble(key: String): Double = (tags[key] as? NbtDouble)?.value ?: 0.0
    fun getString(key: String): String = (tags[key] as? NbtString)?.value ?: ""
    fun getBoolean(key: String): Boolean = getByte(key) != 0.toByte()
    fun getCompound(key: String): NbtCompound? = tags[key] as? NbtCompound
    fun getList(key: String): NbtList? = tags[key] as? NbtList
    fun getByteArray(key: String): ByteArray? = (tags[key] as? NbtByteArray)?.value
    fun getIntArray(key: String): IntArray? = (tags[key] as? NbtIntArray)?.value
    fun getLongArray(key: String): LongArray? = (tags[key] as? NbtLongArray)?.value

    fun putByte(key: String, value: Byte) { tags[key] = NbtByte(value) }
    fun putShort(key: String, value: Short) { tags[key] = NbtShort(value) }
    fun putInt(key: String, value: Int) { tags[key] = NbtInt(value) }
    fun putLong(key: String, value: Long) { tags[key] = NbtLong(value) }
    fun putFloat(key: String, value: Float) { tags[key] = NbtFloat(value) }
    fun putDouble(key: String, value: Double) { tags[key] = NbtDouble(value) }
    fun putString(key: String, value: String) { tags[key] = NbtString(value) }
    fun putBoolean(key: String, value: Boolean) { tags[key] = NbtByte(if (value) 1 else 0) }
    fun putByteArray(key: String, value: ByteArray) { tags[key] = NbtByteArray(value) }
    fun putIntArray(key: String, value: IntArray) { tags[key] = NbtIntArray(value) }
    fun putLongArray(key: String, value: LongArray) { tags[key] = NbtLongArray(value) }
    fun putCompound(key: String, block: NbtCompound.() -> Unit) { tags[key] = NbtCompound().also(block) }
    fun putList(key: String, elementType: Byte, block: NbtList.() -> Unit) { tags[key] = NbtList(elementType).also(block) }
}

class NbtIntArray(val value: IntArray) : NbtTag() {
    override fun equals(other: Any?) = other is NbtIntArray && value.contentEquals(other.value)
    override fun hashCode() = value.contentHashCode()
}

class NbtLongArray(val value: LongArray) : NbtTag() {
    override fun equals(other: Any?) = other is NbtLongArray && value.contentEquals(other.value)
    override fun hashCode() = value.contentHashCode()
}

fun nbtCompound(block: NbtCompound.() -> Unit = {}): NbtCompound = NbtCompound().also(block)

internal object NbtTagId {
    const val END: Byte = 0
    const val BYTE: Byte = 1
    const val SHORT: Byte = 2
    const val INT: Byte = 3
    const val LONG: Byte = 4
    const val FLOAT: Byte = 5
    const val DOUBLE: Byte = 6
    const val BYTE_ARRAY: Byte = 7
    const val STRING: Byte = 8
    const val LIST: Byte = 9
    const val COMPOUND: Byte = 10
    const val INT_ARRAY: Byte = 11
    const val LONG_ARRAY: Byte = 12

    fun of(tag: NbtTag): Byte = when (tag) {
        is NbtEnd -> END
        is NbtByte -> BYTE
        is NbtShort -> SHORT
        is NbtInt -> INT
        is NbtLong -> LONG
        is NbtFloat -> FLOAT
        is NbtDouble -> DOUBLE
        is NbtByteArray -> BYTE_ARRAY
        is NbtString -> STRING
        is NbtList -> LIST
        is NbtCompound -> COMPOUND
        is NbtIntArray -> INT_ARRAY
        is NbtLongArray -> LONG_ARRAY
    }
}
