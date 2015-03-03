package gitbucket.gist.util

import gitbucket.core.controller.ControllerBase
import gitbucket.core.util.ControlUtil._
import gitbucket.core.util.Implicits._

/**
 * Allows only editor of the accessed snippet.
 */
trait GistEditorAuthenticator { self: ControllerBase =>
  protected def editorOnly(action: => Any) = { authenticate(action) }
  protected def editorOnly[T](action: T => Any) = (form: T) => { authenticate(action(form)) }

  private def authenticate(action: => Any) = {
    {
      defining(request.paths){ paths =>
        if(context.loginAccount.map { loginAccount =>
          loginAccount.isAdmin || loginAccount.userName == paths(1)
        }.getOrElse(false)){
          action
        } else {
          Unauthorized()
        }
      }
    }
  }
}