import gitbucket.core.service.SystemSettingsService.SystemSettings
import gitbucket.gist.controller.GistController
import gitbucket.core.plugin.PluginRegistry
import gitbucket.core.util.Version
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

    // Add Snippet link to the header
    val path = settings.baseUrl.getOrElse(context.getContextPath)
    registry.addJavaScript(".*",
      s"""
        |$$('a.brand').after(
        |  $$('<span style="float: left; margin-top: 10px;">|&nbsp;&nbsp;&nbsp;&nbsp;<a href="${path}/gist" style="color: black;">Snippet</a></span>')
        |);
      """.stripMargin)

    // Create gist repository directory
    val rootdir = new File(GistRepoDir)
    if(!rootdir.exists){
      rootdir.mkdirs()
    }

    println("-- Gist plug-in initialized --")
  }

  override val controllers = Seq(
    "/*" -> new GistController()
  )

  override val images = Seq(
    "images/menu-revisions-active.png" -> fromClassPath("images/menu-revisions-active.png"),
    "images/menu-revisions.png"        -> fromClassPath("images/menu-revisions.png"),
    "images/snippet.png"               -> fromClassPath("images/snippet.png")
  )

  override def shutdown(registry: PluginRegistry, context: ServletContext, settings: SystemSettings): Unit = {
  }
}
