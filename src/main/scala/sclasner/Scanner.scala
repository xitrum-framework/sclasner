package sclasner

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.collection.mutable.ListBuffer

object Scanner {
  def foldLeft[T](cacheFileName: String, acc: T, f: (T, FileEntry) => T): T = {
    // "target" is a directory in the current directory
    val targetPath = new File("target").getAbsolutePath

    val files = Discoverer.files
    val (subtargets, others) = files.partition { file =>
      val path = file.getAbsolutePath
      path.startsWith(targetPath)
    }

    val acc2 = foldLeft(others, cacheFileName, acc, f)
    foldLeft(subtargets, acc2, f)
  }

  def foldLeft[T](acc: T, f: (T, FileEntry) => T): T = {
    val files = Discoverer.files
    foldLeft(files, acc, f)
  }

  //----------------------------------------------------------------------------

  private def foldLeft[T](files: List[File], cacheFileName: String, acc: T, f: (T, FileEntry) => T): T = {
    val file = new File(cacheFileName)
    if (file.exists) {
      val fis  = new FileInputStream(file)
      val in   = new ObjectInputStream(fis)
      val acc2 = in.readObject.asInstanceOf[T]
      in.close
      acc2
    } else {
      val acc2 = foldLeft(files, acc, f)
      val fos  = new FileOutputStream(file)
      val out  = new ObjectOutputStream(fos)
      out.writeObject(acc2)
      out.close
      acc2
    }
  }

  private def foldLeft[T](files: List[File], acc: T, f: (T, FileEntry) => T): T = {
    files.foldLeft(acc) { (acc2, file) =>
      if (file.isDirectory) Loader.forDir(file, acc2, f) else Loader.forJar(file, acc2, f)
    }
  }
}
