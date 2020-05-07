package eu.jrie.put.wti.bigstore.hub.companions

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpMethods.{DELETE, PUT}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model._
import com.typesafe.config.ConfigFactory
import eu.jrie.put.wti.bigstore.hub.companions.CompanionConnector.CompanionMsg

import scala.concurrent.{ExecutionContextExecutor, Future}

object CompanionConnector {
  sealed trait CompanionMsg
  case class FindEntity(id: Long, replyTo: ActorRef[CompanionResponse]) extends CompanionMsg
  case class UpdateEntity(id: Long, resource: String, data: String, replyTo: ActorRef[CompanionResponse]) extends CompanionMsg
  case class DeleteEntity(id: Long, replyTo: ActorRef[CompanionResponse]) extends CompanionMsg

  case class CompanionResponse(response: Future[HttpResponse]) extends CompanionMsg

  private val config = ConfigFactory.load().getConfig("big-store.companion")
  private val companionPort = config.getInt("port")
  private val entityName = config.getString("entity.name")

  def apply(host: String, http: HttpExt): Behavior[CompanionMsg] = Behaviors.setup(implicit context => new CompanionConnector(host, http))
}

private class CompanionConnector (val host: String, val http: HttpExt)
                                 (implicit context: ActorContext[CompanionMsg])
  extends AbstractBehavior[CompanionMsg](context) {

  import CompanionConnector.{companionPort, entityName, FindEntity, UpdateEntity, DeleteEntity, CompanionResponse}
  private implicit val system: ActorSystem[Nothing] = context.system
  private implicit val executionContext: ExecutionContextExecutor = system.executionContext

  override def onMessage(msg: CompanionMsg): Behavior[CompanionMsg] = {
    msg match {
      case FindEntity(id, replyTo) =>
        replyTo ! CompanionResponse(findEntity(id))
        Behaviors.same
      case UpdateEntity(id, resource, data, replyTo) =>
        replyTo ! CompanionResponse(updateEntity(id, resource, data))
        Behaviors.same
      case DeleteEntity(id, replyTo) =>
        replyTo ! CompanionResponse(deleteEntity(id))
        Behaviors.same
    }
  }

  private def findEntity(id: Long) = Future {
      HttpRequest(
        uri = url(id),
        headers = Seq(Accept(MediaTypes.`application/json`))
      )
    }.flatMap { http.singleRequest(_) }

  private def updateEntity(id: Long, resource: String, data: String) = Future {
      HttpRequest(
        uri = s"${url(id)}/$resource",
        method = PUT,
        headers = Seq(Accept(MediaTypes.`application/json`)),
        entity = HttpEntity(ContentTypes.`application/json`, data)
      )
    }.flatMap { http.singleRequest(_) }

  private def deleteEntity(id: Long) = Future {
    HttpRequest(
      uri = url(id),
      method = DELETE,
      headers = Seq(Accept(MediaTypes.`application/json`))
    )
  }.flatMap { http.singleRequest(_) }

  private def url(id: Long) = s"http://$host:$companionPort/$entityName/$id"
}
