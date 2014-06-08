package sclasner

import java.io.File
import java.net.{URL, URLClassLoader, URLDecoder}
import java.util.StringTokenizer

import scala.collection.mutable.{SetBuilder}

// See the source code of https://github.com/ngocdaothanh/annovention
object Discoverer {
  // Use lazy to avoid Null Pointer Exception

  /** Each File should point to a directory or a .jar/.zip file. */
  lazy val containers: List[File] = {
    // Convert to List to prevent careless bug because Set#map returns a Set
    val urls = (urlsForClassLoader ++ urlsForClasspath).toList

    urls.foldLeft(List[File]()) { (acc, url) =>
      if (url.getProtocol.equals("file")) {
        val filePath = URLDecoder.decode(url.getPath, "UTF-8")
        val file     = new File(filePath)
        if (file.exists) acc :+ file else acc
      } else {
        acc
      }
    }
  }

  //----------------------------------------------------------------------------

  // See http://code.google.com/p/reflections/source/browse/trunk/reflections/src/main/java/org/reflections/util/ClasspathHelper.java?r=129
  private lazy val urlsForClassLoader: Set[URL] = {
    val builder = new SetBuilder[URL, Set[URL]](Set.empty)
    var loader  = Thread.currentThread.getContextClassLoader

    while (loader != null) {
      if (loader.isInstanceOf[URLClassLoader]) {
        val urls = loader.asInstanceOf[URLClassLoader].getURLs
        builder ++= urls
      }
      loader = loader.getParent
    }

    builder.result
  }

  private lazy val urlsForClasspath: Set[URL] = {
    val builder   = new SetBuilder[URL, Set[URL]](Set.empty)
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
