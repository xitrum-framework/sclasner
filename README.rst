Sclasner is a classpath scanner written in Scala.

It is intended as a replacement of `Annovention <https://github.com/ngocdaothanh/annovention>`_
and mainly used for standalone JVM applications. If you want a more complex solution,
please see `Reflections <http://code.google.com/p/reflections/>`_.

With Sclasner, you can:

* Scan all .class files (including those inside .jar files in classpath),
  then use `Javassist <http://www.javassist.org/>`_ to extract annotations
* Load all `.po files <https://github.com/ngocdaothanh/scaposer>`_
* etc.

Scan
----

For example, if you want to load all .txt files:

::

  import java.io.File
  import sclasner.{FileEntry, Scanner}

  def f(acc: List[(String, String)], entry: FileEntry): List[(String, String)] = {
    if (entry.relPath.endsWith(".txt")) {
      val fileName = entry.relPath.split(File.pathSeparator).last
      val body     = new String(entry.bytes)
      acc :+ (fileName, body)
    } else {
      acc
    }
  }

  val acc = Scanner.foldLeft(List(), f)

Things in ``FileEntry``:

* ``container: File``, may be a directory or a JAR file in classpath.
  You may call ``container.isDirectory`` or ``container.isFile``.
* ``relPath: String``, path to the file you want to check, relative to ``container``.
* ``bytes: Array[Byte]``, body of the file ``relPath`` points to.
  This is a lazy val, accessing the first time will actually read the file from
  disk. But because reading from disk is slow, you should avoid accessing
  ``bytes`` if you don't have to.

``foldLeft`` will accummulate and return results from ``f``:

::

  foldLeft[T](acc: T, f: (T, FileEntry) => T): T

Cache
-----

One scan may take 10-15 seconds, depending things in your classpath and your computer
spec etc. Fortunately, because things in classpath do not change frequently,
you may cache the result to a file and load it later.

You provide the cache file name to ``foldLeft``:

::

  val acc = Scanner.foldLeft("txts.sclasner", f)

If txts.sclasner exists, ``f`` will not be run. Otherwise, ``f`` will be run and
the result will be serialized to txts.sclasner. If you want to force ``f`` to
run, just delete the cache file.

Note that the result of ``f`` must be serializable.

Cache in development mode
-------------------------

Suppose you are using SBT, Maven, or Gradle.

While developing, you normally do not want to cache the result of processing
the ``target`` (SBT, Maven) directory or ``build`` (Gradle) in the current
working directory:

* If ``container`` is a subdirectory of ``target`` or ``build``, the result of
  processing that ``container`` will not be cached.
* When loading the cache file, if a ``container`` is a subdirectory of
  ``target`` or ``build``, ``f`` will be run for that ``container``.

Use with SBT
------------

Supported Scala versions: 2.10.x

::

  libraryDependencies += "tv.cntt" %% "sclasner" % "1.2"

Sclasner is used in `Xitrum <https://github.com/ngocdaothanh/xitrum>`_.

