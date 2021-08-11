package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

object IdClient {

  sealed trait Command
  final case class GenerateId(reply: ActorRef[IdWorker.IdGenerated]) extends Command

  def behavior(datacenterId: IdWorker.DatacenterId, idGenerator: ActorRef[IdRouter.Command]): Behavior[Command] =
    Behaviors.setup { _ =>
      var workerId = 0
      Behaviors.receiveMessage { case GenerateId(replyTo) =>
        idGenerator ! IdRouter.GenerateId(datacenterId, IdWorker.WorkerId.generate(workerId), replyTo)
        workerId += 1
        Behaviors.same
      }
    }

}
