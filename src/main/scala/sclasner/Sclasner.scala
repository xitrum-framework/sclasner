package sclasner

import java.io.{File, FileInputStream, FileOutputStream, InputStream, ObjectInputStream, ObjectOutputStream}
import java.util.zip.ZipInputStream

import scala.collection.mutable.ListBuffer

object Sclasner {
  def scan[T](cacheFileName: String, f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    val targetPath = new File("target").getAbsolutePath

    val files = Discoverer.files
    val (subtargets, others) = files.partition { file =>
      val path = file.getAbsolutePath
      path.startsWith(targetPath)
    }

    val list1 = scan(others, cacheFileName, f)
    val list2 = scan(subtargets, f)
    list1 ++ list2
  }

  def scan[T](f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    val files = Discoverer.files
    scan(files, f)
  }

  //----------------------------------------------------------------------------

  private def scan[T](files: List[File], cacheFileName: String, f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    val file = new File(cacheFileName)
    if (file.exists()) {
      val fis  = new FileInputStream(file)
	  val in   = new ObjectInputStream(fis)
	  val list = in.readObject.asInstanceOf[List[T]]
      in.close

      list
    } else {
      val list = scan(files, f)
      val fos  = new FileOutputStream(file)
	  val out  = new ObjectOutputStream(fos)
	  out.writeObject(list)
	  out.close
      list
    }
  }

  private def scan[T](files: List[File], f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    files.foldLeft(List[T]()) { (acc, file) =>
      val list = if (file.isDirectory) forDir(file, f) else forJar(file, f)
      acc ++ list
    }
  }

  private def forDir[T](dir: File, f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    forDir(dir, dir, f)
  }

  private def forDir[T](container: File, dir: File, f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    val files = dir.listFiles
    files.foldLeft(List[T]()) { (acc, fileOrDir) =>
      if (fileOrDir.isFile) {
        val file = fileOrDir

        val bytesf = () => {
          val is = new FileInputStream(file)
          val bytes = readInputStream(is)
          is.close
          bytes
        }

        val relPath = file.getAbsolutePath.substring(container.getAbsolutePath.length + File.pathSeparator.length)
        acc ++ f(container, relPath, bytesf)
      } else {
        acc ++ forDir(container, fileOrDir, f)
      }
    }
  }

  private def forJar[T](file: File, f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    val zip    = new ZipInputStream(new FileInputStream(file))
    val buffer = ListBuffer[T]()

    var entry = zip.getNextEntry
    while (entry != null) {
      if (!entry.isDirectory) {
    	val bytesf = () => readInputStream(zip)
        val list = f(file, entry.getName, bytesf)
        buffer ++= list
      }

      zip.closeEntry
      entry = zip.getNextEntry
    }

    zip.close
    buffer.toList
  }

  //----------------------------------------------------------------------------

  val BUFFER_SIZE = 1024
  private def readInputStream(is: InputStream): Array[Byte] = {
    var ret = Array[Byte]()

    var buffer = new Array[Byte](BUFFER_SIZE)
    while (is.available > 0) {
      val bytesRead = is.read(buffer)
      ret = ret ++ buffer.take(bytesRead)
    }

    ret
  }
}
