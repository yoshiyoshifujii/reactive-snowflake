package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.typed.{ ActorRef, Behavior, PreRestart }
import akka.actor.typed.scaladsl.Behaviors

object IdWorker {

  final case class DatacenterId(value: Long)
  final case class WorkerId(value: Long)

  sealed trait Reply
  final case class IdGenerated(id: Long)

  sealed trait Command
  final case class GenerateId(reply: ActorRef[IdGenerated]) extends Command

  def behavior(dcId: DatacenterId, wId: WorkerId): Behavior[Command] =
    Behaviors.setup { context =>
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = dcId.value
        override protected val workerId: Long     = wId.value
      }

      var sequenceId: Long = 0L
      var lastTimestamp    = -1L

      Behaviors.receiveMessage[Command] { case msg @ GenerateId(replyTo) =>
        val NextId(idOpt, timestamp, nextSequence) = worker.nextId(timeGen(), lastTimestamp, sequenceId)
        sequenceId = nextSequence
        lastTimestamp = timestamp

        idOpt match {
          case Some(id) =>
            replyTo ! IdGenerated(id)
            Behaviors.same
          case None =>
            context.self ! msg
            Behaviors.same
        }
      }
    }

  private def timeGen(): Long = System.currentTimeMillis()

}
