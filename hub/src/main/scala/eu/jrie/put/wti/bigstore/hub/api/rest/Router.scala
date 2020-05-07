package eu.jrie.put.wti.bigstore.hub.api.rest

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import eu.jrie.put.wti.bigstore.hub.companions.CompanionConnector

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object Router {

  private val config = ConfigFactory.load().getConfig("big-store")

  def run(companionsHosts: Seq[(String, Int)]): ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>
    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    ctx.log.info(s"got $companionsHosts")
    val http = Http()
    val routes = Routes(
      companionsHosts.map { case (host, id) =>
        (id, ctx.spawn(CompanionConnector(host, http), s"companionConnector_$host"))
      }
    )(ctx.system)
    http.bindAndHandle(
      routes,
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
