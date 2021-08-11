package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.typed.{ ActorRef, Behavior, PreRestart }
import akka.actor.typed.scaladsl.Behaviors

object IdWorker {

  final case class DatacenterId(value: Long)
  final case class WorkerId(value: Long)
  final case class SequenceId(value: Long)

  object SequenceId {
    val Default: SequenceId = SequenceId(0L)
  }

  final case class LastTimestamp(value: Long)

  object LastTimestamp {
    val Default: LastTimestamp = LastTimestamp(-1L)
  }

  sealed trait Reply
  final case class IdGenerated(id: Long)

  sealed trait Command
  final case class GenerateId(reply: ActorRef[IdGenerated]) extends Command

  def behavior(
      dcId: DatacenterId,
      wId: WorkerId,
      seqId: SequenceId = SequenceId.Default,
      lastTime: LastTimestamp = LastTimestamp.Default
  )(implicit timeGen: Unit => Long = _ => System.currentTimeMillis()): Behavior[Command] =
    Behaviors.setup { context =>
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = dcId.value
        override protected val workerId: Long     = wId.value
      }

      var sequenceId: Long = seqId.value
      var lastTimestamp    = lastTime.value

      Behaviors.receiveMessage[Command] { case msg @ GenerateId(replyTo) =>
        try {
          val NextId(idOpt, timestamp, nextSequence) = worker.nextId(timeGen(), lastTimestamp, sequenceId)
          sequenceId = nextSequence
          lastTimestamp = timestamp

          idOpt match {
            case Some(id) =>
              replyTo ! IdGenerated(id)
              Behaviors.same
            case None =>
              context.log.debug("retrying to avoid id duplication")
              context.self ! msg
              Behaviors.same
          }
        } catch {
          case InvalidSystemClock(_, timestamp, lastSequenceId) =>
            sequenceId = lastSequenceId
            lastTimestamp = timestamp
            context.self ! msg
            Behaviors.same
        }
      }
    }

}
