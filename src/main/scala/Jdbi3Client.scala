import java.util.UUID

import com.zaxxer.hikari.HikariConfig
import org.jdbi.v3.core.Jdbi
import com.zaxxer.hikari.HikariDataSource
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.util.Random
import scala.collection.JavaConverters._
class Jdbi3Client(host: String, port: Int) extends TestClient {

  val config = new HikariConfig()
  config.setJdbcUrl(s"jdbc:postgresql://$host:$port/test")
  config.setUsername("test")
  config.addDataSourceProperty("cachePrepStmts", "true")
  config.addDataSourceProperty("prepStmtCacheSize", "250")
  config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
  config.setDriverClassName("org.postgresql.Driver")
  config.setLeakDetectionThreshold(0)
  config.setMaximumPoolSize(15)
  config.setPoolName("hikariSource")
  config.setReadOnly(false)

  val ds = new HikariDataSource(config)
  private val jdbi: Jdbi = Jdbi.create(ds)

  private val tableName = "jdbitest"
  private def async[T](f: => T): Future[T] = {
    Future(blocking(f))
  }

  def createTable: Future[Unit] = async {
    jdbi.useHandle { handle =>
      handle.execute(s"""
          |CREATE TABLE IF NOT EXISTS test.$tableName(
          | id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
          | long_string STRING
          |);""".stripMargin)
    }
  }

  def insertItem(): Future[Unit] = async {
    jdbi.useHandle { handle =>
      handle
        .createUpdate(s"insert into test.$tableName(long_string) values (?);")
        .bind(0, Random.alphanumeric.take(18).mkString)
        .execute()
    }
  }

  def selectItem(id: UUID): Future[Unit] = async {
    jdbi.withHandle { handle =>
      handle
        .createQuery(s"select * from test.$tableName where id = ?;")
        .bind(0, id)
        .mapToMap
        .findOnly()
    }
  }

  def selectBatch(size: Int, total: Int): Future[Seq[UUID]] = async {
    val from = Random.nextInt(total)
    jdbi.withHandle { handle =>
      handle
        .createQuery(s"select id from test.$tableName offset ? limit ?;")
        .bind(0, from)
        .bind(1, size)
        .mapTo(classOf[UUID])
        .list()
        .asScala
    }
  }

  override def selectAll: Future[Seq[UUID]] = async {
    jdbi.withHandle { handle =>
      handle
        .createQuery(
          s"select id from test.$tableName ORDER BY gen_random_uuid();")
        .mapTo(classOf[UUID])
        .list()
        .asScala
    }
  }

  def dropTable: Future[Unit] = async {
    jdbi.useHandle { handle =>
      handle.execute(s""" DROP TABLE IF EXISTS test.$tableName;""".stripMargin)
    }
  }

}
