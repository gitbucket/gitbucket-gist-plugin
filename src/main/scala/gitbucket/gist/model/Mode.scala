package gitbucket.gist.model

sealed trait Mode {
  val code: String
}

object Mode {

  def from(code: String): Mode = {
    code match {
      case Public.code  => Public
      case Secret.code  => Public
      case Private.code => Public
    }
  }

  case object Public extends Mode {
    val code = "PUBLIC"
  }

  case object Secret extends Mode {
    val code = "SECRET"
  }

  case object Private extends Mode {
    val code = "PRIVATE"
  }

}