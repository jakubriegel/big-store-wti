package eu.jrie.put.wti.bigstore.hub.companions

import akka.actor.typed.ActorRef
import eu.jrie.put.wti.bigstore.hub.companions.CompanionConnector.CompanionMsg

class Companions (val hosts: Seq[(Int, ActorRef[CompanionMsg])]) {
  def getCompanionForEntity(id: Int): ActorRef[CompanionMsg] = {
    val companionId = (id % hosts.length) + 1
    hosts.find { case (i, _) => i == companionId }
      .map { case (_, companion) => companion}
      .get
  }
}
