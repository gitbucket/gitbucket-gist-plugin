package gitbucket.gist.util

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.AccountService
import gitbucket.core.util.Implicits._

/**
 * Allows only editor of the accessed snippet.
 */
trait GistEditorAuthenticator { self: ControllerBase with AccountService =>
  protected def editorOnly(action: => Any) = { authenticate(action) }
  protected def editorOnly[T](action: T => Any) = (form: T) => { authenticate(action(form)) }

  private def authenticate(action: => Any) = {
    {
      val paths = request.paths
      if(context.loginAccount.map { loginAccount =>
        loginAccount.isAdmin || loginAccount.userName == paths(1) || getGroupsByUserName(loginAccount.userName).contains(paths(1))
      }.getOrElse(false)){
        action
      } else {
        Unauthorized()
      }
    }
  }
}