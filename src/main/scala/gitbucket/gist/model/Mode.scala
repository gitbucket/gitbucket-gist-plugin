package gitbucket.gist.model

sealed trait Mode {
  val code: String
}

object Mode {

  def from(code: String): Mode = {
    code match {
      case Public.code  => Public
      case Secret.code  => Secret
      case Private.code => Private
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