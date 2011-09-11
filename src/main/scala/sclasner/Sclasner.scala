package sclasner

import java.io.{File, FileInputStream, FileOutputStream, InputStream, ObjectInputStream, ObjectOutputStream}
import java.net.URLDecoder
import java.util.zip.ZipInputStream

import scala.collection.mutable.ListBuffer

object Sclasner {
  def scan[T](cacheFileName: String, f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    val file = new File(cacheFileName)
    if (file.exists()) {
      val fis  = new FileInputStream(file)
	  val in   = new ObjectInputStream(fis)
	  val list = in.readObject.asInstanceOf[List[T]]
      in.close
      list
    } else {
      val list = scan(f)
      val fos = new FileOutputStream(file)
	  val out = new ObjectOutputStream(fos)
	  out.writeObject(list)
	  out.close
      list
    }
  }

  def scan[T](f: (File, String, () => Array[Byte]) => List[T]): List[T] = {
    val urls   = Discoverer.urls.toList

    val buffer = ListBuffer[T]()
    urls.foreach { url =>
      if (url.getProtocol.equals("file")) {
        val filePath = URLDecoder.decode(url.getPath, "UTF-8")
        val file     = new File(filePath)
        if (file.exists) {
          val list = if (file.isDirectory) forDir(file, f) else forJar(file, f)
          buffer ++= list
        }
      }
    }
    buffer.toList
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
    val zip       = new ZipInputStream(new FileInputStream(file))
    val buffer    = ListBuffer[T]()

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

  private def readInputStream(is: InputStream): Array[Byte] = {
    val BUFFER_SIZE = 1024
    var ret = Array[Byte]()

    var buffer = new Array[Byte](BUFFER_SIZE)
    while (is.available > 0) {
      val bytesRead = is.read(buffer)
      ret = ret ++ buffer.take(bytesRead)
    }

    ret
  }
}
