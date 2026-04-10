package com.example.musicapp.data.drive

import java.io.IOException
import java.io.InputStream

/**
 * Output of [FlacStreamMetadataParser.parse].
 *
 * All fields are nullable because a FLAC file is not required to carry any particular
 * metadata block other than STREAMINFO. Callers should treat missing fields as "unknown".
 */
internal data class FlacMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val pictureBytes: ByteArray? = null,
)

/**
 * Pure-Kotlin streaming parser for FLAC metadata. Reads the fLaC magic, walks every
 * METADATA_BLOCK header, and extracts STREAMINFO duration, VORBIS_COMMENT tags and
 * PICTURE block bytes as it goes. The parser stops reading as soon as it sees a block
 * with the last-metadata-block flag set, so callers can disconnect the underlying HTTP
 * stream and avoid downloading audio frames.
 *
 * Intentionally does NOT buffer the whole file and does NOT touch the filesystem.
 *
 * Spec reference: https://xiph.org/flac/format.html
 */
internal object FlacStreamMetadataParser {

    // Large, but bounded so a malformed/adversarial file cannot force an unbounded
    // allocation. 16 MiB comfortably fits any realistic embedded cover art.
    private const val MAX_BLOCK_SIZE = 16 * 1024 * 1024

    private const val BLOCK_TYPE_STREAMINFO = 0
    private const val BLOCK_TYPE_VORBIS_COMMENT = 4
    private const val BLOCK_TYPE_PICTURE = 6

    fun parse(input: InputStream): FlacMetadata? {
        val magic = ByteArray(4)
        if (!tryReadFully(input, magic)) return null
        if (magic[0] != 'f'.code.toByte() ||
            magic[1] != 'L'.code.toByte() ||
            magic[2] != 'a'.code.toByte() ||
            magic[3] != 'C'.code.toByte()
        ) {
            return null
        }

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs: Long? = null
        var pictureBytes: ByteArray? = null

        while (true) {
            val header = ByteArray(4)
            if (!tryReadFully(input, header)) break

            val isLast = (header[0].toInt() and 0x80) != 0
            val blockType = header[0].toInt() and 0x7F
            val length = ((header[1].toInt() and 0xFF) shl 16) or
                ((header[2].toInt() and 0xFF) shl 8) or
                (header[3].toInt() and 0xFF)

            if (length < 0 || length > MAX_BLOCK_SIZE) {
                // Skip oversized / bogus block and keep walking the chain.
                if (!trySkipFully(input, length.toLong())) break
                if (isLast) break
                continue
            }

            val blockData = ByteArray(length)
            if (!tryReadFully(input, blockData)) break

            when (blockType) {
                BLOCK_TYPE_STREAMINFO -> {
                    durationMs = parseStreamInfoDuration(blockData)
                }

                BLOCK_TYPE_VORBIS_COMMENT -> {
                    val comments = parseVorbisComments(blockData)
                    if (title == null) title = comments["TITLE"]
                    if (artist == null) artist = comments["ARTIST"] ?: comments["ALBUMARTIST"]
                    if (album == null) album = comments["ALBUM"]
                }

                BLOCK_TYPE_PICTURE -> {
                    if (pictureBytes == null) pictureBytes = parsePictureBlock(blockData)
                }
            }

            if (isLast) break
        }

        return FlacMetadata(
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            pictureBytes = pictureBytes,
        )
    }

    /**
     * STREAMINFO is a fixed 34-byte block. We only need the packed sample-rate +
     * total-samples field (bytes 10..17) to derive duration:
     *
     *   20 bits : sample rate
     *    3 bits : channels - 1
     *    5 bits : bits per sample - 1
     *   36 bits : total samples
     */
    private fun parseStreamInfoDuration(data: ByteArray): Long? {
        if (data.size < 18) return null
        val b10 = data[10].toInt() and 0xFF
        val b11 = data[11].toInt() and 0xFF
        val b12 = data[12].toInt() and 0xFF
        val sampleRate = (b10 shl 12) or (b11 shl 4) or (b12 ushr 4)
        val totalSamplesHigh = (data[13].toInt() and 0x0F).toLong()
        val b14 = (data[14].toInt() and 0xFF).toLong()
        val b15 = (data[15].toInt() and 0xFF).toLong()
        val b16 = (data[16].toInt() and 0xFF).toLong()
        val b17 = (data[17].toInt() and 0xFF).toLong()
        val totalSamples = (totalSamplesHigh shl 32) or
            (b14 shl 24) or
            (b15 shl 16) or
            (b16 shl 8) or
            b17
        if (sampleRate <= 0 || totalSamples <= 0L) return null
        return (totalSamples * 1000L) / sampleRate.toLong()
    }

