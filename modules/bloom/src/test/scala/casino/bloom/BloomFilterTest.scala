package casino.bloom

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.util.Random

class BloomFilterTest extends AnyFunSuite with Matchers {

    // Helper method to create a bloom filter with reasonable defaults
    def createBloomFilter(expectedItems: Long = 1000, falsePositiveRate: Double = 0.01, seed: Int = 42): BloomFilter = {
        BloomFilter.fromNP(expectedItems, falsePositiveRate, seed).create
    }

    // Helper method for creating filters with specific parameters for edge case testing
    def createBloomFilterRaw(bitsetSize: Int, k: Int, seed: Int): BloomFilter = {
        BloomFilter(Array.fill(bitsetSize)(0L), seed, k)
    }

    // Helper method to generate test data
    def generateTestData(size: Int): Array[Array[Byte]] = {
        val random = new Random(12345) // Fixed seed for reproducible tests
        Array.fill(size) {
            val bytes = new Array[Byte](8)
            random.nextBytes(bytes)
            bytes
        }
    }

    test("empty filter should not contain any items") {
        val filter    = createBloomFilter()
        val testItems = generateTestData(10)

        testItems.foreach { item =>
            filter.contains(item).shouldBe(false)
        }
    }

    test("added item should be found in filter") {
        val filter   = createBloomFilter()
        val testItem = "hello world".getBytes("UTF-8") // Item should not be present initially
        filter.contains(testItem).shouldBe(false)

        // Add item
        filter.add(testItem)

        // Item should now be present
        filter.contains(testItem).shouldBe(true)
    }

    test("multiple added items should all be found") {
        val filter    = createBloomFilter()
        val testItems = Array(
          "item1".getBytes("UTF-8"),
          "item2".getBytes("UTF-8"),
          "item3".getBytes("UTF-8"),
          "longer test string".getBytes("UTF-8")
        )

        // Add all items
        testItems.foreach(filter.add)

        // All items should be found
        testItems.foreach { item =>
            filter.contains(item).shouldBe(true)
        }
    }

    test("no false negatives - added items always return true") {
        val filter    = createBloomFilter(expectedItems = 500, falsePositiveRate = 0.01)
        val testItems = generateTestData(50)

        // Add items one by one and verify each is always found
        testItems.foreach { item =>
            filter.add(item)
            filter.contains(item).shouldBe(true)
        }

        // Verify all items are still found after all additions
        testItems.foreach { item =>
            filter.contains(item).shouldBe(true)
        }
    }

    test("false positives are possible but limited") {
        val filter        = createBloomFilter(expectedItems = 50, falsePositiveRate = 0.1) // Higher false positive rate
        val addedItems    = generateTestData(20)
        val notAddedItems = generateTestData(100)

        // Add first set of items
        addedItems.foreach(filter.add)

        // All added items must be found (no false negatives)
        addedItems.foreach { item =>
            filter.contains(item).shouldBe(true)
        }

        // Count false positives in items that were never added
        val falsePositives = notAddedItems.count(filter.contains)

        // Should have some false positives but not too many (less than 50% for this small filter)
        falsePositives should be > 0
        falsePositives should be < (notAddedItems.length / 2)
    }

    test("same item added multiple times behaves correctly") {
        val filter   = createBloomFilter()
        val testItem = "duplicate test".getBytes("UTF-8")

        // Add same item multiple times
        filter.add(testItem)
        filter.add(testItem)
        filter.add(testItem)

        // Should still be found
        filter.contains(testItem).shouldBe(true)
    }

    test("empty byte array handling") {
        val filter     = createBloomFilter()
        val emptyArray = Array.empty[Byte]

        filter.contains(emptyArray).shouldBe(false)
        filter.add(emptyArray)
        filter.contains(emptyArray).shouldBe(true)
    }

    test("single byte handling") {
        val filter     = createBloomFilter()
        val singleByte = Array[Byte](42)

        filter.contains(singleByte).shouldBe(false)
        filter.add(singleByte)
        filter.contains(singleByte).shouldBe(true)
    }

    test("large byte array handling") {
        val filter     = createBloomFilter()
        val largeArray = Array.fill[Byte](1000)(123)

        filter.contains(largeArray).shouldBe(false)
        filter.add(largeArray)
        filter.contains(largeArray).shouldBe(true)
    }

    test("different seeds produce different behavior") {
        val filter1  = createBloomFilter(seed = 1)
        val filter2  = createBloomFilter(seed = 2)
        val testItem = "seed test".getBytes("UTF-8")

        filter1.add(testItem)
        filter2.add(testItem)

        // Both should contain the item
        filter1.contains(testItem).shouldBe(true)
        filter2.contains(testItem).shouldBe(true)

        // But their internal bit patterns should be different
        filter1.bitset should not equal filter2.bitset
    }

    test("different hash parameters affect distribution") {
        val filter1  = createBloomFilterRaw(bitsetSize = 1000, k = 1, seed = 42)
        val filter2  = createBloomFilterRaw(bitsetSize = 1000, k = 5, seed = 42)
        val testItem = "k value test".getBytes("UTF-8")

        filter1.add(testItem)
        filter2.add(testItem)

        // Both should contain the item
        filter1.contains(testItem).shouldBe(true)
        filter2.contains(testItem).shouldBe(true)

        // Filter with higher k should have more bits set (generally)
        val bits1 = filter1.bitset.map(java.lang.Long.bitCount).sum
        val bits2 = filter2.bitset.map(java.lang.Long.bitCount).sum

        bits2 should be >= bits1
    }

    test("monotonicity - once positive, always positive") {
        val filter    = createBloomFilter()
        val testItems = generateTestData(20)

        testItems.zipWithIndex.foreach { case (item, index) =>
            // Add items one by one
            filter.add(item)

            // All previously added items should still be found
            testItems.take(index + 1).foreach { previousItem =>
                filter.contains(previousItem).shouldBe(true)
            }
        }
    }

}
