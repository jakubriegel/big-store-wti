package eu.jrie.put.wti.bigstore.hub

import eu.jrie.put.wti.bigstore.hub.discovery.DiscoveryRoutes
import eu.jrie.put.wti.bigstore.hub.routing.Router

object HubService extends App {
//  Router.run(Seq("ip1", "ip2", "ip3"))
  DiscoveryRoutes.run
}
