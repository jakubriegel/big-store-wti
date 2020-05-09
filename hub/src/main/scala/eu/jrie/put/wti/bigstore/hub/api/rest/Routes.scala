package eu.jrie.put.wti.bigstore.hub.api.rest

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives.{as, complete, delete, entity, get, path, put}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation.concat
import akka.util.Timeout
import eu.jrie.put.wti.bigstore.hub.companions.CompanionConnector._

import scala.concurrent.{ExecutionContextExecutor, Future}

object Routes {

  import scala.concurrent.duration._
  implicit val timeout: Timeout = 15.seconds

  def apply(companionsHosts: Seq[(Int, ActorRef[CompanionMsg])])(implicit actorSystem: ActorSystem[Nothing]): Route = routes(companionsHosts)

  private def routes(companionsHosts: Seq[(Int, ActorRef[CompanionMsg])])(implicit actorSystem: ActorSystem[Nothing]): Route = {
    concat(
      companionGetRoutes(companionsHosts),
      companionManageRoutes(companionsHosts)
    )
  }

  private def companionGetRoutes(companionsHosts: Seq[(Int, ActorRef[CompanionMsg])])(implicit actorSystem: ActorSystem[Nothing]) = {
    import akka.actor.typed.scaladsl.AskPattern._
    import akka.http.scaladsl.server.PathMatcher._
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    get {
      concat(
        companionsHosts map { case (companionId, connector) =>
          path("user" / new ModuloMatcher(companionsHosts.length, companionId - 1)) { entityId =>
            actorSystem.log.info(s"Routing GET user $entityId to companion $companionId")
            val response: Future[CompanionResponse] = connector ? (FindEntity(entityId, _))
            complete(response.map { _.response })
          }
        }: _*
      )
    }
  }

  private def companionManageRoutes(companionsHosts: Seq[(Int, ActorRef[CompanionMsg])])(implicit actorSystem: ActorSystem[Nothing]) = {
    import akka.actor.typed.scaladsl.AskPattern._
    import akka.http.scaladsl.server.PathMatcher._
    import akka.http.scaladsl.server.PathMatchers._
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    concat(
      put {
        concat(
          companionsHosts map { case (companionId, connector) =>
            path("user" / new ModuloMatcher(companionsHosts.length, companionId - 1) / Remaining) { case (entityId, resource) =>
              entity(as[String]) { payload =>
                actorSystem.log.info(s"Routing PUT user $entityId to companion $companionId")
                val response: Future[CompanionResponse] = connector ? (UpdateEntity(entityId, resource, payload, _))
                complete(response.map { _.response })
              }
            }
          } :_*
        )
      },
      delete {
        concat(
          companionsHosts map { case (companionId, connector) =>
            path("user" / new ModuloMatcher(companionsHosts.length, companionId - 1)) { entityId =>
              actorSystem.log.info(s"Routing DELETE user $entityId to companion $companionId")
              val response: Future[CompanionResponse] = connector ? (DeleteEntity(entityId, _))
              complete(response.map { _.response })
            }
          }: _*
        )
      }
    )
  }
}
