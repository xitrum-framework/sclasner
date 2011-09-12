package sclasner

import java.io.{File, FileInputStream, FileOutputStream, InputStream, ObjectInputStream, ObjectOutputStream}
import java.util.zip.ZipInputStream

import scala.collection.mutable.ListBuffer

object Sclasner {
  def foldLeft[T](cacheFileName: String, acc: T, f: (T, FileEntry) => T): T = {
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
    if (file.exists()) {
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
      if (file.isDirectory) forDir(file, acc2, f) else forJar(file, acc2, f)
    }
  }

  private def forDir[T](dir: File, acc: T, f: (T, FileEntry) => T): T = {
    forDir(dir, dir, acc, f)
  }

  private def forDir[T](container: File, dir: File, acc: T, f: (T, FileEntry) => T): T = {
    val files = dir.listFiles
    files.foldLeft(acc) { (acc2, fileOrDir) =>
      if (fileOrDir.isFile) {
        val file    = fileOrDir
        val relPath = file.getAbsolutePath.substring(container.getAbsolutePath.length + File.pathSeparator.length)
        val bytesf  = () => {
          val is = new FileInputStream(file)
          val bytes = readInputStream(is)
          is.close
          bytes
        }
        val fileEntry = new FileEntry(container, relPath, bytesf)
        f(acc2, fileEntry)
      } else {
        forDir(container, fileOrDir, acc2, f)
      }
    }
  }

  private def forJar[T](file: File, acc: T, f: (T, FileEntry) => T): T = {
    val zip  = new ZipInputStream(new FileInputStream(file))
    var acc2 = acc

    var entry = zip.getNextEntry
    while (entry != null) {
      if (!entry.isDirectory) {
    	val bytesf    = () => readInputStream(zip)
    	val fileEntry = new FileEntry(file, entry.getName, bytesf)
        acc2 = f(acc2, fileEntry)
      }

      zip.closeEntry
      entry = zip.getNextEntry
    }

    zip.close
    acc2
  }

  //----------------------------------------------------------------------------

  private val BUFFER_SIZE = 1024
  private def readInputStream(is: InputStream): Array[Byte] = {
    var ret = Array[Byte]()

    var buffer = new Array[Byte](BUFFER_SIZE)
    while (is.available > 0) {  // "available" is not always the exact size
      val bytesRead = is.read(buffer)
      ret = ret ++ buffer.take(bytesRead)
    }

    ret
  }
}
