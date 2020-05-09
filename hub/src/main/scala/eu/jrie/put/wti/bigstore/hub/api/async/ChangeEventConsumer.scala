package eu.jrie.put.wti.bigstore.hub.api.async

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.alpakka.amqp.scaladsl.AmqpSource
import akka.stream.alpakka.amqp.{AmqpUriConnectionProvider, ExchangeDeclaration, TemporaryQueueSourceSettings}
import akka.util.Timeout
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigFactory
import eu.jrie.put.wti.bigstore.hub.api.async.ChangeEventConsumer.{ChangeEventConsumerMsg, StartConsuming}
import eu.jrie.put.wti.bigstore.hub.companions.CompanionConnector.{CompanionResponse, UpdateEntity}
import eu.jrie.put.wti.bigstore.hub.companions.Companions

import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object ChangeEventConsumer {
  sealed trait ChangeEventConsumerMsg
  case class StartConsuming() extends ChangeEventConsumerMsg

  def apply(companions: Companions): Behavior[ChangeEventConsumerMsg] =
    Behaviors.setup(implicit context => new ChangeEventConsumer(companions))
}

private class ChangeEventConsumer (private val companions: Companions) (
                                 implicit context: ActorContext[ChangeEventConsumerMsg]
                               ) extends AbstractBehavior[ChangeEventConsumerMsg](context) {

  import akka.actor.typed.scaladsl.AskPattern._

  import scala.concurrent.duration._

  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val exchangeDeclaration = ExchangeDeclaration("user_profile", "fanout")
  private implicit val system: ActorSystem[Nothing] = context.system
  private implicit val executionContext: ExecutionContextExecutor = context.system.executionContext
  private implicit val timeout: Timeout = 15.seconds
  private val config = ConfigFactory.load().getConfig("big-store.hub.publicApi.async")
  private val rabbitmqUri = config.getString("rabbitmq.uri")
  private val queueName = config.getString("queueName")
  private val bufferSize = config.getInt("bufferSize")

  case class ListResourceUpdateRequest(data: ArrayNode)

  override def onMessage(msg: ChangeEventConsumerMsg): Behavior[ChangeEventConsumerMsg] = {
    msg match {
      case StartConsuming() =>
        consume()
        Behaviors.same
      case _ =>
        context.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def consume(): Unit = {
    system.log.info(s"Consuming events from $queueName, host $rabbitmqUri")
    AmqpSource
      .atMostOnceSource(
        TemporaryQueueSourceSettings(
          AmqpUriConnectionProvider(rabbitmqUri),
          queueName
        ).withDeclaration(exchangeDeclaration),
        bufferSize = bufferSize
      )
      .map { _.bytes.utf8String }
      .map { mapper.readTree }
      .map { event =>
        (event.get("id"), event.get("averageRating"), event.get("ratedMovies"), event.get("stats"))
      }
      .map { case (id, ratings, movies, stats) =>
        (id.asInt, ratings.toString, movies, stats.toString)
      }
      .map { case (id, ratings, movies, stats) =>
        system.log.info(s"Got event for user $id")
        (companions.getCompanionForEntity(id), (id, ratings, movies, stats))
      }
      .map { case (companion, (id, ratings, movies, stats)) =>
        val r: Future[CompanionResponse] = companion ? (UpdateEntity(id, "ratings", ratings, _))
        val s: Future[CompanionResponse] = companion ? (UpdateEntity(id, "stats", stats, _))
        val request: JsonNode = mapper.createObjectNode().set("data", movies)
        val m: Future[CompanionResponse] = companion ? (UpdateEntity(id, "movies", request.toString, _))
        (r, s, m)
      }
      .runForeach { case (r, s, m) =>
        Await.ready(r, Duration.Inf)
        Await.ready(s, Duration.Inf)
        Await.ready(m, Duration.Inf)
      }
  }
}
