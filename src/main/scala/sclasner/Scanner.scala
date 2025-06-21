package sclasner

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.util.control.NonFatal
import scala.reflect.ClassTag

object Scanner {
  /** Cache file is tried to be deserialized. If failed, it is deleted and updated. */
  def foldLeft[T: ClassTag](cacheFileName: String, acc: T, entryProcessor: (T, FileEntry) => T): T = {
    val file = new File(cacheFileName)
    foldLeft(file, acc, entryProcessor)
  }

  /** Cache file is tried to be deserialized. If failed, it is deleted and updated. */
  def foldLeft[T: ClassTag](cacheFile: File, acc: T, entryProcessor: (T, FileEntry) => T): T = {
    // "target" (SBT, Maven) and "build" (Gradle) are
    // directories in the current directory
    val targetPath = new File("target").getAbsolutePath
    val buildPath  = new File("build").getAbsolutePath

    val containers = Discoverer.containers
    val (noncachables, cachables) = containers.partition { file =>
      val path = file.getAbsolutePath
      path.startsWith(targetPath) || path.startsWith(buildPath)
    }

    val acc2 = deserializeCacheFileWithFallback(cachables, cacheFile, acc, entryProcessor)
    doFoldLeft(noncachables, acc2, entryProcessor)
  }

  def foldLeft[T](acc: T, entryProcessor: (T, FileEntry) => T): T = {
    val containers = Discoverer.containers
    doFoldLeft(containers, acc, entryProcessor)
  }

  //----------------------------------------------------------------------------

  private def deserializeCacheFileWithFallback[T: ClassTag](
    containers:     Seq[File],
    cacheFile:      File,
    acc:            T,
    entryProcessor: (T, FileEntry) => T
  ): T = {
    if (cacheFile.exists) {
      try {
        deserialize[T](cacheFile)  // Bug: deserialize(cacheFile)
      } catch {
        case NonFatal(e) =>
          println("Could not deserialize " + cacheFile)
          e.printStackTrace()

          println("Delete and update " + cacheFile)
          cacheFile.delete()
          doFoldLeftAndSerialize(containers, cacheFile, acc, entryProcessor)
      }
    } else {
      doFoldLeftAndSerialize(containers, cacheFile, acc, entryProcessor)
    }
  }

  // This may throw exception because serialized classes are older than
  // the current version.
  private def deserialize[T: ClassTag](cacheFile: File): T = {
    val fis = new FileInputStream(cacheFile)
    val in  = new ObjectInputStream(fis)
    try {
      val ret = in.readObject.asInstanceOf[T]

      // Check compatibility
      val retClass      = ret.getClass
      val expectedClass = summon[ClassTag[T]].runtimeClass
      if (!expectedClass.isAssignableFrom(retClass))
        throw(new Exception("Expected: " + expectedClass + ", got: " + ret.getClass))

      ret
    } finally {
      in.close()
    }
  }

  private def doFoldLeftAndSerialize[T](
    containers:     Seq[File],
    cacheFile:      File,
    acc:            T,
    entryProcessor: (T, FileEntry) => T
  ): T = {
    val acc2 = doFoldLeft(containers, acc, entryProcessor)
    val fos  = new FileOutputStream(cacheFile)
    val out  = new ObjectOutputStream(fos)
    out.writeObject(acc2)
    out.close()
    acc2
  }

  private def doFoldLeft[T](
    containers:     Seq[File],
    acc:            T,
    entryProcessor: (T, FileEntry) => T
  ): T = {
    containers.foldLeft(acc) { (acc2, container) =>
      if (container.isDirectory)
        Loader.forDir(container, acc2, entryProcessor)
      else
        Loader.forJar(container, acc2, entryProcessor)
    }
  }
}
