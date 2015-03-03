package gitbucket.gist.service

import gitbucket.gist.model.Gist
import gitbucket.gist.model.Profile._
import profile.simple._

trait GistService {

  def getRecentGists(userName: String, offset: Int, limit: Int)(implicit s: Session): List[Gist] =
    Gists.filter(_.userName === userName.bind).sortBy(_.registeredDate desc).drop(offset).take(limit).list

  def getPublicGists(offset: Int, limit: Int)(implicit s: Session): List[Gist] =
    Gists.filter(_.isPrivate === false.bind).sortBy(_.registeredDate desc).drop(offset).take(limit).list

  def countPublicGists()(implicit s: Session): Int =
    Query(Gists.filter(_.isPrivate === false.bind).length).first

  def getUserGists(userName: String, loginUserName: Option[String], offset: Int, limit: Int)(implicit s: Session): List[Gist] =
    (if(loginUserName.isDefined){
      Gists filter(t => (t.userName === userName.bind) && ((t.userName === loginUserName.bind) || (t.isPrivate === false.bind)))
    } else {
      Gists filter(t => (t.userName === userName.bind) && (t.isPrivate === false.bind))
    }).sortBy(_.registeredDate desc).drop(offset).take(limit).list


  def countUserGists(userName: String, loginUserName: Option[String])(implicit s: Session): Int =
    Query((if(loginUserName.isDefined){
      Gists.filter(t => (t.userName === userName.bind) && ((t.userName === loginUserName.bind) || (t.isPrivate === false.bind)))
    } else {
      Gists.filter(t => (t.userName === userName.bind) && (t.isPrivate === false.bind))
    }).length).first

  def getGist(userName: String, repositoryName: String)(implicit s: Session): Option[Gist] =
    Gists.filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)).firstOption

  def registerGist(userName: String, repositoryName: String, isPrivate: Boolean, title: String, description: String)(implicit s: Session): Unit =
    Gists.insert(Gist(userName, repositoryName, isPrivate, title, description, new java.util.Date(), new java.util.Date()))

  def updateGist(userName: String, repositoryName: String, title: String, description: String)(implicit s: Session): Unit =
    Gists
      .filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind))
      .map(t => (t.title, t.description, t.updatedDate))
      .update(title, description, new java.util.Date())

  def updateGistAccessibility(userName: String, repositoryName: String, isPrivate: Boolean)(implicit s: Session): Unit =
    Gists
      .filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind))
      .map(t => (t.isPrivate))
      .update(isPrivate)


  def deleteGist(userName: String, repositoryName: String)(implicit s: Session): Unit = {
    GistComments.filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)).delete
    Gists       .filter(t => (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)).delete
  }

}