    /**
     * Vorbis comment block layout (all integers little-endian):
     *
     *   u32  vendor_length
     *   u8 * vendor_length       vendor string (UTF-8)
     *   u32  user_comment_count
     *   for each comment:
     *     u32  length
     *     u8 * length            "TAG=value" (UTF-8)
     */
    private fun parseVorbisComments(data: ByteArray): Map<String, String> {
        if (data.size < 8) return emptyMap()
        var pos = 0

        val vendorLength = readInt32LE(data, pos)
        pos += 4
        if (vendorLength < 0 || pos + vendorLength > data.size) return emptyMap()
        pos += vendorLength

        if (pos + 4 > data.size) return emptyMap()
        val commentCount = readInt32LE(data, pos)
        pos += 4
        // Sanity bound — a reasonable FLAC has at most a few dozen tags.
        if (commentCount < 0 || commentCount > 4096) return emptyMap()

        val result = LinkedHashMap<String, String>()
        repeat(commentCount) {
            if (pos + 4 > data.size) return@repeat
            val commentLength = readInt32LE(data, pos)
            pos += 4
            if (commentLength < 0 || pos + commentLength > data.size) return@repeat

            val comment = String(data, pos, commentLength, Charsets.UTF_8)
            pos += commentLength

            val eq = comment.indexOf('=')
            if (eq > 0) {
                val key = comment.substring(0, eq).uppercase()
                val value = comment.substring(eq + 1)
                if (value.isNotBlank() && !result.containsKey(key)) {
                    result[key] = value
                }
            }
        }
        return result
    }

    /**
     * FLAC PICTURE block layout (all integers big-endian):
     *
     *   u32  picture type
     *   u32  mime length
     *   u8 * mime length        MIME type (ASCII)
     *   u32  description length
     *   u8 * description length description (UTF-8)
     *   u32  width
     *   u32  height
     *   u32  color depth
     *   u32  colors used
     *   u32  picture data length
     *   u8 * picture data length raw image bytes
     */
    private fun parsePictureBlock(data: ByteArray): ByteArray? {
        if (data.size < 32) return null
        var pos = 0

        pos += 4 // picture type — we don't care which kind of cover it is

        if (pos + 4 > data.size) return null
        val mimeLen = readInt32BE(data, pos); pos += 4
        if (mimeLen < 0 || pos + mimeLen > data.size) return null
        pos += mimeLen

        if (pos + 4 > data.size) return null
        val descLen = readInt32BE(data, pos); pos += 4
        if (descLen < 0 || pos + descLen > data.size) return null
        pos += descLen

        // Skip width/height/depth/colors-used (4 * u32).
        if (pos + 16 > data.size) return null
        pos += 16

        if (pos + 4 > data.size) return null
        val picLen = readInt32BE(data, pos); pos += 4
        if (picLen <= 0 || pos + picLen > data.size) return null

        return data.copyOfRange(pos, pos + picLen)
    }

    private fun readInt32BE(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)

    private fun readInt32LE(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)

    private fun tryReadFully(input: InputStream, buf: ByteArray): Boolean {
        var offset = 0
        while (offset < buf.size) {
            val n = try {
                input.read(buf, offset, buf.size - offset)
            } catch (_: IOException) {
                return false
            }
            if (n < 0) return false
            offset += n
        }
        return true
    }

    private fun trySkipFully(input: InputStream, count: Long): Boolean {
        var remaining = count
        while (remaining > 0) {
            val skipped = try {
                input.skip(remaining)
            } catch (_: IOException) {
                return false
            }
            if (skipped <= 0) {
                val oneByte = try {
                    input.read()
                } catch (_: IOException) {
                    return false
                }
                if (oneByte < 0) return false
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
        return true
    }
}
