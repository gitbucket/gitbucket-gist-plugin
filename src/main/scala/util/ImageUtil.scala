package util

import ControlUtil._
import org.apache.commons.codec.binary.{StringUtils, Base64}

object ImageUtil {

  private val cache = scala.collection.concurrent.TrieMap[String, String]()

  def dataURI(path: String): String = {
    cache.getOrElseUpdate(path, {
      val bytes = using(getClass.getClassLoader.getResourceAsStream(path)){ in =>
        val bytes = new Array[Byte](in.available)
        in.read(bytes)
        bytes
      }
      val encoded = StringUtils.newStringUtf8(Base64.encodeBase64(bytes, false))
      s"data:image/png;base64,${encoded}"
    })
  }


}
