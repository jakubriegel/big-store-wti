package eu.jrie.put.wti.bigstore.hub

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import eu.jrie.put.wti.bigstore.hub.api.async.ChangeEventConsumer
import eu.jrie.put.wti.bigstore.hub.api.async.ChangeEventConsumer.StartConsuming
import eu.jrie.put.wti.bigstore.hub.api.rest.Router
import eu.jrie.put.wti.bigstore.hub.discovery.DiscoveryRoutes

object HubService extends App {
  sealed trait HubServiceMsg
  case class Companions(companionsHosts: Seq[(String, Int)]) extends HubServiceMsg
  case class HubError(msg: String = "") extends HubServiceMsg

  val hubSystem = ActorSystem[HubServiceMsg](Behaviors.receive { case (ctx, msg) =>
    msg match {
      case Companions(hosts) =>
        Router.run(hosts)
        ctx.spawn(ChangeEventConsumer(), "changeEventConsumer") ! StartConsuming()
        Behaviors.same
      case HubError(_) =>
        Behaviors.stopped
    }
  }, "hubSystem")

  DiscoveryRoutes.run(hubSystem)
}
