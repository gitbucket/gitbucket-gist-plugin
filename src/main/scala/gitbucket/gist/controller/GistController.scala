package gitbucket.gist.controller

import java.io.File
import gitbucket.core.view.helpers
import io.github.gitbucket.scalatra.forms._

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.AccountService
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.util._
import gitbucket.core.util.Directory._
import gitbucket.core.util.ControlUtil._
import gitbucket.core.util.Implicits._
import gitbucket.core.view.helpers._

import gitbucket.gist.model._
import gitbucket.gist.service._
import gitbucket.gist.util._
import gitbucket.gist.util.GistUtils._
import gitbucket.gist.util.Configurations._
import gitbucket.gist.html

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.scalatra.Ok
import play.twirl.api.Html

class GistController extends GistControllerBase with GistService with GistCommentService with AccountService
  with GistEditorAuthenticator with UsersAuthenticator

trait GistControllerBase extends ControllerBase {
  self: GistService with GistCommentService with AccountService with GistEditorAuthenticator with UsersAuthenticator =>

  case class CommentForm(content: String)

  val commentForm = mapping(
    "content" -> trim(label("Comment", text(required)))
  )(CommentForm.apply)

  ////////////////////////////////////////////////////////////////////////////////
  //
  // Gist Actions
  //
  ////////////////////////////////////////////////////////////////////////////////

  get("/gist"){
    val page = request.getParameter("page") match {
      case ""|null => 1
      case s => s.toInt
    }
    val result = getVisibleGists((page - 1) * Limit, Limit, None)
    val count  = countPublicGists()

    val gists: Seq[(Gist, GistInfo)] = result.map { gist =>
      val userName = gist.userName
      val repoName = gist.repositoryName
      val files = getGistFiles(userName, repoName)
      val (fileName, source) = files.head

      (gist, GistInfo(fileName, getLines(source), files.length, getForkedCount(userName, repoName), getCommentCount(userName, repoName)))
    }

    html.list(None, gists, page, page * Limit < count)
  }

  get("/gist/:userName/:repoName"){
    _gist(params("userName"), Some(params("repoName")))
  }

  get("/gist/:userName/:repoName/:revision"){
    _gist(params("userName"), Some(params("repoName")), params("revision"))
  }

