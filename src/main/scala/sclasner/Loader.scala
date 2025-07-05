package sclasner

import java.io.{InputStream, File, FileInputStream}
import java.util.zip.ZipInputStream

object Loader {
  def forDir[T](dir: File, acc: T, f: (T, FileEntry) => T): T = {
    forDir(dir, dir, acc, f)
  }

  def forJar[T](file: File, acc: T, f: (T, FileEntry) => T): T = {
    val zip  = new ZipInputStream(new FileInputStream(file))
    var acc2 = acc

    var entry = zip.getNextEntry
    while (entry != null) {
      if (!entry.isDirectory) {
        val bytes     = readInputStream(zip)
        val bytesf    = () => bytes
        val fileEntry = new FileEntry(file, entry.getName, bytesf)
        acc2 = f(acc2, fileEntry)
      }

      zip.closeEntry()
      entry = zip.getNextEntry
    }

    zip.close()
    acc2
  }

  //----------------------------------------------------------------------------

  private def forDir[T](container: File, dir: File, acc: T, f: (T, FileEntry) => T): T = {
    val files = dir.listFiles
    files.foldLeft(acc) { (acc2, fileOrDir) =>
      if (fileOrDir.isFile) {
        val file    = fileOrDir
        val relPath = file.getAbsolutePath.substring(container.getAbsolutePath.length + File.pathSeparator.length)
        val bytesf  = () => {
          val is    = new FileInputStream(file)
          val bytes = readInputStream(is)
          is.close()
          bytes
        }
        val fileEntry = new FileEntry(container, relPath, bytesf)
        f(acc2, fileEntry)
      } else {
        forDir(container, fileOrDir, acc2, f)
      }
    }
  }

  private val BUFFER_SIZE = 1024
  private def readInputStream(is: InputStream): Array[Byte] = {
    var ret    = Array[Byte]()
    val buffer = new Array[Byte](BUFFER_SIZE)

    var bytesRead = is.read(buffer)
    while (bytesRead != -1) {
      ret = ret ++ buffer.take(bytesRead)
      bytesRead = is.read(buffer)
    }

    ret
  }
}
