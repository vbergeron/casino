package casino.benchmarks

import org.openjdk.jmh.annotations.{*, given}
import org.openjdk.jmh.infra.Blackhole
import scala.compiletime.uninitialized
import casino.bloom.*

import java.util.concurrent.TimeUnit
import scala.util.Random
import scala.util.hashing.MurmurHash3
import casino.bloom.hash128

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = Array("-Xms2G", "-Xmx2G"))
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class HashComparison:

    @Param(Array("64", "256", "1024", "4096"))
    var size: Int = uninitialized

    var data: Array[Byte] = uninitialized

    @Setup(Level.Trial)
    def setup(): Unit =
        val random = new Random(42)
        data = new Array[Byte](size)
        random.nextBytes(data)

    @Benchmark
    def customMurmur128(blackhole: Blackhole): Unit =
        val result = hash128(data, 42)
        blackhole.consume(result)

    @Benchmark
    def standardMurmur32(blackhole: Blackhole): Unit =
        val result = MurmurHash3.bytesHash(data, 42)
        blackhole.consume(result)
