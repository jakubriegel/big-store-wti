package eu.jrie.put.wti.bigstore.hub.discovery

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import eu.jrie.put.wti.bigstore.hub.discovery.CompanionRegistry.{CompanionRegistryMsg, RegisterCompanion, SetCompanionReady}

import scala.collection.mutable

object CompanionRegistry {
  sealed trait CompanionRegistryMsg
  case class RegisterCompanion(host: String, replyTo: ActorRef[CompanionId]) extends CompanionRegistryMsg
  case class SetCompanionReady(host: String, id: Int, replyTo: ActorRef[AllCompanionsReady]) extends CompanionRegistryMsg
  case class CompanionId(id: Int) extends CompanionRegistryMsg
  case class AllCompanionsReady(allReady: Boolean) extends CompanionRegistryMsg

  private val nextId = new AtomicInteger()
  private val registeredCompanions: mutable.Set[(String, Int)] = mutable.Set()
  private val readyCompanions:  mutable.Set[(String, Int)] = mutable.Set()

  def apply(expectedCompanionsNumber: Int): Behavior[CompanionRegistryMsg] =
    Behaviors.setup(implicit context => new CompanionRegistry(expectedCompanionsNumber))
}

class CompanionRegistry(private val expectedCompanionsNumber: Int)(implicit context: ActorContext[CompanionRegistryMsg])
  extends AbstractBehavior[CompanionRegistryMsg](context) {

  import CompanionRegistry._

  override def onMessage(msg: CompanionRegistryMsg): Behavior[CompanionRegistryMsg] = {
    msg match {
      case RegisterCompanion(host, replyTo) =>
        val id = registeredCompanions.find { case (h, _) => h.equals(host) }
          .map { case (_, id) => id }
          .getOrElse {
            val id = nextId.getAndIncrement()
            registeredCompanions.add((host, id))
            id
          }
        replyTo ! CompanionId(id)
        Behaviors.same
      case SetCompanionReady(readyHost, readyId, replyTo) =>
        if (registeredCompanions.remove((readyHost, readyId))) {
          readyCompanions.add((readyHost, readyId))
        }
        if (readyCompanions.size == expectedCompanionsNumber) {
          replyTo ! AllCompanionsReady(true)
          Behaviors.stopped
        } else {
          replyTo ! AllCompanionsReady(false)
          Behaviors.same
        }
    }
  }
}