  get("/gist/:userName/:repoName/edit")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")
    val gitdir   = new File(GistRepoDir, userName + "/" + repoName)
    if(gitdir.exists){
      using(Git.open(gitdir)){ git =>
        val files: Seq[(String, JGitUtil.ContentInfo)] = JGitUtil.getFileList(git, "master", ".").map { file =>
          (if(isGistFile(file.name)) "" else file.name) -> JGitUtil.getContentInfo(git, file.name, file.id)
        }
        html.edit(Nil, getGist(userName, repoName), files)
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
        val repoName = StringUtil.md5(loginAccount.userName + " " + datetime(new java.util.Date()))
        val gitdir   = new File(GistRepoDir, loginAccount.userName + "/" + repoName)
        gitdir.mkdirs()
        JGitUtil.initRepository(gitdir)

        // Insert record
        registerGist(
          loginAccount.userName,
          repoName,
          isPrivate,
          getTitle(files.head._1, repoName),
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
    val description  = params("description")

    // Update database
    updateGist(
      userName,
      repoName,
      getTitle(files.head._1, repoName),
      description
    )

    // Commit files
    val gitdir = new File(GistRepoDir, userName + "/" + repoName)
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

    redirect(s"/gist/${loginAccount.userName}/${repoName}")
  })

  get("/gist/:userName/:repoName/delete")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    if(isEditable(userName)){
      deleteGist(userName, repoName)

      val gitdir = new File(GistRepoDir, userName + "/" + repoName)
      org.apache.commons.io.FileUtils.deleteDirectory(gitdir)

      redirect(s"/gist/${userName}")
    }
  })

  get("/gist/:userName/:repoName/secret")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    if(isEditable(userName)){
      updateGistAccessibility(userName, repoName, true)
    }

    redirect(s"/gist/${userName}/${repoName}")
  })

  get("/gist/:userName/:repoName/public")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    if(isEditable(userName)){
      updateGistAccessibility(userName, repoName, false)
    }

    redirect(s"/gist/${userName}/${repoName}")
  })

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

          val gist = getGist(userName, repoName).get
          val originUserName = gist.originUserName.getOrElse(userName)
          val originRepoName = gist.originRepositoryName.getOrElse(repoName)

          html.revisions(
            gist,
            getForkedCount(originUserName, originRepoName),
            GistRepositoryURL(gist, baseUrl, context.settings),
            isEditable(userName),
            commits
          )
        }
        case Left(_) => NotFound
      }
    }
  }

  get("/gist/:userName/:repoName/raw/:revision/:fileName"){
    val userName = params("userName")
    val repoName = params("repoName")
    val revision = params("revision")
    val fileName = params("fileName")
    val gitdir   = new File(GistRepoDir, userName + "/" + repoName)
    if(gitdir.exists){
      using(Git.open(gitdir)){ git =>
        val gist = getGist(userName, repoName).get

        if(!gist.isPrivate || context.loginAccount.exists(x => x.isAdmin || x.userName == userName)){
          JGitUtil.getFileList(git, revision, ".").find(_.name == fileName).map { file =>
            defining(JGitUtil.getContentFromId(git, file.id, false).get){ bytes =>
              RawData(FileUtil.getContentType(file.name, bytes), bytes)
            }
          } getOrElse NotFound
        } else Unauthorized
      }
    } else NotFound
  }

  get("/gist/:userName/:repoName/download/*"){
    val format = multiParams("splat").head match {
      case name if name.endsWith(".zip")    => "zip"
      case name if name.endsWith(".tar.gz") => "tar.gz"
    }

    val userName = params("userName")
    val repoName = params("repoName")

    val workDir = getDownloadWorkDir(userName, repoName, session.getId)
    if(workDir.exists) {
      FileUtils.deleteDirectory(workDir)
    }
    workDir.mkdirs

    using(Git.open(new File(GistRepoDir, userName + "/" + repoName))){ git =>
      val revCommit = JGitUtil.getRevCommitFromId(git, git.getRepository.resolve("master"))

      contentType = "application/octet-stream"
      response.setHeader("Content-Disposition", s"attachment; filename=${repoName}.${format}")
      response.setBufferSize(1024 * 1024);

      git.archive
        .setFormat(format)
        .setTree(revCommit.getTree)
        .setOutputStream(response.getOutputStream)
        .call()

      Unit
    }
  }

  get("/gist/:userName"){
    _gist(params("userName"))
  }

  get("/gist/_new")(usersOnly {
    val gists = getRecentGists(context.loginAccount.get.userName, 0, 4)
    html.edit(gists, None, Seq(("", JGitUtil.ContentInfo("text", None, Some("UTF-8")))))
  })

  get("/gist/_add"){
    val count = params("count").toInt
    html.editor(count, "", JGitUtil.ContentInfo("text", None, Some("UTF-8")))
  }

  ////////////////////////////////////////////////////////////////////////////////
  //
  // Fork Actions
  //
  ////////////////////////////////////////////////////////////////////////////////

  post("/gist/:userName/:repoName/fork")(usersOnly {
    val userName = params("userName")
    val repoName = params("repoName")
    val loginAccount = context.loginAccount.get

    if(getGist(loginAccount.userName, repoName).isDefined){
      redirect(s"/gist/${userName}/${repoName}")
    } else {
      getGist(userName, repoName).map { gist =>
        // Insert to the database at first
        val originUserName = gist.originUserName.getOrElse(gist.userName)
        val originRepoName = gist.originRepositoryName.getOrElse(gist.repositoryName)

        registerGist(loginAccount.userName, repoName, gist.isPrivate, gist.title, gist.description,
          Some(originUserName), Some(originRepoName))

        // Clone repository
        JGitUtil.cloneRepository(
          new File(GistRepoDir, userName + "/" + repoName),
          new File(GistRepoDir, loginAccount.userName + "/" + repoName)
        )

        redirect(s"/gist/${loginAccount.userName}/${repoName}")

      } getOrElse NotFound
    }
  })

  get("/gist/:userName/:repoName/forks"){
    val userName = params("userName")
    val repoName = params("repoName")

    getGist(userName, repoName).map { gist =>
      html.forks(
        gist,
        getForkedCount(userName, repoName),
        GistRepositoryURL(gist, baseUrl, context.settings),
        getForkedGists(userName, repoName),
        isEditable(userName)
      )
    } getOrElse NotFound
  }

  ////////////////////////////////////////////////////////////////////////////////
  //
  // Comment Actions
  //
  ////////////////////////////////////////////////////////////////////////////////

  post("/gist/:userName/:repoName/_preview"){
    val userName = params("userName")
    val repoName = params("repoName")

    contentType = "text/html"
    helpers.markdown(params("content"),
      RepositoryInfo(
        owner       = userName,
        name        = repoName,
        httpUrl     = "",
        repository  = null,
        issueCount  = 0,
        pullCount   = 0,
        commitCount = 0,
        forkedCount = 0,
        branchList  = Nil,
        tags        = Nil,
        managers    = Nil
      ), false, false, false, false)
  }

  post("/gist/:userName/:repoName/_comment", commentForm)(usersOnly { form =>
    val userName = params("userName")
    val repoName = params("repoName")
    val loginAccount = context.loginAccount.get

    getGist(userName, repoName).map { gist =>
      registerGistComment(userName, repoName, form.content, loginAccount.userName)
      redirect(s"/gist/${userName}/${repoName}")
    } getOrElse NotFound
  })

  ajaxPost("/gist/:userName/:repoName/_comments/:commentId/_delete")(usersOnly {
    val userName  = params("userName")
    val repoName  = params("repoName")
    val commentId = params("commentId").toInt

    // TODO Access check

    Ok(deleteGistComment(userName, repoName, commentId))
  })

  ajaxGet("/gist/:userName/:repoName/_comments/:commentId")(usersOnly {
    val userName  = params("userName")
    val repoName  = params("repoName")
    val commentId = params("commentId").toInt

    // TODO Access check
    getGist(userName, repoName).flatMap { gist =>
      getGistComment(userName, repoName, commentId).map { comment =>
        params.get("dataType") collect {
          case t if t == "html" => gitbucket.gist.html.commentedit(
            comment.content, comment.commentId, comment.userName, comment.repositoryName)
        } getOrElse {
          contentType = formats("json")
          org.json4s.jackson.Serialization.write(
            Map("content" -> gitbucket.core.view.Markdown.toHtml(comment.content, gist.toRepositoryInfo, false, true, true, true))
          )
        }
      }
    } getOrElse NotFound
  })

  ajaxPost("/gist/:userName/:repoName/_comments/:commentId/_update", commentForm)(usersOnly { form =>
    val userName  = params("userName")
    val repoName  = params("repoName")
    val commentId = params("commentId").toInt

    // TODO Access check

    updateGistComment(userName, repoName, commentId, form.content)
    redirect(s"/gist/${userName}/${repoName}/_comments/${commentId}")
  })

  ////////////////////////////////////////////////////////////////////////////////
  //
  // Private Methods
  //
  ////////////////////////////////////////////////////////////////////////////////

  private def _gist(userName: String, repoName: Option[String] = None, revision: String = "master"): Html = {
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

        val gists: Seq[(Gist, GistInfo)] = result._1.map { gist =>
          val repoName = gist.repositoryName
          val files = getGistFiles(userName, repoName, revision)
          val (fileName, source) = files.head
          (gist, GistInfo(fileName, getLines(source), files.length, getForkedCount(userName, repoName), getCommentCount(userName, repoName)))
        }

        val fullName = getAccountByUserName(userName).get.fullName
        html.list(Some(GistUser(userName, fullName)), gists, page, page * Limit < result._2)
      }
      case Some(repoName) => {
        val gist = getGist(userName, repoName).get
        val originUserName = gist.originUserName.getOrElse(userName)
        val originRepoName = gist.originRepositoryName.getOrElse(repoName)

        html.detail(
          gist,
          getForkedCount(originUserName, originRepoName),
          GistRepositoryURL(gist, baseUrl, context.settings),
          revision,
          getGistFiles(userName, repoName, revision),
          getGistComments(userName, repoName),
          isEditable(userName)
        )
      }
    }
  }

  private def getGistFiles(userName: String, repoName: String, revision: String = "master"): Seq[(String, String)] = {
    val gitdir = new File(GistRepoDir, userName + "/" + repoName)
    using(Git.open(gitdir)){ git =>
      JGitUtil.getFileList(git, revision, ".").map { file =>
        file.name -> StringUtil.convertFromByteArray(JGitUtil.getContentFromId(git, file.id, true).get)
      }
    }
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

}
