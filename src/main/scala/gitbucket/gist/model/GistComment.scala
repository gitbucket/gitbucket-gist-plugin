package gitbucket.gist.model

trait GistCommentComponent { self: gitbucket.core.model.Profile =>
  import profile.simple._
  import self._

  lazy val GistComments = TableQuery[GistComments]

  class GistComments(tag: Tag) extends Table[GistComment](tag, "GIST_COMMENT") {
    val userName          = column[String]("USER_NAME")
    val repositoryName    = column[String]("REPOSITORY_NAME")
    val commentId         = column[Int]("COMMENT_ID")
    val commentedUserName = column[String]("COMMENTED_USER_NAME")
    val content           = column[String]("DESCRIPTION")
    val registeredDate    = column[java.util.Date]("REGISTERED_DATE")
    val updatedDate       = column[java.util.Date]("UPDATED_DATE")
    def * = (userName, repositoryName, commentId, commentedUserName, content, registeredDate, updatedDate) <> (GistComment.tupled, GistComment.unapply)
  }
}

case class GistComment(
  userName: String,
  repositoryName: String,
  commentId: Int,
  commentedUserName: String,
  content: String,
  registeredDate: java.util.Date,
  updatedDate: java.util.Date
)
