import app.GistController
import plugin.PluginRegistry
import util.Version
import java.io.File
import util.Configurations._

class Plugin extends plugin.Plugin {
  override val pluginId: String = "gist"
  override val pluginName: String = "Gist Plugin"
  override val description: String = "Provides Gist feature on GitBucket."
  override val versions: List[Version] = List(Version(1, 0))

  override def initialize(registry: PluginRegistry): Unit = {
    // Add Snippet link to the header
    registry.addJavaScript(".*",
      """
        |$('a.brand').after($('<span style="float: left; margin-top: 10px;">|&nbsp;&nbsp;&nbsp;&nbsp;<a href="/gist" style="color: black;">Snippet</a></span>'));
      """.stripMargin)

    val rootdir = new File(GistRepoDir)
    if(!rootdir.exists){
      rootdir.mkdirs()
    }

    registry.addGlobalAction("GET" , "/gist"          )(GistController.list)
    registry.addGlobalAction("GET" , "/gist/.*/edit"  )(GistController.edit)
    registry.addGlobalAction("GET" , "/gist/_add"     )(GistController.add)
    registry.addGlobalAction("POST", "/gist/_new"     )(GistController._new)
    registry.addGlobalAction("POST", "/gist/.*/edit"  )(GistController._edit)
    registry.addGlobalAction("GET" , "/gist/.*/delete")(GistController.delete)
    registry.addGlobalAction("GET" , "/gist/.*/secret")(GistController.secret)
    registry.addGlobalAction("GET" , "/gist/.*/public")(GistController.public)
    registry.addGlobalAction("GET" , "/gist/.*"       )(GistController._gist)

    println("-- initialized --")
  }

  override def shutdown(registry: PluginRegistry): Unit = {
  }
}
