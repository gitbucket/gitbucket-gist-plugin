package gitbucket.gist.util

import gitbucket.core.controller.Context
import gitbucket.core.model.Account
import gitbucket.core.service.SystemSettingsService
import gitbucket.core.service.SystemSettingsService.SystemSettings
import gitbucket.core.util.JGitUtil
import gitbucket.gist.model.Gist
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.lib.{FileMode, Constants, ObjectId}

object GistUtils {

  def isEditable(userName: String, groupNames: Seq[String])(implicit context: Context): Boolean = {
    context.loginAccount.map { loginAccount =>
      loginAccount.isAdmin || loginAccount.userName == userName || groupNames.contains(userName)
    }.getOrElse(false)
  }

  def commitFiles(git: Git, loginAccount: Account, message: String, files: Seq[(String, String)]): ObjectId = {
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
    inserter.close()

    commitId
  }

  def getLines(fileName: String, source: String): String = {
    val lines = source.split("\n").map(_.trim).take(10)

    (if((fileName.endsWith(".md") || fileName.endsWith(".markdown")) && lines.count(_ == "```") % 2 != 0) {
      lines :+ "```"
    } else {
      lines
    }).mkString("\n")
  }

  def isGistFile(fileName: String): Boolean = fileName.matches("gistfile[0-9]+\\.txt")

  def getTitle(fileName: String, repoName: String): String = if(isGistFile(fileName)) repoName else fileName

  case class GistRepositoryURL(gist: Gist, baseUrl: String, settings: SystemSettings){

    def httpUrl: String = s"${baseUrl}/git/gist/${gist.userName}/${gist.repositoryName}.git"

    def embedUrl: String = s"${baseUrl}/gist/${gist.userName}/${gist.repositoryName}.js"

    def sshUrl: Option[String] = {
      settings.sshUrl.map { sshUrl =>
        s"${sshUrl}/gist/${gist.userName}/${gist.repositoryName}.git"
      }
    }
  }

}
