import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import gitbucket.core.service.AccountService
import gitbucket.core.service.SystemSettingsService.SystemSettings
import gitbucket.gist.controller.GistController
import gitbucket.core.plugin._
import gitbucket.core.util.Version
import gitbucket.core.util.Implicits._
import java.io.File
import javax.servlet.ServletContext
import gitbucket.gist.util.Configurations._

class Plugin extends gitbucket.core.plugin.Plugin {

  override val pluginId: String = "gist"

  override val pluginName: String = "Gist Plugin"

  override val description: String = "Provides Gist feature on GitBucket."

  override val versions: List[Version] = List(
    Version(1, 2),
    Version(1, 0)
  )

  override def initialize(registry: PluginRegistry, context: ServletContext, settings: SystemSettings): Unit = {
    super.initialize(registry, context, settings)

    // Create gist repository directory
    val rootdir = new File(GistRepoDir)
    if(!rootdir.exists){
      rootdir.mkdirs()
    }

    println("-- Gist plug-in initialized --")
  }

  override val repositoryRoutings = Seq(
    GitRepositoryRouting("gist/(.+?)/(.+?)\\.git", "gist/$1/$2", new GistRepositoryFilter())
  )

  override val controllers = Seq(
    "/*" -> new GistController()
  )

  override val images = Seq(
    "images/menu-revisions-active.png" -> fromClassPath("images/menu-revisions-active.png"),
    "images/menu-revisions.png"        -> fromClassPath("images/menu-revisions.png"),
    "images/snippet.png"               -> fromClassPath("images/snippet.png")
  )

  override def javaScripts(registry: PluginRegistry, context: ServletContext, settings: SystemSettings): Seq[(String, String)] = {
    // Add Snippet link to the header
    val path = settings.baseUrl.getOrElse(context.getContextPath)
    Seq(
      ".*" -> s"""
        |$$('a.brand').after(
        |  $$('<span style="float: left; margin-top: 10px;">|&nbsp;&nbsp;&nbsp;&nbsp;<a href="${path}/gist" style="color: black;">Snippet</a></span>')
        |);
      """.stripMargin
    )
  }
}

class GistRepositoryFilter extends GitRepositoryFilter with AccountService {

  override def filter(request: HttpServletRequest, response: HttpServletResponse,
                      settings: SystemSettings, isUpdating: Boolean): Boolean = {
    implicit val r = request

    if(isUpdating){
      // Allow updating to self repository only
      val passed = for {
        auth <- Option(request.getHeader("Authorization"))
        Array(username, password) = decodeAuthHeader(auth).split(":", 2)
        account <- authenticate(settings, username, password)
      } yield {
        request.paths match {
          case Array(_, _, owner, _*) => owner == username
        }
      }

      passed getOrElse false
    } else true
  }

  // TODO This method shoud be provided by gitbucket-core
  private def decodeAuthHeader(header: String): String = {
    try {
      new String(new sun.misc.BASE64Decoder().decodeBuffer(header.substring(6)))
    } catch {
      case _: Throwable => ""
    }
  }

}
