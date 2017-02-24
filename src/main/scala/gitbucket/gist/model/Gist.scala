package gitbucket.gist.model

trait GistComponent { self: gitbucket.core.model.Profile =>
  import profile.api._
  import self._

  lazy val Gists = TableQuery[Gists]

  class Gists(tag: Tag) extends Table[Gist](tag, "GIST") {
    val userName             = column[String]("USER_NAME")
    val repositoryName       = column[String]("REPOSITORY_NAME")
    val title                = column[String]("TITLE")
    val description          = column[String]("DESCRIPTION")
    val registeredDate       = column[java.util.Date]("REGISTERED_DATE")
    val updatedDate          = column[java.util.Date]("UPDATED_DATE")
    val originUserName       = column[String]("ORIGIN_USER_NAME")
    val originRepositoryName = column[String]("ORIGIN_REPOSITORY_NAME")
    val mode                 = column[String]("MODE")
    def * = (userName, repositoryName, title, description, registeredDate, updatedDate, originUserName.?, originRepositoryName.?, mode) <> (Gist.tupled, Gist.unapply)
  }
}

case class Gist(
  userName: String,
  repositoryName: String,
  title: String,
  description: String,
  registeredDate: java.util.Date,
  updatedDate: java.util.Date,
  originUserName: Option[String],
  originRepositoryName: Option[String],
  mode: String
){
  def toRepositoryInfo = {
    gitbucket.core.service.RepositoryService.RepositoryInfo(
      owner       = userName,
      name        = repositoryName,
      repository  = null,
      issueCount  = 0,
      pullCount   = 0,
      forkedCount = 0,
      branchList  = Nil,
      tags        = Nil,
      managers    = Nil
    )
  }
}



