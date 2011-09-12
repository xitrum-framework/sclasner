package sclasner

import java.io.File

class FileEntry(val container: File, val relPath: String, val bytesf: () => Array[Byte])
