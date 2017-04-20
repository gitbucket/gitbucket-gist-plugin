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

  def isEditable(userName: String)(implicit context: Context): Boolean = {
    context.loginAccount.map { loginAccount =>
      loginAccount.isAdmin || loginAccount.userName == userName
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

  def getLines(source: String): String = source.split("\n").take(10).mkString("\n")

  def isGistFile(fileName: String): Boolean = fileName.matches("gistfile[0-9]+\\.txt")

  def getTitle(fileName: String, repoName: String): String = if(isGistFile(fileName)) repoName else fileName

  case class GistRepositoryURL(gist: Gist, baseUrl: String, settings: SystemSettings){

    def httpUrl: String = s"${baseUrl}/git/gist/${gist.userName}/${gist.repositoryName}.git"
    
    def embedUrl: String = s"${baseUrl}/gist/${gist.userName}/${gist.repositoryName}.js"

    def sshUrl(loginUser: String): String = {
      val host = """^https?://(.+?)(:\d+)?/""".r.findFirstMatchIn(httpUrl).get.group(1)
      s"ssh://${loginUser}@${host}:${settings.sshPort.getOrElse(SystemSettingsService.DefaultSshPort)}/gist/${gist.userName}/${gist.repositoryName}.git"
    }

  }

}
