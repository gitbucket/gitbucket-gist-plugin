package gitbucket.gist.service

import gitbucket.core.model.Account
import gitbucket.gist.model.Gist
import gitbucket.gist.model.Mode
import gitbucket.gist.model.Profile._
import profile.simple._

trait GistService {

  def getRecentGists(userName: String, offset: Int, limit: Int)(implicit s: Session): Seq[Gist] =
    Gists.filter(_.userName === userName.bind).sortBy(_.registeredDate desc).drop(offset).take(limit).list

  def getVisibleGists(offset: Int, limit: Int, account: Option[Account])(implicit s: Session): Seq[Gist] = {
    val query = account.map { x =>
      Gists.filter { t => (t.mode === "PUBLIC".bind) || (t.userName === x.userName.bind) }
    } getOrElse {
      Gists.filter { t => (t.mode === "PUBLIC".bind) }
    }
    query.sortBy(_.registeredDate desc).drop(offset).take(limit).list
  }

  def countPublicGists()(implicit s: Session): Int =
    Query(Gists.filter(_.mode === "PUBLIC".bind).length).first

  def getUserGists(userName: String, loginUserName: Option[String], offset: Int, limit: Int)(implicit s: Session): Seq[Gist] =
    (if(loginUserName.isDefined){
      Gists filter(t => (t.userName === userName.bind) && ((t.userName === loginUserName.bind) || (t.mode === "PUBLIC".bind)))
    } else {
      Gists filter(t => (t.userName === userName.bind) && (t.mode === "PUBLIC".bind))
    }).sortBy(_.registeredDate desc).drop(offset).take(limit).list


  def countUserGists(userName: String, loginUserName: Option[String])(implicit s: Session): Int =
    Query((if(loginUserName.isDefined){
      Gists.filter(t => (t.userName === userName.bind) && ((t.userName === loginUserName.bind) || (t.mode === "PUBLIC".bind)))
    } else {
      Gists.filter(t => (t.userName === userName.bind) && (t.mode === "PUBLIC".bind))
    }).length).first

  def getGist(userName: String, repositoryName: String)(implicit s: Session): Option[Gist] =
    Gists.filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)).firstOption

  def getForkedCount(userName: String, repositoryName: String)(implicit s: Session): Int =
    Query(Gists.filter(t => (t.originUserName === userName.bind) && (t.originRepositoryName === repositoryName.bind)).length).first

  def getForkedGists(userName: String, repositoryName: String)(implicit s: Session): Seq[Gist] =
    Gists.filter(t => (t.originUserName === userName.bind) && (t.originRepositoryName === repositoryName.bind)).sortBy(_.userName).list

  def registerGist(userName: String, repositoryName: String, title: String, description: String, mode: Mode,
                   originUserName: Option[String] = None, originRepositoryName: Option[String] = None)(implicit s: Session): Unit =
    Gists.insert(Gist(userName, repositoryName, title, description, new java.util.Date(), new java.util.Date(),
      originUserName, originRepositoryName, mode.code))

  def updateGist(userName: String, repositoryName: String, title: String, description: String, mode: Mode)(implicit s: Session): Unit =
    Gists
      .filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind))
      .map(t => (t.title, t.description, t.updatedDate, t.mode))
      .update(title, description, new java.util.Date(), mode.code)

  def deleteGist(userName: String, repositoryName: String)(implicit s: Session): Unit = {
    GistComments.filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)).delete
    Gists       .filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)).delete
  }

}
