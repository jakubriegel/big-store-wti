package eu.jrie.put.wti.bigstore.hub.api.async

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.alpakka.amqp.scaladsl.AmqpSource
import akka.stream.alpakka.amqp.{AmqpUriConnectionProvider, ExchangeDeclaration, TemporaryQueueSourceSettings}
import eu.jrie.put.wti.bigstore.hub.api.async.ChangeEventConsumer.{ChangeEventConsumerMsg, StartConsuming}

import scala.concurrent.ExecutionContextExecutor

object ChangeEventConsumer {
  sealed trait ChangeEventConsumerMsg
  case class StartConsuming() extends ChangeEventConsumerMsg

  def apply(): Behavior[ChangeEventConsumerMsg] = Behaviors.setup(implicit context => new ChangeEventConsumer)
}

private class ChangeEventConsumer (
                                 implicit context: ActorContext[ChangeEventConsumerMsg]
                               ) extends AbstractBehavior[ChangeEventConsumerMsg](context) {

  override def onMessage(msg: ChangeEventConsumerMsg): Behavior[ChangeEventConsumerMsg] = {
    msg match {
      case StartConsuming() =>
        consume(context.system)
        Behaviors.same
      case _ =>
        context.log.info("unsupported repo msg")
        Behaviors.stopped
    }
  }

  private def consume(implicit actorSystem: ActorSystem[_]): Unit = {
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext
    val exchangeDeclaration = ExchangeDeclaration("user_profile", "fanout")
    AmqpSource
      .atMostOnceSource(
        TemporaryQueueSourceSettings(
          AmqpUriConnectionProvider("amqp://events"),
          "user_profile"
        ).withDeclaration(exchangeDeclaration),
        bufferSize = 5
      )
      .map { _.bytes.utf8String }
      .runForeach {
        actorSystem.log.info(_)
      } .onComplete { res =>
        if (res.isFailure) actorSystem.log.info(s"FAILED ${res.failed.get}")
        else actorSystem.log.info("COOL")

    }
  }
}