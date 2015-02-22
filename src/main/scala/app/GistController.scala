package app

import java.io.File
import jp.sf.amateras.scalatra.forms._
import model.{GistUser, Gist, Account}
import service.{AccountService, GistService}
import util.Directory._
import util.{UsersAuthenticator, GistEditorAuthenticator, JGitUtil, StringUtil}
import util.ControlUtil._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.eclipse.jgit.dircache.DirCache
import util.Implicits._
import util.Configurations._

class GistController extends GistControllerBase with GistService with AccountService
  with GistEditorAuthenticator with UsersAuthenticator

trait GistControllerBase extends ControllerBase {
  self: GistService with AccountService with GistEditorAuthenticator with UsersAuthenticator =>

  get("/gist"){
    if(context.loginAccount.isDefined){
      val gists = getRecentGists(context.loginAccount.get.userName, 0, 4)
      gist.html.edit(gists, None, Seq(("", JGitUtil.ContentInfo("text", None, Some("UTF-8")))))(context)
    } else {
      val page = request.getParameter("page") match {
        case ""|null => 1
        case s => s.toInt
      }
      val result = getPublicGists((page - 1) * Limit, Limit)
      val count  = countPublicGists()

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

      gist.html.list(None, gists, page, page * Limit < count)
    }
  }

  get("/gist/:userName/:repoName/edit")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")
    val gitdir   = new File(GistRepoDir, userName + "/" + repoName)
    if(gitdir.exists){
      using(Git.open(gitdir)){ git =>
        val files: Seq[(String, JGitUtil.ContentInfo)] = JGitUtil.getFileList(git, "master", ".").map { file =>
          file.name -> JGitUtil.getContentInfo(git, file.name, file.id)
        }
        _root_.gist.html.edit(Nil, getGist(userName, repoName), files)
      }
    }
  })

  post("/gist/_new")(usersOnly {
    if(context.loginAccount.isDefined){
      val loginAccount = context.loginAccount.get
      val files = getFileParameters(true)

      if(files.isEmpty){
        redirect(s"/gist")

      } else {
        val isPrivate    = params("private").toBoolean
        val description  = params("description")

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
        )

        // Commit files
        using(Git.open(gitdir)){ git =>
          commitFiles(git, loginAccount, "Initial commit", files)
        }

        redirect(s"/gist/${loginAccount.userName}/${repoName}")
      }
    }
  })

  post("/gist/:userName/:repoName/edit")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    val loginAccount = context.loginAccount.get
    val files        = getFileParameters(true)
    // TODO Save isPrivate and description
    //val isPrivate    = params("private")
    val description  = params("description")
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

    redirect(s"${context.path}/gist/${loginAccount.userName}/${repoName}")
  })

  get("/gist/:userName/:repoName/delete")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    if(isEditable(userName)){
      deleteGist(userName, repoName)

      val gitdir = new File(GistRepoDir, userName + "/" + repoName)
      org.apache.commons.io.FileUtils.deleteDirectory(gitdir)

      redirect(s"${context.path}/gist/${userName}")
    }
  })

  get("/gist/:userName/:repoName/secret")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    if(isEditable(userName)){
      updateGistAccessibility(userName, repoName, true)
    }

    redirect(s"${context.path}/gist/${userName}/${repoName}")
  })

  get("/gist/:userName/:repoName/public")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    if(isEditable(userName)){
      updateGistAccessibility(userName, repoName, false)
    }

    redirect(s"${context.path}/gist/${userName}/${repoName}")
  })

  get("/gist/:userName/:repoName"){
    _gist(params("userName"), Some(params("repoName")))
  }

  get("/gist/:userName/:repoName/revisions"){
    val userName = params("userName")
    val repoName = params("repoName")
    val gitdir = new File(GistRepoDir, userName + "/" + repoName)

    using(Git.open(gitdir)){ git =>
      JGitUtil.getCommitLog(git, "master") match {
        case Right((revisions, hasNext)) => {
          val commits = revisions.map { revision =>
            defining(JGitUtil.getRevCommitFromId(git, git.getRepository.resolve(revision.id))){ revCommit =>
              JGitUtil.getDiffs(git, revision.id) match { case (diffs, oldCommitId) =>
                (revision, diffs)
              }
            }
          }
          gist.html.revisions("revision", getGist(userName, repoName).get, isEditable(userName), commits)
        }
        case Left(_) => NotFound
      }
    }
  }

  get("/gist/:userName"){
    _gist(params("userName"))
  }

  get("/gist/_add"){
    val count = params("count").toInt
    gist.html.editor(count, "", JGitUtil.ContentInfo("text", None, Some("UTF-8")))
  }

  private def _gist(userName: String, repoName: Option[String] = None) = {
    repoName match {
      case None => {
        val page = params.get("page") match {
          case Some("")|None => 1
          case Some(s) => s.toInt
        }

        val result: (Seq[Gist], Int)  = (
          getUserGists(userName, context.loginAccount.map(_.userName), (page - 1) * Limit, Limit),
          countUserGists(userName, context.loginAccount.map(_.userName))
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

        val fullName = getAccountByUserName(userName).get.fullName
        gist.html.list(Some(GistUser(userName, fullName)), gists, page, page * Limit < result._2)(context) // TODO Paging
      }
      case Some(repoName) => {
        val gitdir = new File(GistRepoDir, userName + "/" + repoName)
        if(gitdir.exists){
          using(Git.open(gitdir)){ git =>
            val gist = getGist(userName, repoName).get

            if(!gist.isPrivate || context.loginAccount.exists(x => x.isAdmin || x.userName == userName)){
              val files: Seq[(String, String)] = JGitUtil.getFileList(git, "master", ".").map { file =>
                file.name -> StringUtil.convertFromByteArray(JGitUtil.getContentFromId(git, file.id, true).get)
              }

              _root_.gist.html.detail("code", gist, files, isEditable(userName))
            } else {
              // TODO Permission Error
            }
          }
        }
      }
    }
  }

  private def isEditable(userName: String): Boolean = {
    context.loginAccount.map { loginAccount =>
      loginAccount.isAdmin || loginAccount.userName == userName
    }.getOrElse(false)
  }

  private def getFileParameters(flatten: Boolean): Seq[(String, String)] = {
    val count = params("count").toInt
    if(flatten){
      (0 to count - 1).flatMap { i =>
        (params.get(s"fileName-${i}"), params.get(s"content-${i}")) match {
          case (Some(fileName), Some(content)) if(content.nonEmpty) => Some((if(fileName.isEmpty) s"gistfile${i + 1}.txt" else fileName, content))
          case _ => None
        }
      }
    } else {
      (0 to count - 1).map { i =>
        val fileName = request.getParameter(s"fileName-${i}")
        val content  = request.getParameter(s"content-${i}")
        (if(fileName.isEmpty) s"gistfile${i + 1}.txt" else fileName, content)
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
