package eu.jrie.put.wti.bigstore.hub.routing

import java.net.http.HttpHeaders

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.server.Directives.{complete, delete, get, path, put, entity, as}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation.concat
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Router {

  private val config = ConfigFactory.load().getConfig("big-store")

  private def routes(companionsHosts: Seq[(String, Int)], companionPort: Int, http: HttpExt)(implicit actorSystem: ActorSystem[Nothing]): Route = {
    concat(
      companionGetRoutes(companionsHosts, companionPort, http),
      companionManageRoutes(companionsHosts, companionPort, http)
    )
  }

  private def companionGetRoutes(companionsHosts: Seq[(String, Int)], companionPort: Int, http: HttpExt)(implicit actorSystem: ActorSystem[Nothing]) = {
    import akka.http.scaladsl.server.PathMatcher._
    import akka.http.scaladsl.model._
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    get {
      concat(
        companionsHosts map { case (host, id) =>
          path("user" / new ModuloMatcher(companionsHosts.length, id - 1)) { userId =>
            actorSystem.log.info(s"Routing GET user $userId to companion ($host, $id)")
            complete(
              Future {
                HttpRequest(
                  uri = s"http://$host:$companionPort/user/$userId",
                  headers = Seq(Accept(MediaTypes.`application/json`))
                )
              }.flatMap {
                http.singleRequest(_)
              }
            )
          }
        }: _*
      )
    }
  }

  private def companionManageRoutes(companionsHosts: Seq[(String, Int)], companionPort: Int, http: HttpExt)(implicit actorSystem: ActorSystem[Nothing]) = {
    import akka.http.scaladsl.server.PathMatchers._
    import akka.http.scaladsl.server.PathMatcher._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.model.HttpMethods._
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    concat(
      put {
        concat(
          companionsHosts map { case (host, id) =>
            path("user" / new ModuloMatcher(companionsHosts.length, id - 1) / Remaining) { case (userId, resource) =>
              entity(as[String]) { payload =>
                actorSystem.log.info(s"Routing PUT /$resource user $userId to companion ($host, $id)")
                complete(
                  Future {
                    HttpRequest(
                      uri = s"http://$host:$companionPort/user/$userId/$resource",
                      method = PUT,
                      headers = Seq(Accept(MediaTypes.`application/json`)),
                      entity = HttpEntity(ContentTypes.`application/json`, payload)
                    )
                  }.flatMap {
                    http.singleRequest(_)
                  }
                )
              }
            }
          } :_*
        )
      },
      delete {
        concat(
          companionsHosts map { case (host, id) =>
            path("user" / new ModuloMatcher(companionsHosts.length, id - 1)) { userId =>
              actorSystem.log.info(s"Routing DELETE user $userId to companion ($host, $id)")
              complete(
                Future {
                  HttpRequest(
                    uri = s"http://$host:$companionPort/user/$userId",
                    method = DELETE,
                    headers = Seq(Accept(MediaTypes.`application/json`))
                  )
                }.flatMap {
                  http.singleRequest(_)
                }
              )
            }
          }: _*
        )
      }
    )
  }

  def run(companionsHosts: Seq[(String, Int)]): ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>
    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    ctx.log.info(s"got $companionsHosts")
    val http = Http()
    http.bindAndHandle(
      routes(companionsHosts, config.getInt("companion.port"), http)(ctx.system),
      config.getString("hub.publicApi.host"), config.getInt("hub.publicApi.port")
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
