package part3_highlevelserver

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{not => _, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.pattern.ask
import akka.testkit.TestProbe
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class RouteDSLFutureSpec extends AnyWordSpecLike with ScalatestRouteTest {

  implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(3 seconds)

  val replyActor: TestProbe = TestProbe("replyActor")
  val service = new DSLFutureService(replyActor.ref)

  "Test backend" should {
    "simple complete from future" in {
      // send an HTTP request through an endpoint that you want to test
      // inspect the response
      Post("/future") ~> service.route ~> check {
        // assertions
        status shouldBe StatusCodes.OK
      }
    }

    "simple complete from asked actor" in {

      val routeTestResult: RouteTestResult = Post("/ask") ~> service.route
      val request = replyActor.expectMsgType[String]
      replyActor.reply(s"++++ $request ++++")

      routeTestResult ~> check {
        status shouldBe StatusCodes.OK
      }
    }

  }
}

class DSLFutureService(asked: ActorRef) {

  val futureRoute: Route =
    (pathPrefix("future") & post) {
      entity(as[String]) { body =>
        // execution context for futures
        import scala.concurrent.ExecutionContext.Implicits.global
        val futureResponse: Future[String] = Future {
          // Simulate some processing
          Thread.sleep(100) // Simulating a delay
          s"Processed: $body"
        }
        complete(StatusCodes.OK, futureResponse)
      }
    }

  val actorRoute: Route =
    (pathPrefix("ask") & post) {
      entity(as[String]) { body =>
        // Here you would typically send a message to the actor
        // and handle the response asynchronously
        val result = asked.ask(body)(1 seconds).mapTo[String] // Sending the message to the actor
        complete(StatusCodes.OK, result)
      }
    }


  val route: Route = futureRoute ~ actorRoute
}
