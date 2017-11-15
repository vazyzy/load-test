import java.util.UUID

import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.Predef._
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.{ScenarioBuilder, ScenarioContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object LoadSuite extends Simulation {
  def createScenario(name: String, client: TestClient): ScenarioBuilder = {
    scenario(name)
      .exec(createTable(client))
      .exitHereIfFailed
      .repeat(1) {
        exec(insert(client))
      }
      .exec(selectAll(client))
      .foreach(s => s("items").as[Seq[UUID]].take(2), "item") {
        exec(select(client))
      }
      .repeat(1) {
        exec(selectBatch(client))
      }
  }

  def selectAll(client: TestClient): ActionBuilder = {
    futureActionBuilder(
      "selectAll",
      session => client.selectAll.map(items => session.set("items", items))
    )
  }

  def selectBatch(client: TestClient): ActionBuilder = {
    futureActionBuilder(
      "selectBatch",
      session => client.selectBatch(1, 1).map(_ => session)
    )
  }

  def select(client: TestClient): ActionBuilder = {
    futureActionBuilder(
      "select",
      session => {
        val item = session("item").as[UUID]
        client.selectItem(item).map(_ => session)
      }
    )
  }

  def createTable(client: TestClient): ActionBuilder = {
    futureActionBuilder(
      "createTable",
      session =>
        client.createTable.map(_ => {
          session
        })
    )
  }

  def insert(client: TestClient): ActionBuilder = {
    futureActionBuilder(
      "insert",
      session => client.insertItem().map(_ => session)
    )
  }
  def futureActionBuilder(actionName: String,
                          fn: Session => Future[Session]): ActionBuilder = {
    new ActionBuilder {
      override def build(ctx: ScenarioContext, nextAction: Action): Action = {
        new Action {
          override def name: String = actionName

          override def execute(session: Session): Unit = {
            val start = System.currentTimeMillis()
            fn(session)
              .andThen({
                  case Success(nextSession) =>
                    ctx.coreComponents.statsEngine.logResponse(
                      session = nextSession,
                      requestName = name,
                      timings =
                        ResponseTimings(start, System.currentTimeMillis()),
                      status = OK,
                      responseCode = None,
                      message = None
                    )
                    nextAction ! nextSession
                  case Failure(e) =>
                    ctx.coreComponents.statsEngine.logResponse(
                      session = session,
                      requestName = name,
                      timings =
                        ResponseTimings(start, System.currentTimeMillis()),
                      status = KO,
                      responseCode = None,
                      message = Some(e.getMessage)
                    )
                    nextAction ! session
                })
          }
        }
      }
    }
  }

}
