import gitbucket.core.controller.Context
import gitbucket.core.model._
import gitbucket.core.service.AccountService
import gitbucket.core.service.SystemSettingsService.SystemSettings
import gitbucket.gist.controller.GistController
import gitbucket.core.plugin._
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version
import java.io.File
import javax.servlet.ServletContext
import gitbucket.gist.util.Configurations._

class Plugin extends gitbucket.core.plugin.Plugin {

  override val pluginId: String = "gist"

  override val pluginName: String = "Gist Plugin"

  override val description: String = "Provides Gist feature on GitBucket."

  override val versions: List[Version] = List(
    new Version("2.0.0", // This is mistake in 4.0.0 but it can't be fixed for migration.
      new LiquibaseMigration("update/gitbucket-gist_2.0.xml")
    ),
    new Version("4.2.0",
      new LiquibaseMigration("update/gitbucket-gist_4.2.xml")
    ),
    new Version("4.4.0"),
    new Version("4.5.0")
  )

  override def initialize(registry: PluginRegistry, context: ServletContext, settings: SystemSettings): Unit = {
    super.initialize(registry, context, settings)

    // Create gist repository directory
    val rootdir = new File(GistRepoDir)
    if(!rootdir.exists){
      rootdir.mkdirs()
    }

  }

  override val repositoryRoutings = Seq(
    GitRepositoryRouting("gist/(.+?)/(.+?)\\.git", "gist/$1/$2", new GistRepositoryFilter())
  )

  override val controllers = Seq(
    "/*" -> new GistController()
  )

  override val globalMenus = Seq(
    (context: Context) => Some(Link("snippets", "Snippets", "gist"))
  )
  override val profileTabs = Seq(
    (account: Account, context: Context) => Some(Link("snippets", "Snippets", s"gist/${account.userName}/_profile"))
  )
  override val assetsMappings = Seq("/gist" -> "/gitbucket/gist/assets")

//  override def javaScripts(registry: PluginRegistry, context: ServletContext, settings: SystemSettings): Seq[(String, String)] = {
//    // Add Snippet link to the header
//    val path = settings.baseUrl.getOrElse(context.getContextPath)
//    Seq(
//      ".*" -> s"""
//        |var accountName = $$('div.account-username').text();
//        |if(accountName != ''){
//        |  var active = location.href.endsWith('_profile');
//        |  $$('li:has(a:contains(Public Activity))').after(
//        |    $$('<li' + (active ? ' class="active"' : '') + '><a href="${path}/gist/' + accountName + '/_profile">Snippets</a></li>')
//        |  );
//        |}
//      """.stripMargin
//    )
//  }
}

class GistRepositoryFilter extends GitRepositoryFilter with AccountService {

  override def filter(path: String, userName: Option[String], settings: SystemSettings, isUpdating: Boolean)
                     (implicit session: Session): Boolean = {
    if(isUpdating){
      (for {
        userName <- userName
        account  <- getAccountByUserName(userName)
      } yield
        path.startsWith("/" + userName + "/") || account.isAdmin
      ).getOrElse(false)
    } else true
  }

}
