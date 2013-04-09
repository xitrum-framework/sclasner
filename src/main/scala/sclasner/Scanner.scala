package sclasner

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.util.control.NonFatal

object Scanner {
  /** Cache file is tried to be deserialized. If failed, it is deleted and updated. */
  def foldLeft[T](cacheFileName: String, acc: T, f: (T, FileEntry) => T): T = {
    // "target" (SBT, Maven) and "build" (Gradle) are
    // directories in the current directory
    val targetPath = new File("target").getAbsolutePath
    val buildPath  = new File("build").getAbsolutePath

    val files = Discoverer.files
    val (subtargets, others) = files.partition { file =>
      val path = file.getAbsolutePath
      path.startsWith(targetPath) || path.startsWith(buildPath)
    }

    val acc2 = deserializeCacheFileWithFallback(others, cacheFileName, acc, f)
    doFoldLeft(subtargets, acc2, f)
  }

  def foldLeft[T](acc: T, f: (T, FileEntry) => T): T = {
    val files = Discoverer.files
    doFoldLeft(files, acc, f)
  }

  //----------------------------------------------------------------------------

  private def deserializeCacheFileWithFallback[T](files: Seq[File], cacheFileName: String, acc: T, f: (T, FileEntry) => T): T = {
    val cacheFile = new File(cacheFileName)
    if (cacheFile.exists) {
      try {
        deserialize[T](cacheFile)  // Bug: deserialize(cacheFile)
      } catch {
        case NonFatal(e) =>
          println("Could not deserialize " + cacheFileName)
          e.printStackTrace()

          println("Delete and update " + cacheFileName)
          cacheFile.delete()
          doFoldLeftAndSerialize(files, cacheFile, acc, f)
      }
    } else {
      doFoldLeftAndSerialize(files, cacheFile, acc, f)
    }
  }

  // This may throw exception because serialized classes are older than
  // the current version.
  private def deserialize[T](cacheFile: File): T = {
    val fis = new FileInputStream(cacheFile)
    val in  = new ObjectInputStream(fis)
    try {
      in.readObject.asInstanceOf[T]
    } finally {
      in.close
    }
  }

  private def doFoldLeftAndSerialize[T](files: Seq[File], cacheFile: File, acc: T, f: (T, FileEntry) => T): T = {
    val acc2 = doFoldLeft(files, acc, f)
    val fos  = new FileOutputStream(cacheFile)
    val out  = new ObjectOutputStream(fos)
    out.writeObject(acc2)
    out.close
    acc2
  }

  private def doFoldLeft[T](files: Seq[File], acc: T, f: (T, FileEntry) => T): T = {
    files.foldLeft(acc) { (acc2, file) =>
      if (file.isDirectory) Loader.forDir(file, acc2, f) else Loader.forJar(file, acc2, f)
    }
  }
}
