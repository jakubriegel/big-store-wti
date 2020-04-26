package eu.jrie.put.wti.bigstore.hub

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import eu.jrie.put.wti.bigstore.hub.discovery.DiscoveryRoutes
import eu.jrie.put.wti.bigstore.hub.routing.Router

object HubService extends App {
  sealed trait HubServiceMsg
  case class Companions(companionsHosts: Seq[String]) extends HubServiceMsg
  case class HubError(msg: String = "") extends HubServiceMsg

  val hubSystem = ActorSystem[HubServiceMsg](Behaviors.receiveMessage {
    case Companions(hosts) =>
      Router.run(hosts)
      Behaviors.same
    case HubError(_) =>
      Behaviors.stopped
  }, "hubSystem")

  DiscoveryRoutes.run(hubSystem)
}
