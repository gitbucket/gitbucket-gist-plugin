package gitbucket.gist.model

trait GistComponent { self: gitbucket.core.model.Profile =>
  import profile.simple._
  import self._

  lazy val Gists = TableQuery[Gists]

  class Gists(tag: Tag) extends Table[Gist](tag, "GIST") {
    val userName             = column[String]("USER_NAME")
    val repositoryName       = column[String]("REPOSITORY_NAME")
    val isPrivate            = column[Boolean]("PRIVATE")
    val title                = column[String]("TITLE")
    val description          = column[String]("DESCRIPTION")
    val registeredDate       = column[java.util.Date]("REGISTERED_DATE")
    val updatedDate          = column[java.util.Date]("UPDATED_DATE")
    val originUserName       = column[String]("ORIGIN_USER_NAME")
    val originRepositoryName = column[String]("ORIGIN_REPOSITORY_NAME")
    def * = (userName, repositoryName, isPrivate, title, description, registeredDate, updatedDate, originUserName.?, originRepositoryName.?) <> (Gist.tupled, Gist.unapply)
  }
}

case class Gist(
  userName: String,
  repositoryName: String,
  isPrivate: Boolean,
  title: String,
  description: String,
  registeredDate: java.util.Date,
  updatedDate: java.util.Date,
  originUserName: Option[String],
  originRepositoryName: Option[String]
)



