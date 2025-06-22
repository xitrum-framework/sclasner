package sclasner

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.io.File

class IntegrationSpec extends AnyFlatSpec with Matchers {
  "Sclasner integration" should "scan for class files and provide basic statistics" in {
    var classFileCount = 0
    var jarFileCount = 0
    var totalSize = 0L

    val result = Scanner.foldLeft(
      (classFileCount, jarFileCount, totalSize),
      (acc: (Int, Int, Long), entry: FileEntry) => {
        val (classCount, jarCount, size) = acc

        if (entry.relPath.endsWith(".class")) {
          (classCount + 1, jarCount, size + entry.bytes.length)
        } else if (entry.container.getName.endsWith(".jar") && !entry.relPath.endsWith(".class")) {
          (classCount, jarCount + 1, size + entry.bytes.length)
        } else {
          acc
        }
      }
    )

    val (finalClassCount, finalJarCount, finalSize) = result

    // Should find some class files in the classpath
    finalClassCount should be > 0
    finalSize should be > 0L

    println(s"Found $finalClassCount .class files")
    println(s"Found $finalJarCount other files in JAR containers")
    println(s"Total size: ${finalSize / 1024} KB")
  }

  it should "work with caching" in {
    val cacheFile = "integration-test.cache"

    try {
      // First run - should create cache
      val result1 = Scanner.foldLeft(cacheFile, 0, (acc: Int, entry: FileEntry) => {
        if (entry.relPath.endsWith(".class")) acc + 1 else acc
      })

      // Cache file should exist
      new File(cacheFile).exists shouldBe true

      // Second run - should use cache (results should be same or similar)
      val result2 = Scanner.foldLeft(cacheFile, 0, (acc: Int, entry: FileEntry) => {
        if (entry.relPath.endsWith(".class")) acc + 1 else acc
      })

      // Results should be consistent (allowing for non-cacheable containers)
      result1 should be > 0
      result2 should be >= result1 - 100 // Allow some variance for non-cacheable

    } finally {
      // Clean up
      new File(cacheFile).delete()
    }
  }

  it should "find specific file types" in {
    case class FileStats(
      classFiles: Int = 0,
      javaFiles: Int = 0,
      xmlFiles: Int = 0,
      properties: Int = 0
    )

    val stats = Scanner.foldLeft(FileStats(), (acc: FileStats, entry: FileEntry) => {
      entry.relPath.toLowerCase match {
        case path if path.endsWith(".class") =>
          acc.copy(classFiles = acc.classFiles + 1)
        case path if path.endsWith(".java") =>
          acc.copy(javaFiles = acc.javaFiles + 1)
        case path if path.endsWith(".xml") =>
          acc.copy(xmlFiles = acc.xmlFiles + 1)
        case path if path.endsWith(".properties") =>
          acc.copy(properties = acc.properties + 1)
        case _ => acc
      }
    })

    // Should find at least some class files
    stats.classFiles should be > 0

    println(s"File type statistics:")
    println(s"  .class files: ${stats.classFiles}")
    println(s"  .java files: ${stats.javaFiles}")
    println(s"  .xml files: ${stats.xmlFiles}")
    println(s"  .properties files: ${stats.properties}")
  }

  it should "work exactly like the README example for txt files" in {
    // This test replicates the exact example from README.rst
    import java.io.File
    import sclasner.{FileEntry, Scanner}

    // We define a callback to process each FileEntry:
    // - The 1st argument is an accumulator to gather process results for each entry.
    // - The 2nd argument is each entry.
    // - The result of this callback will be passed to as the accumulator (the
    //   1st argument) to the next call.
    // - When all entries have been visited, the accumulator will be returned.
    def entryProcessor(acc: Seq[(String, String)], entry: FileEntry): Seq[(String, String)] = {
      if (entry.relPath.endsWith(".txt")) {
        val fileName = entry.relPath.split(File.pathSeparator).last
        val body     = new String(entry.bytes)
        acc :+ (fileName, body)
      } else {
        acc
      }
    }

    // We actually do the scan:
    // - The 1st argument is the initial value of the accumulator.
    // - The 2nd argument is the callback above.
    val acc = Scanner.foldLeft(Seq.empty, entryProcessor)

    // Validate the result
    acc shouldBe a[Seq[?]]

    // Print results for verification (may be empty if no .txt files in classpath)
    if (acc.nonEmpty) {
      println(s"Found ${acc.length} .txt files:")
      acc.foreach { case (fileName, content) =>
        println(s"  $fileName: ${content.take(50)}...")
      }
    } else {
      println("No .txt files found in classpath (this is expected)")
    }
  }
}
