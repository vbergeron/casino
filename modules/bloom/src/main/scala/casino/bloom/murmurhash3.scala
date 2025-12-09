package casino.bloom

import java.lang.invoke.MethodHandles
import java.nio.ByteOrder

private val C1 = 0x87c37b91114253d5L
private val C2 = 0x4cf5ad432745937fL

private val R1 = 31
private val R2 = 27
private val R3 = 33

private val M  = 5
private val N1 = 0x52dce729
private val N2 = 0x38495ab5

private val LONG_LE = MethodHandles.byteArrayViewVarHandle(classOf[Array[Long]], ByteOrder.LITTLE_ENDIAN)

private inline def getLittleEndianLong(data: Array[Byte], index: Int): Long =
    LONG_LE.get(data, index)

private inline def rotateLeft(value: Long, distance: Int): Long =
    java.lang.Long.rotateLeft(value, distance)

private def fmix64(hash: Long): Long =
    var h = hash
    h ^= h >>> 33
    h *= 0xff51afd7ed558ccdL
    h ^= h >>> 33
    h *= 0xc4ceb9fe1a85ec53L
    h ^= h >>> 33
    h

def hash128(data: Array[Byte], seed: Int): Array[Long] =
    hash128(data, 0, data.length, seed.toLong & 0xffffffffL)

def hash128(data: Array[Byte], offset: Int, length: Int, seed: Long): Array[Long] =
    var h1 = seed
    var h2 = seed

    val nblocks = length >> 4

    // blocks
    var i = 0
    while i < nblocks do
        val index = offset + (i << 4)
        var k1    = getLittleEndianLong(data, index)
        var k2    = getLittleEndianLong(data, index + 8)

        // mix k1
        k1 *= C1
        k1 = rotateLeft(k1, R1)
        k1 *= C2
        h1 ^= k1
        h1 = rotateLeft(h1, R2)
        h1 += h2
        h1 = h1 * M + N1

        // mix k2
        k2 *= C2
        k2 = rotateLeft(k2, R3)
        k2 *= C1
        h2 ^= k2
        h2 = rotateLeft(h2, R1)
        h2 += h1
        h2 = h2 * M + N2

        i += 1

    // tail
    var k1 = 0L
    var k2 = 0L

    val index = offset + (nblocks << 4)
    var rem   = offset + length - index // how many tail bytes (0..15)

    while rem > 0 do
        rem match
            case 15 => k2 ^= (data(index + 14) & 0xffL) << 48
            case 14 => k2 ^= (data(index + 13) & 0xffL) << 40
            case 13 => k2 ^= (data(index + 12) & 0xffL) << 32
            case 12 => k2 ^= (data(index + 11) & 0xffL) << 24
            case 11 => k2 ^= (data(index + 10) & 0xffL) << 16
            case 10 => k2 ^= (data(index + 9) & 0xffL) << 8
            case 9  =>
                k2 ^= (data(index + 8) & 0xffL)
                k2 *= C2
                k2 = rotateLeft(k2, R3)
                k2 *= C1
                h2 ^= k2

            case 8 => k1 ^= (data(index + 7) & 0xffL) << 56
            case 7 => k1 ^= (data(index + 6) & 0xffL) << 48
            case 6 => k1 ^= (data(index + 5) & 0xffL) << 40
            case 5 => k1 ^= (data(index + 4) & 0xffL) << 32
            case 4 => k1 ^= (data(index + 3) & 0xffL) << 24
            case 3 => k1 ^= (data(index + 2) & 0xffL) << 16
            case 2 => k1 ^= (data(index + 1) & 0xffL) << 8
            case 1 =>
                k1 ^= (data(index) & 0xffL)
                k1 *= C1
                k1 = rotateLeft(k1, R1)
                k1 *= C2
                h1 ^= k1

            case _ => // rem should never be outside 1..15
        rem -= 1

    // finalization
    h1 ^= length
    h2 ^= length

    h1 += h2
    h2 += h1

    h1 = fmix64(h1)
    h2 = fmix64(h2)

    h1 += h2
    h2 += h1

    Array(h1, h2)
