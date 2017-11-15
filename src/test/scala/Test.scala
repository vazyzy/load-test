import io.gatling.core.Predef._

class Test extends Simulation {
  private val port = 26257
  private val host = "localhost"
  private val jdbiClient = new Jdbi3Client(host, port)
  private val jdbiScenario = LoadSuite.createScenario("Jdbi3Client", jdbiClient)
  jdbiClient.dropTable
  setUp(
    jdbiScenario.inject(constantUsersPerSec(300).during(10)),
  )
}
