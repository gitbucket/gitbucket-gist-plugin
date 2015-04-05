import gitbucket.core.service.SystemSettingsService
import gitbucket.gist.controller.GistController
import gitbucket.core.plugin.PluginRegistry
import gitbucket.core.util.Version
import java.io.File
import javax.servlet.ServletContext
import gitbucket.gist.util.Configurations._

class Plugin extends gitbucket.core.plugin.Plugin with SystemSettingsService {
  override val pluginId: String = "gist"
  override val pluginName: String = "Gist Plugin"
  override val description: String = "Provides Gist feature on GitBucket."
  override val versions: List[Version] = List(Version(1, 0))

  override def initialize(registry: PluginRegistry, context: ServletContext): Unit = {
    // Add Snippet link to the header
    val settings = loadSystemSettings()
    val path = settings.baseUrl.getOrElse(context.getContextPath)
    registry.addJavaScript(".*",
      s"""
        |$$('a.brand').after(
        |  $$('<span style="float: left; margin-top: 10px;">|&nbsp;&nbsp;&nbsp;&nbsp;<a href="${path}/gist" style="color: black;">Snippet</a></span>')
        |);
      """.stripMargin)

    val rootdir = new File(GistRepoDir)
    if(!rootdir.exists){
      rootdir.mkdirs()
    }

    // Mount controller
    registry.addController(new GistController, "/*")

    registry.addImage("images/menu-revisions-active.png",
      getClass.getClassLoader.getResourceAsStream("images/menu-revisions-active.png"))

    registry.addImage("images/menu-revisions.png",
      getClass.getClassLoader.getResourceAsStream("images/menu-revisions.png"))

    registry.addImage("images/snippet.png",
      getClass.getClassLoader.getResourceAsStream("images/snippet.png"))

    println("-- Gist plug-in initialized --")
  }

  override def shutdown(registry: PluginRegistry, context: ServletContext): Unit = {
  }
}
