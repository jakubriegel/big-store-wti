package eu.jrie.put.wti.bigstore.hub.discovery

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import eu.jrie.put.wti.bigstore.hub.discovery.CompanionRegistry.{AllCompanionsReady, CompanionId, CompanionRegistryMsg, RegisterCompanion, SetCompanionReady}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object DiscoveryRoutes {
  private def routes(companionRegistry: ActorRef[CompanionRegistryMsg])(implicit actorSystem: ActorSystem[_]): Route = {
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

    import scala.concurrent.duration._
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
                case AllCompanionsReady(true) => StatusCodes.Accepted
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

  private val config = ConfigFactory.load().getConfig("big-store.hub")

  def run: ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>

    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    val registry: ActorRef[CompanionRegistryMsg] = ctx.spawn(CompanionRegistry(2), "companionRegistry")
    Http().bindAndHandle(
      routes(registry)(ctx.system),
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
  }, "discoverySystem")
}
