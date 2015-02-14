package app

import java.io.File
import model.{GistUser, Gist, Account}
import service.{AccountService, GistService}
import servlet.Database
import util.{JGitUtil, StringUtil}
import util.ControlUtil._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.eclipse.jgit.dircache.DirCache
import util.Configurations._
import plugin.Results._

import scala.slick.jdbc.JdbcBackend

object GistController extends GistService with AccountService {

  implicit def request2Session(request: HttpServletRequest): JdbcBackend#Session = Database.getSession(request)

  def list(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {

    if(context.loginAccount.isDefined){
      val gists = getRecentGists(context.loginAccount.get.userName, 0, 4)(Database.getSession(request)) // TODO Why implicit conversion does not work?
      gist.html.edit(gists, None, Seq(("", JGitUtil.ContentInfo("text", None, Some("UTF-8")))))(context)
    } else {
      val page = request.getParameter("page") match {
        case ""|null => 1
        case s => s.toInt
      }
      val result = getPublicGists((page - 1) * Limit, Limit)(Database.getSession(request))
      val count  = countPublicGists()(Database.getSession(request))

      val gists: Seq[(Gist, String)] = result.map { gist =>
        val gitdir = new File(GistRepoDir, gist.userName + "/" + gist.repositoryName)
        if(gitdir.exists){
          using(Git.open(gitdir)){ git =>
            val source: String = JGitUtil.getFileList(git, "master", ".").map { file =>
              StringUtil.convertFromByteArray(JGitUtil.getContentFromId(git, file.id, true).get).split("\n").take(9).mkString("\n")
            }.head

            (gist, source)
          }
        } else {
          (gist, "Repository is not found!")
        }
      }

      gist.html.list(None, gists, page, page * Limit < count)(context)
    }
  }

  def edit(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    val dim = request.getRequestURI.split("/")
    val userName = dim(2)
    val repoName = dim(3)

    if(isEditable(userName, context)){
      val gitdir = new File(GistRepoDir, userName + "/" + repoName)
      if(gitdir.exists){
        using(Git.open(gitdir)){ git =>
          val files: Seq[(String, JGitUtil.ContentInfo)] = JGitUtil.getFileList(git, "master", ".").map { file =>
            file.name -> JGitUtil.getContentInfo(git, file.name, file.id)
          }
          _root_.gist.html.edit(Nil, getGist(userName, repoName)(Database.getSession(request)), files)(context)
        }
      }
    } else {
      // TODO Permission Error
    }
  }

  def add(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    val count = request.getParameter("count").toInt
    Fragment(gist.html.editor(count, "", JGitUtil.ContentInfo("text", None, Some("UTF-8")))(context))
  }

  def _new(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    if(context.loginAccount.isDefined){
      val loginAccount = context.loginAccount.get
      val files        = getFileParameters(request, true)
      val isPrivate    = request.getParameter("private").toBoolean
      val description  = request.getParameter("description")

      // Create new repository
      val repoName = StringUtil.md5(loginAccount.userName + " " + view.helpers.datetime(new java.util.Date()))
      val gitdir   = new File(GistRepoDir, loginAccount.userName + "/" + repoName)
      gitdir.mkdirs()
      JGitUtil.initRepository(gitdir)

      // Insert record
      registerGist(
        loginAccount.userName,
        repoName,
        isPrivate,
        files.head._1,
        description
      )(Database.getSession(request))

      // Commit files
      using(Git.open(gitdir)){ git =>
        commitFiles(git, loginAccount, "Initial commit", files)
      }

      Redirect(s"/gist/${loginAccount.userName}/${repoName}")
    }
  }

  def _edit(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    val dim = request.getRequestURI.split("/")
    val userName = dim(2)
    val repoName = dim(3)

    if(isEditable(userName, context)){
      val loginAccount = context.loginAccount.get
      val files        = getFileParameters(request, true)
      val isPrivate    = request.getParameter("private")
      val description  = request.getParameter("description")
      val gitdir       = new File(GistRepoDir, userName + "/" + repoName)

      // Commit files
      using(Git.open(gitdir)){ git =>
        val commitId = commitFiles(git, loginAccount, "Update", files)

        // update refs
        val refUpdate = git.getRepository.updateRef(Constants.HEAD)
        refUpdate.setNewObjectId(commitId)
        refUpdate.setForceUpdate(false)
        refUpdate.setRefLogIdent(new org.eclipse.jgit.lib.PersonIdent(loginAccount.fullName, loginAccount.mailAddress))
        //refUpdate.setRefLogMessage("merged", true)
        refUpdate.update()
      }

      Redirect(s"${context.path}/gist/${loginAccount.userName}/${repoName}")
    } else {
      // TODO Permission Error
    }
  }

  def delete(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    val dim = request.getRequestURI.split("/")
    val userName = dim(2)
    val repoName = dim(3)

    if(isEditable(userName, context)){
      val loginAccount = context.loginAccount.get
      val gitdir = new File(GistRepoDir, userName + "/" + repoName)

      // TODO
//      val conn = getConnection(request)
//      conn.update("DELETE FROM GIST_COMMENT WHERE USER_NAME = ? AND REPOSITORY_NAME = ?", userName, repoName)
//      conn.update("DELETE FROM GIST WHERE USER_NAME = ? AND REPOSITORY_NAME = ?", userName, repoName)

      org.apache.commons.io.FileUtils.deleteDirectory(gitdir)

      Redirect(s"${context.path}/gist/${userName}")
    }
  }

  def secret(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    val dim = request.getRequestURI.split("/")
    val userName = dim(2)
    val repoName = dim(3)

    if(isEditable(userName, context)){
      updateGistAccessibility(userName, repoName, true)(Database.getSession(request))
    }

    Redirect(s"${context.path}/gist/${userName}/${repoName}")
  }

  def public(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    val dim = request.getRequestURI.split("/")
    val userName = dim(2)
    val repoName = dim(3)

    if(isEditable(userName, context)){
      updateGistAccessibility(userName, repoName, false)(Database.getSession(request))
    }

    Redirect(s"${context.path}/gist/${userName}/${repoName}")
  }

  def _gist(request: HttpServletRequest, response: HttpServletResponse, context: Context) = {
    val dim = request.getRequestURI.split("/")
    if(dim.length == 3){
      val userName = dim(2)

      val page = request.getParameter("page") match {
        case ""|null => 1
        case s => s.toInt
      }

      val result: (Seq[Gist], Int)  = (
        getUserGists(userName, context.loginAccount.map(_.userName), (page - 1) * Limit, Limit)(Database.getSession(request)),
        countUserGists(userName, context.loginAccount.map(_.userName))(Database.getSession(request))
      )

      val gists: Seq[(Gist, String)] = result._1.map { gist =>
        val repoName = gist.repositoryName
        val gitdir = new File(GistRepoDir, userName + "/" + repoName)
        if(gitdir.exists){
          using(Git.open(gitdir)){ git =>
            val source: String = JGitUtil.getFileList(git, "master", ".").map { file =>
              StringUtil.convertFromByteArray(JGitUtil.getContentFromId(git, file.id, true).get).split("\n").take(9).mkString("\n")
            }.head

            (gist, source)
          }
        } else {
          (gist, "Repository is not found!")
        }
      }

      val fullName = getAccountByUserName(userName)(Database.getSession(request)).get.fullName
      gist.html.list(Some(GistUser(userName, fullName)), gists, page, page * Limit < result._2)(context) // TODO Paging

    } else {
      val userName = dim(2)
      val repoName = dim(3)
      val gitdir = new File(GistRepoDir, userName + "/" + repoName)
      if(gitdir.exists){
        using(Git.open(gitdir)){ git =>
          val gist = getGist(userName, repoName)(Database.getSession(request)).get

          if(!gist.isPrivate || context.loginAccount.exists(x => x.isAdmin || x.userName == userName)){
            val files: Seq[(String, String)] = JGitUtil.getFileList(git, "master", ".").map { file =>
              file.name -> StringUtil.convertFromByteArray(JGitUtil.getContentFromId(git, file.id, true).get)
            }

            _root_.gist.html.detail("code", gist, files, isEditable(userName, context))(context)
          } else {
            // TODO Permission Error
          }
        }
      }
    }
  }

  private def isEditable(userName: String, context: app.Context): Boolean = {
    context.loginAccount.map { loginAccount =>
      loginAccount.isAdmin || loginAccount.userName == userName
    }.getOrElse(false)
  }

  private def getFileParameters(request: javax.servlet.http.HttpServletRequest, flatten: Boolean): Seq[(String, String)] = {
    val count = request.getParameter("count").toInt
    if(flatten){
      (0 to count - 1).flatMap { i =>
        val fileName = request.getParameter(s"fileName-${i}")
        val content  = request.getParameter(s"content-${i}")
        if(fileName.nonEmpty && content.nonEmpty){
          Some((fileName, content))
        } else {
          None
        }
      }
    } else {
      (0 to count - 1).map { i =>
        val fileName = request.getParameter(s"fileName-${i}")
        val content  = request.getParameter(s"content-${i}")
        if(fileName.nonEmpty && content.nonEmpty){
          (fileName, content)
        } else {
          ("", "")
        }
      }
    }
  }

  private def commitFiles(git: Git, loginAccount: Account, message: String, files: Seq[(String, String)]): ObjectId = {
    val builder  = DirCache.newInCore.builder()
    val inserter = git.getRepository.newObjectInserter()
    val headId   = git.getRepository.resolve(Constants.HEAD + "^{commit}")

    files.foreach { case (fileName, content) =>
      builder.add(JGitUtil.createDirCacheEntry(fileName, FileMode.REGULAR_FILE,
        inserter.insert(Constants.OBJ_BLOB, content.getBytes("UTF-8"))))
    }
    builder.finish()

    val commitId = JGitUtil.createNewCommit(git, inserter, headId, builder.getDirCache.writeTree(inserter),
      Constants.HEAD, loginAccount.fullName, loginAccount.mailAddress, message)

    inserter.flush()
    inserter.release()

    commitId
  }

}
