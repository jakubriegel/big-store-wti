package eu.jrie.put.wti.bigstore.hub.routing

import java.net.http.HttpHeaders

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation.concat
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Router {

  private val config = ConfigFactory.load().getConfig("big-store.hub.publicApi")

  private def routes(companionsHosts: Seq[String], http: HttpExt)(implicit actorSystem: ActorSystem[Nothing]): Route = {
    concat(
      get {
        concat(companionGetRoute(companionsHosts, http):_*)
      }
    )
  }

  private def companionGetRoute(companionsHosts: Seq[String], http: HttpExt)(implicit actorSystem: ActorSystem[Nothing]) = {
    import akka.http.scaladsl.server.PathMatcher._
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    companionsHosts.zipWithIndex map { case (host, index) =>
      actorSystem.log.info(s"creating path for $host with id $index")
      path("user" / new ModuloMatcher(companionsHosts.length, index)) { id =>
        actorSystem.log.info(s"Routing GET user $id to companion $host")
        complete(
          Future {
            HttpRequest(
              uri = s"http://$host:60001/user/full/$id",
              headers = Seq(Accept(MediaTypes.`application/json`))
            )
          } .flatMap { http.singleRequest(_) }
        )
      }
    }
  }

  def run(companionsHosts: Seq[String]): ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>
    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    ctx.log.info(s"got $companionsHosts")
    val http = Http()
    http.bindAndHandle(
      routes(companionsHosts, http)(ctx.system),
      config.getString("host"), config.getInt("port")
    ).onComplete {
      case Success(bound) =>
        ctx.log.info(s"Public API online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
      case Failure(e) =>
        ctx.log.error(s"Public API could not start! $e")
        e.printStackTrace()
        ctx.self ! Done
    }

    Behaviors.receiveMessage {
      case Done => Behaviors.stopped
    }
  }, "routingSystem")
}
