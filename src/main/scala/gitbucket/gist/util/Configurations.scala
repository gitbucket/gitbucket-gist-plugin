package gitbucket.gist.util

object Configurations {
  lazy val GistRepoDir = s"${gitbucket.core.util.Directory.GitBucketHome}/gist"
  lazy val Limit = 10
}
