package sclasner

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.io.File

class FileEntrySpec extends AnyFlatSpec with Matchers {
  "FileEntry" should "lazily load bytes when accessed" in {
    val testContent = "Hello, World!".getBytes("UTF-8")
    var bytesLoaded = false

    val bytesFunction = () => {
      bytesLoaded = true
      testContent
    }

    val container = new File("/test/container")
    val fileEntry = new FileEntry(container, "test.txt", bytesFunction)

    // Bytes should not be loaded yet
    bytesLoaded shouldBe false

    // Now access bytes
    val loadedBytes = fileEntry.bytes

    // Bytes should now be loaded
    bytesLoaded shouldBe true
    loadedBytes shouldBe testContent
  }

  it should "store container and relative path correctly" in {
    val container = new File("/test/container")
    val relPath = "some/nested/file.txt"
    val bytesFunction = () => Array[Byte]()

    val fileEntry = new FileEntry(container, relPath, bytesFunction)

    fileEntry.container shouldBe container
    fileEntry.relPath shouldBe relPath
  }

  it should "cache bytes after first access" in {
    var callCount = 0
    val testContent = "Test content".getBytes("UTF-8")

    val bytesFunction = () => {
      callCount += 1
      testContent
    }

    val container = new File("/test/container")
    val fileEntry = new FileEntry(container, "test.txt", bytesFunction)

    // First access
    val bytes1 = fileEntry.bytes
    callCount shouldBe 1

    // Second access should use cached value
    val bytes2 = fileEntry.bytes
    callCount shouldBe 1 // Should still be 1

    bytes1 shouldBe bytes2
    bytes1 shouldBe testContent
  }
}
