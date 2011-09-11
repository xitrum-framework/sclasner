package sclasner

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.StringTokenizer

import scala.collection.mutable.{SetBuilder}

// See the source code of https://github.com/ngocdaothanh/annovention
object Discoverer {
  // Use lazy to avoid Null Pointer Exception

  /** Each URL should point to a directory or a .jar/.zip file. */
  lazy val urls = urlsForClassLoader ++ urlsForClasspath

  // See http://code.google.com/p/reflections/source/browse/trunk/reflections/src/main/java/org/reflections/util/ClasspathHelper.java?r=129
  lazy val urlsForClassLoader: Set[URL] = {
    val builder = new SetBuilder[URL, Set[URL]](Set())

    var loader = Thread.currentThread.getContextClassLoader
    while (loader != null) {
      if (loader.isInstanceOf[URLClassLoader]) {
        val urls = loader.asInstanceOf[URLClassLoader].getURLs
        builder ++= urls
      }
      loader = loader.getParent
    }

    builder.result
  }

  lazy val urlsForClasspath: Set[URL] = {
    val builder   = new SetBuilder[URL, Set[URL]](Set())
    val classpath = System.getProperty("java.class.path")
    val tokenizer = new StringTokenizer(classpath, File.pathSeparator)

    while (tokenizer.hasMoreTokens) {
      val path = tokenizer.nextToken
      val file = new File(path)
      builder += file.toURI.toURL
    }

    builder.result
  }
}
