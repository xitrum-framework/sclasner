package sclasner

import java.io.File

class FileEntry(val container: File, val relPath: String, bytesf: () => Array[Byte]) {
  lazy val bytes = bytesf()
}
