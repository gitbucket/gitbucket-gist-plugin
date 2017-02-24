package gitbucket.gist.service

import scala.language.reflectiveCalls
import gitbucket.gist.model.GistComment
import gitbucket.gist.model.Profile._
import gitbucket.gist.model.Profile.profile.blockingApi._
import gitbucket.gist.model.Profile.dateColumnType

trait GistCommentService {

  def registerGistComment(userName: String, repositoryName: String, content: String, commentedUserName: String)
                         (implicit s: Session): Int =
    GistComments.autoInc insert GistComment(
      userName          = userName,
      repositoryName    = repositoryName,
      commentedUserName = commentedUserName,
      content           = content,
      registeredDate    = currentDate,
      updatedDate       = currentDate)

  def getGistComments(userName: String, repositoryName: String)(implicit s: Session): Seq[GistComment] =
    GistComments.filter { t =>
      (t.userName       === userName.bind) &&
      (t.repositoryName === repositoryName.bind)
    }.sortBy(_.registeredDate.desc).list

  def getGistComment(userName: String, repositoryName: String, commentId: Int)(implicit s: Session): Option[GistComment] =
    GistComments.filter { t =>
      (t.userName       === userName.bind) &&
      (t.repositoryName === repositoryName.bind) &&
      (t.commentId      === commentId.bind)
    }.firstOption

  def updateGistComment(userName: String, repositoryName: String, commentId: Int, content: String)(implicit s: Session): Int =
    GistComments.filter { t =>
      (t.userName       === userName.bind) &&
      (t.repositoryName === repositoryName.bind) &&
      (t.commentId      === commentId.bind)
    }.map { t =>
      (t.content, t.updatedDate)
    }.update(content, currentDate)

  def deleteGistComment(userName: String, repositoryName: String, commentId: Int)(implicit s: Session): Int =
    GistComments.filter { t =>
      (t.userName       === userName.bind) &&
      (t.repositoryName === repositoryName.bind) &&
      (t.commentId      === commentId.bind)
    }.delete

  def getCommentCount(userName: String, repositoryName: String)(implicit s: Session): Int =
    Query(GistComments.filter { t =>
      (t.userName       === userName.bind) &&
      (t.repositoryName === repositoryName.bind)
    }.length).first

}
