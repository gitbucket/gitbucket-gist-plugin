package model

object GistProfile extends {
  val profile = Profile.profile

} with GistComponent
  with GistCommentComponent
  with AccountComponent with Profile {
}
