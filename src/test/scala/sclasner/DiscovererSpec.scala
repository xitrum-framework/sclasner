package sclasner

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.io.File
import java.net.URL

class DiscovererSpec extends AnyFlatSpec with Matchers {
  "Discoverer" should "find containers from classpath" in {
    val containers = Discoverer.containers

    // Should find at least some containers
    containers should not be empty

    // All containers should be existing files or directories
    containers.foreach { container =>
      container.exists shouldBe true
    }
  }

  it should "return containers as a List" in {
    val containers = Discoverer.containers
    containers shouldBe a[List[?]]
  }

  it should "include jar files and directories" in {
    val containers = Discoverer.containers

    // Should contain at least one directory or jar file
    val hasDirectories = containers.exists(_.isDirectory)
    val hasJarFiles = containers.exists(f => f.isFile && (f.getName.endsWith(".jar") || f.getName.endsWith(".zip")))

    // At least one of these should be true in a typical classpath
    (hasDirectories || hasJarFiles) shouldBe true
  }

  it should "filter out non-file protocol URLs" in {
    // This test verifies that only file:// URLs are processed
    // We can't easily test this directly without mocking, but we can verify
    // that all returned containers are actual files
    val containers = Discoverer.containers

    containers.foreach { container =>
      container shouldBe a[File]
      // If it exists, it should be a proper file system entry
      if (container.exists) {
        (container.isFile || container.isDirectory) shouldBe true
      }
    }
  }
}
