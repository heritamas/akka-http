package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}

object HandlingExceptions extends App {

  implicit val system: ActorSystem = ActorSystem("HandlingExceptions")
  // implicit val materializer = ActorMaterializer() // needed only with Akka Streams < 2.6
  import system.dispatcher

  val simpleRoute =
    path("api" / "people") {
      get {
        // directive that throws some exception
        throw new RuntimeException("Getting all the people took too long")
      } ~
      post {
        parameter("id") { id =>
          if (id.length > 2)
            throw new NoSuchElementException(s"Parameter $id cannot be found in the database, TABLE FLIP!")

          complete(StatusCodes.OK)
        }
      }
    }

  implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      complete(StatusCodes.NotFound, e.getMessage)
    case e: IllegalArgumentException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }


//  Http()
//    .newServerAt("localhost", 8080)
//    .bind(Route.seal(simpleRoute))


  val runtimeExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      println("RuntimeException handled")
      complete(StatusCodes.NotFound, e.getMessage)
  }

  val noSuchElementExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: NoSuchElementException =>
      println("NoSuchElementException handled")
      complete(StatusCodes.BadRequest, e.getMessage)
  }

  val delicateHandleRoute =
    handleExceptions(runtimeExceptionHandler) {
      path("api" / "people") {
        get {
          // directive that throws some exception
          throw new RuntimeException("Getting all the people took too long")
        } ~
        handleExceptions(noSuchElementExceptionHandler) {
          post {
            parameter("id") { id =>
              if (id.length > 2)
                throw new NoSuchElementException(s"Parameter $id cannot be found in the database, TABLE FLIP!")

              complete(StatusCodes.OK)
            }
          }
        }
      }
    }

  Http()
    .newServerAt("localhost", 8080)
    .bind(Route.seal(delicateHandleRoute))


}
