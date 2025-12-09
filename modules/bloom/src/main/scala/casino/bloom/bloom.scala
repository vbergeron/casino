package casino.bloom

import scala.util.hashing.MurmurHash3

val seed1 = "hash1".##
val seed2 = "hash2".##

def hashes(x: Int, n: Int): Array[Int] =
    val h1 = MurmurHash3.finalizeHash(MurmurHash3.mix(x, seed1), 1)
    val h2 = MurmurHash3.finalizeHash(MurmurHash3.mix(x, seed2), 1)

    val hs = new Array[Int](n)
    var i  = 0
    while i < n do
        hs(i) = h1 + i * h2
        i += 1
    hs
