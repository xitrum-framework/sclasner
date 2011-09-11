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
  import sclasner.Sclasner

  def f(container: File, relPath: String, bytesf: () => Array[Byte]): List[(String, String)] = {
    if (relPath.endsWith(".txt")) {
      val bytes    = bytesf()
      val fileName = relPath.split(File.pathSeparator).last
      val contents = new String(bytes)
      List((fileName, contents))
    } else {
      Nil
    }
  }

  val acc: List[(String, String)] = Sclasner.scan(f)

* ``scan`` will accummulate and return all results from ``f``
* ``f`` must return a list. The list may be empty (``Nil``)
* ``container`` may be a directory or a JAR file,
  you may call ``container.isDirectory`` or ``container.isFile`` to check
* ``relPath`` is path to the file you want to check, relative to ``container``
* ``bytesf`` returns contents of the file ``relPath`` points to.
  This function should be called at most one time in ``f``, the second call will
  return empty array.

Cache
-----

One scan may take 10-15 seconds, depending things in your classpath and your computer
spec etc. Fortunately, because things in classpath normally does not change frequently,
you may cache the result to a file and load it later.

You provide the cache file name to ``scan``:

::

  val acc = Sclasner.scan("txts.sclasner", f)

If txts.sclasner exists, ``f`` will not be run. Otherwise, ``f`` will be run and
the result will be serialized to txts.sclasner. If you want to force ``f`` to
run, just delete the cache file.

Note that the result of ``f`` must be serializable.

Cache in development mode
-------------------------

Suppose you are using SBT or Maven.

While developing, you normally do not want to cache the result of processing the
``target`` directory in the current working directory.

If ``container`` is a subdirectory of ``target``, Sclasner will not cache the
result of processing that ``container``. When loading the cache file, if a
``container`` is a subdirectory of ``target``, Sclasner also run ``f`` for that
``container``.

Use with SBT
------------

Scala versions: 2.9.1.

::

  libraryDependencies += "tv.cntt" %% "sclasner" % "1.0"

Or

::

  libraryDependencies += "tv.cntt" % "sclasner_2.9.1" % "1.0"

Sclasner is used in `Xitrum <https://github.com/ngocdaothanh/xitrum>`_.

