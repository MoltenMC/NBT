package io.github.moltenmc.nbt

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * File-level NBT I/O helpers.
 *
 * Java Edition chunk data (from Anvil) is stored as gzip-compressed big-endian NBT.
 * Bedrock Edition chunk data (from LevelDB) is stored as raw little-endian NBT.
 */
object NbtIo {

    /** Reads a gzip-compressed Java Edition NBT file. */
    fun readJavaFile(path: Path): NbtCompound =
        readCompressed(Files.readAllBytes(path))

    /** Writes a gzip-compressed Java Edition NBT file. */
    fun writeJavaFile(compound: NbtCompound, path: Path, rootName: String = "") {
        Files.write(path, writeCompressed(compound, rootName))
    }

    /** Reads a raw Bedrock Edition NBT file (little-endian, no compression). */
    fun readBedrockFile(path: Path): NbtCompound =
        Nbt.readBedrock(Files.readAllBytes(path))

    /** Writes a raw Bedrock Edition NBT file (little-endian, no compression). */
    fun writeBedrockFile(compound: NbtCompound, path: Path, rootName: String = "") {
        Files.write(path, Nbt.writeBedrock(compound, rootName))
    }

    /**
     * Decompresses gzip and reads as Java Edition NBT.
     * Use this on bytes returned by [io.github.moltenmc.anvil.Anvil.readChunk].
     */
    fun readCompressed(bytes: ByteArray): NbtCompound {
        val decompressed = GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()
        return Nbt.readJava(decompressed)
    }

    /**
     * Writes as Java Edition NBT and gzip-compresses.
     * Use this to produce bytes for [io.github.moltenmc.anvil.Anvil.writeChunk].
     */
    fun writeCompressed(compound: NbtCompound, rootName: String = ""): ByteArray {
        val raw = Nbt.writeJava(compound, rootName)
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(raw) }
        return out.toByteArray()
    }

    /**
     * Reads Java Edition NBT from raw (already decompressed) bytes.
     * Anvil's readChunk already decompresses, so use [Nbt.readJava] directly instead.
     */
    fun readRaw(bytes: ByteArray): NbtCompound = Nbt.readJava(bytes)

    /** Writes Java Edition NBT to raw (uncompressed) bytes. */
    fun writeRaw(compound: NbtCompound, rootName: String = ""): ByteArray =
        Nbt.writeJava(compound, rootName)
}
