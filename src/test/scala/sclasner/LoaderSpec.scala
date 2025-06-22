package sclasner

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import java.io.{File, FileOutputStream, FileWriter}
import java.util.zip.{ZipOutputStream, ZipEntry}
import java.nio.file.{Files, Path}
import scala.compiletime.uninitialized

class LoaderSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  var tempDir: Path = uninitialized
  var testDir: File = uninitialized
  var testJar: File = uninitialized

  override def beforeEach(): Unit = {
    tempDir = Files.createTempDirectory("sclasner-test")
    testDir = tempDir.resolve("testdir").toFile
    testDir.mkdirs()

    // Create test files in directory
    val file1 = new File(testDir, "file1.txt")
    val file2 = new File(testDir, "subdir/file2.txt")
    file2.getParentFile.mkdirs()

    writeToFile(file1, "Content of file1")
    writeToFile(file2, "Content of file2")

    // Create test JAR file
    testJar = tempDir.resolve("test.jar").toFile
    createTestJar(testJar)
  }

  override def afterEach(): Unit = {
    deleteRecursively(tempDir.toFile)
  }

  private def writeToFile(file: File, content: String): Unit = {
    val writer = new FileWriter(file)
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }

  private def createTestJar(jarFile: File): Unit = {
    val fos = new FileOutputStream(jarFile)
    val zos = new ZipOutputStream(fos)

    try {
      // Add first file
      val entry1 = new ZipEntry("jar-file1.txt")
      zos.putNextEntry(entry1)
      zos.write("JAR Content 1".getBytes("UTF-8"))
      zos.closeEntry()

      // Add second file in subdirectory
      val entry2 = new ZipEntry("subdir/jar-file2.txt")
      zos.putNextEntry(entry2)
      zos.write("JAR Content 2".getBytes("UTF-8"))
      zos.closeEntry()
    } finally {
      zos.close()
      fos.close()
    }
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory) {
      file.listFiles().foreach(deleteRecursively)
    }
    file.delete()
  }

  "Loader.forDir" should "process all files in a directory" in {
    val result = Loader.forDir(testDir, List.empty[String], (acc, entry) => {
      val content = new String(entry.bytes, "UTF-8")
      acc :+ s"${entry.relPath}:$content"
    })

    result should have size 2
    result should contain("file1.txt:Content of file1")
    result should contain(s"subdir${File.separator}file2.txt:Content of file2")
  }

  it should "provide correct container reference" in {
    Loader.forDir(testDir, (), (_, entry) => {
      entry.container shouldBe testDir
    })
  }

  it should "handle empty directories" in {
    val emptyDir = tempDir.resolve("empty").toFile
    emptyDir.mkdirs()

    val result = Loader.forDir(emptyDir, 0, (acc, _) => acc + 1)
    result shouldBe 0
  }

  "Loader.forJar" should "process all files in a JAR" in {
    val result = Loader.forJar(testJar, List.empty[String], (acc, entry) => {
      val content = new String(entry.bytes, "UTF-8")
      acc :+ s"${entry.relPath}:$content"
    })

    result should have size 2
    result should contain("jar-file1.txt:JAR Content 1")
    result should contain("subdir/jar-file2.txt:JAR Content 2")
  }

  it should "provide correct container reference for JAR files" in {
    Loader.forJar(testJar, (), (_, entry) => {
      entry.container shouldBe testJar
    })
  }

  it should "skip directories in JAR files" in {
    // Create a JAR with directory entries
    val jarWithDirs = tempDir.resolve("test-with-dirs.jar").toFile
    val fos = new FileOutputStream(jarWithDirs)
    val zos = new ZipOutputStream(fos)

    try {
      // Add directory entry
      val dirEntry = new ZipEntry("some-dir/")
      zos.putNextEntry(dirEntry)
      zos.closeEntry()

      // Add file entry
      val fileEntry = new ZipEntry("some-dir/file.txt")
      zos.putNextEntry(fileEntry)
      zos.write("File content".getBytes("UTF-8"))
      zos.closeEntry()
    } finally {
      zos.close()
      fos.close()
    }

    val result = Loader.forJar(jarWithDirs, 0, (acc, _) => acc + 1)
    result shouldBe 1 // Only the file, not the directory
  }

  "Loader" should "handle different accumulator types" in {
    // Test with String accumulator
    val stringResult = Loader.forDir(testDir, "", (acc, entry) => {
      acc + entry.relPath + ";"
    })
    stringResult should include("file1.txt;")

    // Test with Set accumulator
    val setResult = Loader.forDir(testDir, Set.empty[String], (acc, entry) => {
      acc + entry.relPath
    })
    setResult should contain("file1.txt")
    setResult should have size 2
  }
}
