package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }

object IdRouter {
  sealed trait Command

  final case class GenerateId(
      datacenterId: IdWorker.DatacenterId,
      workerId: IdWorker.WorkerId,
      reply: ActorRef[IdWorker.IdGenerated]
  ) extends Command

  private def createIdWorker(
      datacenterId: IdWorker.DatacenterId,
      workerId: IdWorker.WorkerId,
      name: String
  )(implicit context: ActorContext[Command]): ActorRef[IdWorker.Command] = {
    context.log.info(s"creating IdWorker with $datacenterId, $workerId")
    context.spawn(IdWorker.behavior(datacenterId, workerId), name)
  }

  def behavior: Behavior[Command] =
    Behaviors.setup { implicit context =>
      Behaviors.receiveMessage { case GenerateId(datacenterId, workerId, replyTo) =>
        val childName = IdWorker.name(datacenterId, workerId)
        context.child(childName) match {
          case Some(idWorker) =>
            idWorker.asInstanceOf[ActorRef[IdWorker.Command]] ! IdWorker.GenerateId(replyTo)
          case _ =>
            createIdWorker(datacenterId, workerId, childName) ! IdWorker.GenerateId(replyTo)
        }
        Behaviors.same
      }
    }

}
