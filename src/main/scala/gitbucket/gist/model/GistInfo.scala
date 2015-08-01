package gitbucket.gist.model

case class GistInfo(fileName: String, source: String, fileCount: Int, forkedCount: Int, commentCount: Int)