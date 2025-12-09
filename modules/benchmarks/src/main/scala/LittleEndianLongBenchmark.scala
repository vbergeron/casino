package casino.bloom
package benchmarks

import org.openjdk.jmh.annotations.{*, given}
import org.openjdk.jmh.infra.Blackhole
import scala.compiletime.uninitialized

import java.util.concurrent.TimeUnit
import scala.util.Random

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = Array("-Xms2G", "-Xmx2G"))
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class LittleEndianLongBenchmark:

    @Param(Array("8", "64", "512", "4096"))
    var size: Int = uninitialized

    var data: Array[Byte]   = uninitialized
    var indices: Array[Int] = uninitialized

    @Setup(Level.Trial)
    def setup(): Unit =
        val random = new Random(42) // Fixed seed for reproducibility
        data = new Array[Byte](size)
        random.nextBytes(data)

        // Create valid indices (ensuring we don't go out of bounds for 8-byte reads)
        val maxIndex = math.max(0, size - 8)
        indices = (0 `until` 100).map(_ => random.nextInt(maxIndex + 1)).toArray

    @Benchmark
    def littleEndianLong(blackhole: Blackhole): Unit =
        var i = 0
        while i < indices.length do
            val result = getLittleEndianLong(data, indices(i))
            blackhole.consume(result)
            i += 1
