package model

case class Gist(
  userName: String,
  repositoryName: String,
  isPrivate: Boolean,
  title: String,
  description: String,
  registeredDate: String,
  updatedDate: String
)

object Gist {
  def apply(rs: java.sql.ResultSet): Gist =
    Gist(
      userName       = rs.getString("USER_NAME"),
      repositoryName = rs.getString("REPOSITORY_NAME"),
      isPrivate      = rs.getBoolean("PRIVATE"),
      title          = rs.getString("TITLE"),
      description    = rs.getString("DESCRIPTION"),
      registeredDate = rs.getString("REGISTERED_DATE"),
      updatedDate    = rs.getString("UPDATED_DATE")
    )
}
