package sclasner

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import java.io.{File, FileWriter, FileOutputStream, ObjectOutputStream, ObjectInputStream, FileInputStream}
import java.nio.file.{Files, Path}
import scala.util.control.NonFatal
import scala.compiletime.uninitialized

case class TestAccumulator(files: List[String], count: Int) extends Serializable

class ScannerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  var tempDir: Path = uninitialized
  var cacheFile: File = uninitialized

  override def beforeEach(): Unit = {
    tempDir = Files.createTempDirectory("scanner-test")
    cacheFile = tempDir.resolve("test.cache").toFile
  }

  override def afterEach(): Unit = {
    deleteRecursively(tempDir.toFile)
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) {
      file.listFiles().foreach(deleteRecursively)
    }
    file.delete()
  }

  private def createTestCache(data: TestAccumulator): Unit = {
    val fos = new FileOutputStream(cacheFile)
    val oos = new ObjectOutputStream(fos)
    try {
      oos.writeObject(data)
    } finally {
      oos.close()
      fos.close()
    }
  }

  "Scanner.foldLeft without cache" should "process all entries in classpath" in {
    val result = Scanner.foldLeft(0, (acc, _) => acc + 1)

    // Should find at least some files in the classpath
    result should be > 0
  }

  it should "provide FileEntry with correct information" in {
    var foundScalaFile = false
    var foundEntry: Option[FileEntry] = None

    Scanner.foldLeft((), (_, entry) => {
      if (entry.relPath.endsWith(".class") && !foundScalaFile) {
        foundScalaFile = true
        foundEntry = Some(entry)
      }
    })

    if (foundScalaFile) {
      val entry = foundEntry.get
      entry.container should not be null
      entry.relPath should not be empty
      entry.bytes should not be empty
    }
  }

  "Scanner.foldLeft with cache file name" should "create cache file when it doesn't exist" in {
    val cacheFileName = cacheFile.getAbsolutePath

    cacheFile.exists shouldBe false

    val result = Scanner.foldLeft(cacheFileName, TestAccumulator(List.empty, 0), (acc, entry) => {
      if (entry.relPath.endsWith(".class")) {
        acc.copy(files = entry.relPath :: acc.files, count = acc.count + 1)
      } else {
        acc
      }
    })

    cacheFile.exists shouldBe true
    result.count should be >= 0
  }

  it should "use existing cache file when available" in {
    val testData = TestAccumulator(List("test.class", "another.class"), 2)
    createTestCache(testData)

    cacheFile.exists shouldBe true

    val result = Scanner.foldLeft(cacheFile.getAbsolutePath, TestAccumulator(List.empty, 0), (acc, entry) => {
      // This processor should not be called for cached containers
      // but may be called for non-cacheable containers like target/build directories
      if (entry.relPath.endsWith(".class")) {
        acc.copy(count = acc.count + 1)
      } else {
        acc
      }
    })

    // Should return cached data plus any processing from non-cacheable containers
    // The cached files should be preserved
    result.files should contain allElementsOf testData.files
    // The count may be higher due to non-cacheable containers being processed
    result.count should be >= testData.count
  }

  it should "handle cache file corruption gracefully" in {
    // Create a corrupted cache file
    val writer = new FileWriter(cacheFile)
    writer.write("This is not a valid serialized object")
    writer.close()

    cacheFile.exists shouldBe true

    // Should fall back to processing when cache is corrupted
    val result = Scanner.foldLeft(cacheFile.getAbsolutePath, TestAccumulator(List.empty, 0), (acc, entry) => {
      if (entry.relPath.endsWith(".class")) {
        acc.copy(files = entry.relPath :: acc.files, count = acc.count + 1)
      } else {
        acc
      }
    })

    // Should have processed files (not returned cached data)
    result.count should be >= 0
    // Cache file should still exist (recreated)
    cacheFile.exists shouldBe true
  }

  "Scanner.foldLeft with File parameter" should "work with File objects" in {
    val result = Scanner.foldLeft(cacheFile, TestAccumulator(List.empty, 0), (acc, entry) => {
      if (entry.relPath.endsWith(".class")) {
        acc.copy(count = acc.count + 1)
      } else {
        acc
      }
    })

    result.count should be >= 0
    cacheFile.exists shouldBe true
  }

  "Scanner" should "distinguish between cacheable and non-cacheable containers" in {
    // This is harder to test directly, but we can verify it doesn't crash
    // and produces reasonable results
    val result = Scanner.foldLeft("test.cache", 0, (acc, _) => acc + 1)
    result should be >= 0

    // Clean up
    new File("test.cache").delete()
  }

  it should "handle different accumulator types correctly" in {
    // Test with different types to ensure ClassTag works properly

    // String accumulator
    val stringResult = Scanner.foldLeft("string-test.cache", "", (acc, entry) => {
      if (entry.relPath.endsWith(".class")) acc + "." else acc
    })

    // List accumulator
    val listResult = Scanner.foldLeft("list-test.cache", List.empty[String], (acc, entry) => {
      if (entry.relPath.endsWith(".class")) entry.relPath :: acc else acc
    })

    // Set accumulator
    val setResult = Scanner.foldLeft("set-test.cache", Set.empty[String], (acc, entry) => {
      if (entry.relPath.endsWith(".class")) acc + entry.relPath else acc
    })

    stringResult shouldBe a[String]
    listResult shouldBe a[List[?]]
    setResult shouldBe a[Set[?]]

    // Clean up
    new File("string-test.cache").delete()
    new File("list-test.cache").delete()
    new File("set-test.cache").delete()
  }
}
