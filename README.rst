Sclasner is a classpath scanner written in Scala.

It is intended as a replacement of `Annovention <https://github.com/xitrum-framework/annovention>`_
and mainly used for standalone JVM applications. If you want a more complex solution,
please see `Reflections <http://code.google.com/p/reflections/>`_.

With Sclasner, you can:

* Scan all .class files (including those inside .jar files in classpath),
  then use
  `Javassist <http://www.javassist.org/>`_ or
  `ASM <http://asm.ow2.org/>`_
  to extract annotations
* Load all `.po files <https://github.com/xitrum-framework/scaposer>`_
* etc.

Scan
----

See `Scaladoc <http://xitrum-framework.github.io/sclasner/>`_.

For example, if you want to load all .txt files:

::

  import java.io.File
  import sclasner.{FileEntry, Scanner}

  // We define a callback to process each FileEntry:
  // - The 1st argument is an accumulator to gather process results for each entry.
  // - The 2nd argument is each entry.
  // - The result of this callback will be passed to as the accumulator (the
  //   1st argument) to the next call.
  // - When all entries have been visited, the accumulator will be returned.
  def entryProcessor(acc: Seq[(String, String)], entry: FileEntry): Seq[(String, String)] = {
    if (entry.relPath.endsWith(".txt")) {
      val fileName = entry.relPath.split(File.pathSeparator).last
      val body     = new String(entry.bytes)
      acc :+ (fileName, body)
    } else {
      acc
    }
  }

  // We actually do the scan:
  // - The 1st argument is the initial value of the accumulator.
  // - The 2nd argument is the callback above.
  val acc = Scanner.foldLeft(Seq.empty, entryProcessor)

Things in ``FileEntry``:

* ``container: File``, may be a directory or a JAR file in classpath.
  You may call ``container.isDirectory`` or ``container.isFile``.
  Inside each container, there may be multiple items, represented by the two
  below.
* ``relPath: String``, path to the file you want to check, relative to the
  ``container`` above.
* ``bytes: Array[Byte]``, body of the file the ``relPath`` above points to.
  This is a lazy val, accessing the first time will actually read the file from
  disk. Because reading from disk is slow, you should avoid accessing
  ``bytes`` if you don't have to.

Signature of ``Scanner.foldLeft``:

::

  foldLeft[T](acc: T, entryProcessor: (T, FileEntry) => T): T

Cache
-----

One scan may take 10-15 seconds, depending things in your classpath and your
computer spec etc. Fortunately, because things in classpath do not change
frequently, you may cache the result to a file and load it later.

You provide the cache file name to ``foldLeft``:

::

  val acc = Scanner.foldLeft("sclasner.cache", Seq.empty, entryProcessor)

If sclasner.cache exists, ``entryProcessor`` will not be run. Otherwise,
``entryProcessor`` will be run and the result will be serialized to the file.
If you want to force ``entryProcessor`` to run, just delete the cache file.

If the cache file cannot be successfully deserialized (for example, serialized
classes are older than the current version of the classes), it will be automatically
deleted and updated (``entryProcessor`` will be run).

For the result of ``entryProcessor`` to be written to file, it must be serializable.

Cache in development mode
-------------------------

Suppose you are using SBT, Maven, or Gradle.

While developing, you normally do not want to cache the result of processing
the directory ``target`` (SBT, Maven) or ``build`` (Gradle) in the current
working directory.

Sclasner's behavior:

* If ``container`` is a subdirectory of ``target`` or ``build``, the result of
  processing that ``container`` will not be cached.
* When loading the cache file, if a ``container`` is a subdirectory of
  ``target`` or ``build``, ``entryProcessor`` will be run for that ``container``.

Use with SBT
------------

Supported Scala versions: 2.11.x, 2.10.x

::

  libraryDependencies += "tv.cntt" %% "sclasner" % "1.6"

Sclasner is used in `Xitrum <https://github.com/xitrum-framework/xitrum>`_.
