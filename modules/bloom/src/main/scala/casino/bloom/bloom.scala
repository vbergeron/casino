package casino.bloom

import scala.util.hashing.MurmurHash3
import scala.annotation.tailrec

case class BloomFilter(bitset: Array[Long], seed: Int, k: Int) {
    def merge(that: BloomFilter): Unit =
        require(
          this.bitset.length == that.bitset.length,
          "Bloom filters must have the same size to be combined"
        )
        require(
          this.k == that.k && this.seed == that.seed,
          "Bloom filters must have the same hash parameters to be combined"
        )

        var i = 0
        while i < bitset.length do
            bitset(i) = bitset(i) | that.bitset(i)
            i += 1

    def add(item: Array[Byte]): Unit =
        val base = hash128(item, seed)
        val h1   = base(0)
        val h2   = base(1)

        @tailrec
        def loop(i: Int): Unit =
            if i < k then
                val hash  = h1 + i * h2
                val index = math.abs(hash % (bitset.length * 64L))
                val word  = (index / 64).toInt
                val mask  = 1L << (index % 64)
                bitset(word) = bitset(word) | mask
                loop(i + 1)

        loop(0)

    def contains(item: Array[Byte]): Boolean =
        val base = hash128(item, seed)
        val h1   = base(0)
        val h2   = base(1)

        @tailrec
        def loop(i: Int): Boolean =
            if i < k then
                val hash  = h1 + i * h2
                val index = math.abs(hash % (bitset.length * 64L))
                val word  = (index / 64).toInt
                val mask  = 1L << (index % 64)
                if (bitset(word) & mask) == 0L then false
                else loop(i + 1)
            else true

        loop(0)

}

case class BloomFilterSpec(
    items: Long,
    falsePositiveRate: Double,
    bits: Long,
    hashes: Int,
    seed: Int = 0
) {
    def create: BloomFilter =
        val longSize = (bits + 63L) / 64L
        BloomFilter(new Array[Long](longSize.toInt), seed, hashes)
}

object BloomFilter {
    /*
    n = ceil(m / (-k / log(1 - exp(log(p) / k))))
    p = pow(1 - exp(-k / (m / n)), k)
    m = ceil((n * log(p)) / log(1 / pow(2, log(2))));
    k = round((m / n) * log(2));
     */

    def m(n: Long, p: Double): Long =
        math.ceil((n * math.log(p)) / math.log(1 / math.pow(2, math.log(2)))).toLong

    def k(n: Long, m: Long): Int =
        math.round((m.toDouble / n.toDouble) * math.log(2)).toInt

    def p(n: Long, m: Long, k: Int): Double =
        math.pow(1 - math.exp(-k.toDouble / (m.toDouble / n.toDouble)), k.toDouble)

    def fromNP(n: Long, p: Double, seed: Int = 0): BloomFilterSpec =
        val m = BloomFilter.m(n, p)
        val k = BloomFilter.k(n, m)
        BloomFilterSpec(n, p, m, k, seed)

    def fromNM(n: Long, m: Long, seed: Int = 0): BloomFilterSpec =
        val k = BloomFilter.k(n, m)
        val p = BloomFilter.p(n, m, k)
        BloomFilterSpec(n, p, m, k, seed)

}
