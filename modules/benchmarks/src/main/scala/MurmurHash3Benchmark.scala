package casino.benchmarks

import org.openjdk.jmh.annotations.{*, given}
import org.openjdk.jmh.infra.Blackhole
import scala.compiletime.uninitialized
import casino.bloom.*
import java.util.concurrent.TimeUnit
import scala.util.Random
import casino.bloom.hash128

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = Array("-Xms2G", "-Xmx2G"))
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class MurmurHash3Benchmark:

    @Param(Array("16", "64", "256", "1024", "4096", "16384"))
    var size: Int = uninitialized

    var seed: Int = 42

    var data: Array[Byte] = uninitialized

    @Setup(Level.Trial)
    def setup(): Unit =
        val random = new Random(42) // Fixed seed for reproducibility
        data = new Array[Byte](size)
        random.nextBytes(data)

    @Benchmark
    def hash128Throughput(blackhole: Blackhole): Unit =
        // Benchmark focused on throughput with repeated hashing
        var i = 0
        while i < 100 do
            val result = hash128(data, seed + i)
            blackhole.consume(result)
            i += 1
