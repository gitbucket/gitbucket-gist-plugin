package model

trait GistComponent { self: Profile =>
  import profile.simple._
  import self._

  lazy val Gists = TableQuery[Gists]

  class Gists(tag: Tag) extends Table[Gist](tag, "GIST") {
    val userName       = column[String]("USER_NAME")
    val repositoryName = column[String]("REPOSITORY_NAME")
    val isPrivate      = column[Boolean]("PRIVATE")
    val title          = column[String]("TITLE")
    val description    = column[String]("DESCRIPTION")
    val registeredDate = column[java.util.Date]("REGISTERED_DATE")
    val updatedDate    = column[java.util.Date]("UPDATED_DATE")
    def * = (userName, repositoryName, isPrivate, title, description, registeredDate, updatedDate) <> (Gist.tupled, Gist.unapply)
  }
}

case class Gist(
  userName: String,
  repositoryName: String,
  isPrivate: Boolean,
  title: String,
  description: String,
  registeredDate: java.util.Date,
  updatedDate: java.util.Date
)

object GistProfile extends {
  val profile = Profile.profile

} with GistComponent
  with AccountComponent with Profile {
}



