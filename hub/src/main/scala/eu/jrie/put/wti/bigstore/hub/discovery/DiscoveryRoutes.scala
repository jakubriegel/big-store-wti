package eu.jrie.put.wti.bigstore.hub.discovery

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import eu.jrie.put.wti.bigstore.hub.HubService.{Companions, HubError, HubServiceMsg}
import eu.jrie.put.wti.bigstore.hub.discovery.CompanionRegistry.{AllCompanionsReady, CompanionId, CompanionRegistryMsg, RegisterCompanion, SetCompanionReady}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object DiscoveryRoutes {
  sealed trait CompanionsDiscoveryMsg
  case class CompanionsReady(hosts: Seq[String]) extends CompanionsDiscoveryMsg
  case class CompanionsDiscoveryError() extends CompanionsDiscoveryMsg

  private val config = ConfigFactory.load().getConfig("big-store.hub.internalApi")

  private def routes(companionRegistry: ActorRef[CompanionRegistryMsg])(implicit actorSystem: ActorSystem[CompanionsDiscoveryMsg]): Route = {
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext
    implicit val timeout: Timeout = 15.seconds

    import akka.http.scaladsl.server.Directives._
    import akka.actor.typed.scaladsl.AskPattern._

    concat(
      path("register") {
        post {
          extractHost { host =>
            val id: Future[CompanionId] = companionRegistry ? (RegisterCompanion(host, _))
            complete(
              id.map { _.id }
                .map { id => s"""{"id": $id}""" }
                .map { HttpEntity(ContentTypes.`application/json`, _) }
                .map { HttpResponse(StatusCodes.OK, Seq.empty, _) }
            )
          }
        }
      },
      path("register"/ IntNumber / "ready") { id =>
        post {
          extractHost { host =>
            val data: Future[AllCompanionsReady] = companionRegistry ? (SetCompanionReady(host, id, _))
            complete(
              data.map {
                case AllCompanionsReady(true, hosts) =>
                  actorSystem ! CompanionsReady(hosts)
                  StatusCodes.Accepted
                case _ => StatusCodes.OK
              } .map {
                HttpResponse(_, Seq.empty)
              }
            )
          }
        }
      }
    )
  }

  def run(hub: ActorSystem[HubServiceMsg]): ActorSystem[CompanionsDiscoveryMsg] = ActorSystem[CompanionsDiscoveryMsg](Behaviors.setup[CompanionsDiscoveryMsg] { ctx =>

    import akka.actor.typed.scaladsl.adapter._
    implicit val classicSystem: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    val registry: ActorRef[CompanionRegistryMsg] = ctx.spawn(CompanionRegistry(1), "companionRegistry")
    Http().bindAndHandle(
      routes(registry)(ctx.system.asInstanceOf[ActorSystem[CompanionsDiscoveryMsg]]),
      config.getString("host"), config.getInt("port")
    ).onComplete {
      case Success(bound) =>
        ctx.log.info(s"Register endpoints online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
      case Failure(e) =>
        ctx.log.error(s"Register endpoints not start! $e")
        e.printStackTrace()
        ctx.self ! CompanionsDiscoveryError()
    }

    Behaviors.receiveMessage {
      case CompanionsReady(hosts) =>
        hub ! Companions(hosts)
        Behaviors.stopped
      case CompanionsDiscoveryError() =>
        hub ! HubError("CompanionsDiscoveryError")
        Behaviors.stopped
    }
  }, "discoverySystem")
}
