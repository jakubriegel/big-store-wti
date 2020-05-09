package eu.jrie.put.wti.bigstore.hub

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import eu.jrie.put.wti.bigstore.hub.api.async.ChangeEventConsumer
import eu.jrie.put.wti.bigstore.hub.api.async.ChangeEventConsumer.StartConsuming
import eu.jrie.put.wti.bigstore.hub.api.rest.Router
import eu.jrie.put.wti.bigstore.hub.companions.{CompanionConnector, Companions}
import eu.jrie.put.wti.bigstore.hub.discovery.DiscoveryRoutes

object HubService extends App {
  sealed trait HubServiceMsg
  case class CompanionsMsg(companionsHosts: Seq[(String, Int)]) extends HubServiceMsg
  case class HubError(msg: String = "") extends HubServiceMsg

  val hubSystem = ActorSystem[HubServiceMsg](Behaviors.receive { case (ctx, msg) =>
    import akka.actor.typed.scaladsl.adapter._
    implicit val system: akka.actor.ActorSystem = ctx.system.toClassic
    msg match {
      case CompanionsMsg(hosts) =>
        val http = Http()
        val h = hosts.map { case (host, id) =>
          (id, ctx.spawn(CompanionConnector(host, http), s"companionConnector_$host"))
        }
        Router.run(h, http)
        ctx.spawn(ChangeEventConsumer(new Companions(h)), "changeEventConsumer") ! StartConsuming()
        Behaviors.same
      case HubError(_) =>
        Behaviors.stopped
    }
  }, "hubSystem")

  DiscoveryRoutes.run(hubSystem)
}
