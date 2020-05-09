package eu.jrie.put.wti.bigstore.hub.api.rest

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.HttpExt
import com.typesafe.config.ConfigFactory
import eu.jrie.put.wti.bigstore.hub.companions.CompanionConnector.CompanionMsg

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object Router {

  private val config = ConfigFactory.load().getConfig("big-store")

  def run(companionsHosts: Seq[(Int, ActorRef[CompanionMsg])], http: HttpExt): ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { ctx =>
    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    ctx.log.info(s"got $companionsHosts")
    http.bindAndHandle(
      Routes(companionsHosts)(ctx.system),
      config.getString("hub.publicApi.rest.host"), config.getInt("hub.publicApi.rest.port")
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
