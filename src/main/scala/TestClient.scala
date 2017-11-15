import java.util.UUID

import scala.concurrent.Future

trait TestClient {

  def createTable: Future[_]

  def insertItem(): Future[_]

  def selectItem(id: UUID): Future[_]

  def selectBatch(size: Int, total: Int): Future[Seq[UUID]]

  def selectAll: Future[Seq[UUID]]

  def dropTable: Future[_]
}
