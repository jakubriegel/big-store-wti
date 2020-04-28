package eu.jrie.put.wti.bigstore.hub.discovery

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import eu.jrie.put.wti.bigstore.hub.discovery.CompanionRegistry.{CompanionRegistryMsg, RegisterCompanion, SetCompanionReady}
import scala.language.postfixOps

import scala.collection.mutable

object CompanionRegistry {
  sealed trait CompanionRegistryMsg
  case class RegisterCompanion(host: String, replyTo: ActorRef[CompanionId]) extends CompanionRegistryMsg
  case class SetCompanionReady(host: String, id: Int, replyTo: ActorRef[AllCompanionsReady]) extends CompanionRegistryMsg
  case class CompanionId(id: Int) extends CompanionRegistryMsg
  case class AllCompanionsReady(allReady: Boolean, hosts: Seq[(String, Int)] = Nil) extends CompanionRegistryMsg

  private val nextId = new AtomicInteger()
  private val registeredCompanions: mutable.Set[(String, Int)] = mutable.Set()
  private val readyCompanions:  mutable.Set[(String, Int)] = mutable.Set()

  def apply(expectedCompanionsNumber: Int): Behavior[CompanionRegistryMsg] =
    Behaviors.setup(implicit context => new CompanionRegistry(expectedCompanionsNumber))
}

class CompanionRegistry(private val expectedCompanionsNumber: Int)(implicit ctx: ActorContext[CompanionRegistryMsg])
  extends AbstractBehavior[CompanionRegistryMsg](ctx) {

  import CompanionRegistry._

  override def onMessage(msg: CompanionRegistryMsg): Behavior[CompanionRegistryMsg] = {
    msg match {
      case RegisterCompanion(host, replyTo) =>
        val id = registeredCompanions.find { case (h, _) => h.equals(host) }
          .map { case (_, id) => id }
          .getOrElse {
            val id = nextId.incrementAndGet()
            registeredCompanions.add((host, id))
            context.log.info(s"registering companion with id $id for host $host, ${expectedCompanionsNumber - registeredCompanions.size} to go")
            id
          }
        replyTo ! CompanionId(id)
        Behaviors.same
      case SetCompanionReady(readyHost, readyId, replyTo) =>
        context.log.info(s"registering companion with id $readyId for host $readyHost as ready")
        if (registeredCompanions.remove((readyHost, readyId))) {
          readyCompanions.add((readyHost, readyId))
        }
        if (readyCompanions.size == expectedCompanionsNumber) {
          replyTo ! AllCompanionsReady(
            allReady = true,
            readyCompanions toSeq
          )
          Behaviors.stopped
        } else {
          replyTo ! AllCompanionsReady(allReady = false)
          Behaviors.same
        }
    }
  }
}
