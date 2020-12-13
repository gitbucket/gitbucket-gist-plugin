package gitbucket.gist.controller

import java.io.File
import gitbucket.core.view.helpers
import org.scalatra.forms._

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.AccountService
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.util._
import gitbucket.core.util.SyntaxSugars._
import gitbucket.core.util.Implicits._
import gitbucket.core.view.helpers._

import gitbucket.gist.model._
import gitbucket.gist.service._
import gitbucket.gist.util._
import gitbucket.gist.util.GistUtils._
import gitbucket.gist.util.Configurations._
import gitbucket.gist.html
import gitbucket.gist.js

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.scalatra.Ok
import play.twirl.api.Html
import play.twirl.api.JavaScript
import scala.util.Using

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
    val result = getVisibleGists((page - 1) * Limit, Limit, context.loginAccount)
    val count  = countPublicGists()

    val gists: Seq[(Gist, GistInfo)] = result.map { gist =>
      val userName = gist.userName
      val repoName = gist.repositoryName
      val files = getGistFiles(userName, repoName)
      val (fileName, source) = files.head

      (gist, GistInfo(fileName, getLines(fileName, source), files.length, getForkedCount(userName, repoName), getCommentCount(userName, repoName)))
    }

    html.list(None, gists, page, page * Limit < count)
  }

  get("/gist/:userName/:repoName"){
    _gist(params("userName"), Some(params("repoName")))
  }

  get("/gist/:userName/:repoName.js"){
    val userName = params("userName")
    val repoName = params("repoName")
    getGist(userName, repoName) match {
      case Some(gist) =>
        _embedJs(gist, userName, repoName, "master")
      case None =>
        NotFound()
    }
  }

  get("/gist/:userName/:repoName/:revision"){
    _gist(params("userName"), Some(params("repoName")), params("revision"))
  }

  get("/gist/:userName/:repoName/edit")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")
    val gitdir   = new File(GistRepoDir, userName + "/" + repoName)
    if(gitdir.exists){
      Using.resource(Git.open(gitdir)){ git =>
        val files: Seq[(String, JGitUtil.ContentInfo)] = JGitUtil.getFileList(git, "master", ".").map { file =>
          (if(isGistFile(file.name)) "" else file.name) -> JGitUtil.getContentInfo(git, file.name, file.id)
        }
        html.edit(getGist(userName, repoName), files, None)
      }
    }
  })

  post("/gist/_new")(usersOnly {
    val loginAccount = context.loginAccount.get
    val userName     = params.getOrElse("userName", loginAccount.userName)

    if(isEditable(userName, loginUserGroups)) {
      val files = getFileParameters()
      if(files.isEmpty){
        redirect(s"/gist")

      } else {
        val mode        = Mode.from(params("mode"))
        val description = params("description")

        // Create new repository
        val repoName = StringUtil.md5(userName + " " + datetime(new java.util.Date()))
        val gitdir   = new File(GistRepoDir, userName + "/" + repoName)
        gitdir.mkdirs()
        JGitUtil.initRepository(gitdir)

        // Insert record
        registerGist(
          userName,
          repoName,
          getTitle(files.head._1, repoName),
          description,
          mode
        )

        // Commit files
        Using.resource(Git.open(gitdir)){ git =>
          commitFiles(git, loginAccount, "Initial commit", files)
        }

        redirect(s"/gist/${userName}/${repoName}")
      }
    } else Unauthorized()
  })

  post("/gist/:userName/:repoName/edit")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    val loginAccount = context.loginAccount.get
    val files        = getFileParameters()
    val description  = params("description")
    val mode         = Mode.from(params("mode"))

    // Update database
    updateGist(
      userName,
      repoName,
      getTitle(files.head._1, repoName),
      description,
      mode
    )

    // Commit files
    val gitdir = new File(GistRepoDir, userName + "/" + repoName)
    Using.resource(Git.open(gitdir)){ git =>
      val commitId = commitFiles(git, loginAccount, "Update", files)

      // update refs
      val refUpdate = git.getRepository.updateRef(Constants.HEAD)
      refUpdate.setNewObjectId(commitId)
      refUpdate.setForceUpdate(false)
      refUpdate.setRefLogIdent(new org.eclipse.jgit.lib.PersonIdent(loginAccount.fullName, loginAccount.mailAddress))
      //refUpdate.setRefLogMessage("merged", true)
      refUpdate.update()
    }

    redirect(s"/gist/${userName}/${repoName}")
  })

  get("/gist/:userName/:repoName/delete")(editorOnly {
    val userName = params("userName")
    val repoName = params("repoName")

    if(isEditable(userName, loginUserGroups)){
      deleteGist(userName, repoName)

      val gitdir = new File(GistRepoDir, userName + "/" + repoName)
      org.apache.commons.io.FileUtils.deleteDirectory(gitdir)

      redirect(s"/gist/${userName}")
    }
  })

  get("/gist/:userName/:repoName/revisions"){
    val userName = params("userName")
    val repoName = params("repoName")
    val gitdir = new File(GistRepoDir, userName + "/" + repoName)

    Using.resource(Git.open(gitdir)){ git =>
      JGitUtil.getCommitLog(git, "master") match {
        case Right((revisions, hasNext)) => {
          val commits = revisions.map { revision =>
            defining(JGitUtil.getRevCommitFromId(git, git.getRepository.resolve(revision.id))){ revCommit =>
              (revision, JGitUtil.getDiffs(git, None, revision.id, true, false))
            }
          }

          val gist = getGist(userName, repoName).get
          val originUserName = gist.originUserName.getOrElse(userName)
          val originRepoName = gist.originRepositoryName.getOrElse(repoName)

          html.revisions(
            gist,
            getForkedCount(originUserName, originRepoName),
            GistRepositoryURL(gist, baseUrl, context.settings),
            isEditable(userName, loginUserGroups),
            commits
          )
        }
        case Left(_) => NotFound()
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
      Using.resource(Git.open(gitdir)){ git =>
        val gist = getGist(userName, repoName).get

        if(gist.mode == "PUBLIC" || context.loginAccount.exists(x => x.isAdmin || x.userName == userName)){
          JGitUtil.getFileList(git, revision, ".").find(_.name == fileName).map { file =>
            defining(JGitUtil.getContentFromId(git, file.id, false).get){ bytes =>
              RawData(FileUtil.getMimeType(file.name, bytes), bytes)
            }
          } getOrElse NotFound()
        } else Unauthorized()
      }
    } else NotFound()
  }

  get("/gist/:userName/:repoName/download/*"){
    val format = multiParams("splat").head match {
      case name if name.endsWith(".zip")    => "zip"
      case name if name.endsWith(".tar.gz") => "tar.gz"
    }

    val userName = params("userName")
    val repoName = params("repoName")

    Using.resource(Git.open(new File(GistRepoDir, userName + "/" + repoName))){ git =>
      val revCommit = JGitUtil.getRevCommitFromId(git, git.getRepository.resolve("master"))

      contentType = "application/octet-stream"
      response.setHeader("Content-Disposition", s"attachment; filename=${repoName}.${format}")
      response.setBufferSize(1024 * 1024);

      git.archive
        .setFormat(format)
        .setTree(revCommit.getTree)
        .setOutputStream(response.getOutputStream)
        .call()

      ()
    }
  }

  get("/gist/:userName/_profile"){
    val userName = params("userName")

    val result: (Seq[Gist], Int)  = (
      getUserGists(userName, context.loginAccount.map(_.userName), 0, Limit),
      countUserGists(userName, context.loginAccount.map(_.userName))
    )

    val createSnippet = context.loginAccount.exists { loginAccount =>
      loginAccount.userName == userName || getGroupsByUserName(loginAccount.userName).contains(userName)
    }

    getAccountByUserName(userName).map { account =>
      html.profile(
        account            = account,
        groupNames         = if(account.isGroupAccount) Nil else getGroupsByUserName(userName),
        extraMailAddresses = getAccountExtraMailAddresses(userName),
        gists              = result._1,
        createSnippet      = createSnippet
      )
    } getOrElse NotFound()
  }

  get("/gist/:userName"){
    _gist(params("userName"))
  }

  get("/gist/_new")(usersOnly {
    val userName = params.get("userName")

    if(isEditable(userName.getOrElse(context.loginAccount.get.userName), loginUserGroups)){
      html.edit(None, Seq(("", JGitUtil.ContentInfo("text", None, None, Some("UTF-8")))), userName)
    } else Unauthorized()
  })

  get("/gist/_add"){
    val count = params("count").toInt
    html.editor(count, "", JGitUtil.ContentInfo("text", None, None, Some("UTF-8")))
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

        registerGist(loginAccount.userName, repoName, gist.title, gist.description, Mode.from(gist.mode),
          Some(originUserName), Some(originRepoName))

        // Clone repository
        JGitUtil.cloneRepository(
          new File(GistRepoDir, userName + "/" + repoName),
          new File(GistRepoDir, loginAccount.userName + "/" + repoName)
        )

        redirect(s"/gist/${loginAccount.userName}/${repoName}")

      } getOrElse NotFound()
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
        isEditable(userName, loginUserGroups)
      )
    } getOrElse NotFound()
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
    helpers.markdown(
      markdown   = params("content"),
      repository = RepositoryInfo(
        owner          = userName,
        name           = repoName,
        repository     = null,
        issueCount     = 0,
        pullCount      = 0,
        forkedCount    = 0,
        milestoneCount = 0,
        branchList     = Nil,
        tags           = Nil,
        managers       = Nil
      ),
      branch           = "master",
      enableWikiLink   = false,
      enableRefsLink   = false,
      enableLineBreaks = false,
      enableAnchor     = false,
      enableTaskList   = true
    )
  }

  post("/gist/:userName/:repoName/_comment", commentForm)(usersOnly { form =>
    val userName = params("userName")
    val repoName = params("repoName")
    val loginAccount = context.loginAccount.get

    getGist(userName, repoName).map { gist =>
      registerGistComment(userName, repoName, form.content, loginAccount.userName)
      redirect(s"/gist/${userName}/${repoName}")
    } getOrElse NotFound()
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
          case t if t == "html" => gitbucket.gist.html.commentedit(gist, comment.content, comment.commentId)
        } getOrElse {
          contentType = formats("json")
          org.json4s.jackson.Serialization.write(
            Map("content" -> gitbucket.core.view.Markdown.toHtml(
              markdown         = comment.content,
              repository       = gist.toRepositoryInfo,
              branch           = "master",
              enableWikiLink   = false,
              enableRefsLink   = true,
              enableAnchor     = true,
              enableLineBreaks = true
            ))
          )
        }
      }
    } getOrElse NotFound()
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


  private def _gist(userName: String, repoName: Option[String] = None, revision: String = "master"): Any = {
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
          (gist, GistInfo(fileName, getLines(fileName, source), files.length, getForkedCount(userName, repoName), getCommentCount(userName, repoName)))
        }

        val fullName = getAccountByUserName(userName).get.fullName
        html.list(Some(GistUser(userName, fullName)), gists, page, page * Limit < result._2)
      }
      case Some(repoName) => {
        getGist(userName, repoName) match {
          case Some(gist) =>
            if(gist.mode == "PRIVATE"){
              context.loginAccount match {
                case Some(x) if(x.userName == userName) => _gistDetail(gist, userName, repoName, revision)
                case _ => Unauthorized()
              }
            } else {
              _gistDetail(gist, userName, repoName, revision)
            }
          case None =>
            NotFound()
        }
      }
    }
  }

  private def _embedJs(gist: Gist, userName: String, repoName: String, revision: String): JavaScript = {
    val originUserName = gist.originUserName.getOrElse(userName)
    val originRepoName = gist.originRepositoryName.getOrElse(repoName)

    js.detail(
      gist,
      GistRepositoryURL(gist, baseUrl, context.settings),
      revision,
      getGistFiles(userName, repoName, revision)
    )
  }

  private def _gistDetail(gist: Gist, userName: String, repoName: String, revision: String): Html = {
    val originUserName = gist.originUserName.getOrElse(userName)
    val originRepoName = gist.originRepositoryName.getOrElse(repoName)

    html.detail(
      gist,
      getForkedCount(originUserName, originRepoName),
      GistRepositoryURL(gist, baseUrl, context.settings),
      revision,
      getGistFiles(userName, repoName, revision),
      getGistComments(userName, repoName),
      isEditable(userName, loginUserGroups)
    )
  }

  private def getGistFiles(userName: String, repoName: String, revision: String = "master"): Seq[(String, String)] = {
    val gitdir = new File(GistRepoDir, userName + "/" + repoName)
    Using.resource(Git.open(gitdir)){ git =>
      JGitUtil.getFileList(git, revision, ".").map { file =>
        file.name -> StringUtil.convertFromByteArray(JGitUtil.getContentFromId(git, file.id, true).get)
      }
    }
  }

  private def getFileParameters(): Seq[(String, String)] = {
    val count = params("count").toInt
    (0 to count - 1).flatMap { i =>
      (params.get(s"fileName-${i}"), params.get(s"content-${i}")) match {
        case (Some(fileName), Some(content)) if(content.nonEmpty) => Some((if(fileName.isEmpty) s"gistfile${i + 1}.txt" else fileName, content))
        case _ => None
      }
    }
  }

  private def loginUserGroups: Seq[String] = {
    context.loginAccount.map { account =>
      getGroupsByUserName(account.userName)
    }.getOrElse(Nil)
  }

}
