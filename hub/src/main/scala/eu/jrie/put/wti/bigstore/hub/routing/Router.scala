package eu.jrie.put.wti.bigstore.hub.routing

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation.concat
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Router {
  private def routes(companionsHosts: Seq[String])(implicit actorSystem: ActorSystem[_]): Route = {

    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    import akka.http.scaladsl.server.PathMatcher._

    val getPaths = companionsHosts.zipWithIndex
      .map { case (host, index) => (host, index+1) }
      .map { case (host, id) =>
        path("user" / "" ~ new ModuloMatcher(companionsHosts.length, id)) { id =>
          complete(
            Future { s"""{"id": $id,"companionId": $id,"companionHost": "$host"}""" }
              .map { HttpEntity(ContentTypes.`application/json`, _) }
              .map { HttpResponse(StatusCodes.OK, Seq.empty, _) }
          )
        }
      }

    get {
      concat(getPaths:_*)
    }
  }

  private val config = ConfigFactory.load().getConfig("big-store.hub")

  def run(companionsHosts: Seq[String]): ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>

    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    Http().bindAndHandle(
      routes(companionsHosts)(ctx.system),
      config.getString("host"), config.getInt("port")
    ).onComplete {
      case Success(bound) =>
        ctx.log.info(s"Api online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
      case Failure(e) =>
        Console.err.println(s"Server could not start!")
        e.printStackTrace()
        ctx.self ! Done
    }

    Behaviors.receiveMessage {
      case Done => Behaviors.stopped
    }
  }, "routingSystem")
}
