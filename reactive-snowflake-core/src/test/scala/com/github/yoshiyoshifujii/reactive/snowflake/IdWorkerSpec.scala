package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.github.yoshiyoshifujii.reactive.snowflake.IdWorker.{
  DatacenterId,
  GenerateId,
  IdGenerated,
  LastTimestamp,
  SequenceId,
  WorkerId
}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec

import scala.concurrent.duration._

class IdWorkerSpec extends AnyFreeSpec with BeforeAndAfterAll {

  private val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "IdWorker" - {

    val dcId1     = DatacenterId(1L)
    val workerId1 = WorkerId(1L)

    "generate id" in {
      val probe    = testKit.createTestProbe[IdGenerated]()
      val idWorker = testKit.spawn(IdWorker.behavior(dcId1, workerId1))
      idWorker ! GenerateId(probe.ref)
      probe.expectMessageType[IdGenerated]
    }

    "generate increasing ids" in {
      val probe    = testKit.createTestProbe[IdGenerated]()
      val idWorker = testKit.spawn(IdWorker.behavior(dcId1, workerId1))
      (1 to 100).foreach(_ => idWorker ! GenerateId(probe.ref))
      probe.within(5.seconds) {
        probe.receiveMessages(100).foldLeft(0L) { (prev, current) =>
          assert(prev < current.id)
          current.id
        }
      }
    }

    "generate 1 million ids quickly" in {
      val probe    = testKit.createTestProbe[IdGenerated]()
      val idWorker = testKit.spawn(IdWorker.behavior(dcId1, workerId1))
      val t        = System.currentTimeMillis()
      (1 to 1_000_000).foreach(_ => idWorker ! GenerateId(probe.ref))
      probe.receiveMessages(1_000_000)
      val t2 = System.currentTimeMillis()
      println("generated 1,000,000 ids in %d ms, or %,.0f ids/second".format(t2 - t, 1000000000.0 / (t2 - t)))
    }

    "sleep if we would rollover twice in the same millisecond" in {
      val seqId       = SequenceId(4095L)
      val timestamp   = System.currentTimeMillis()
      val lastTime    = LastTimestamp(timestamp)
      val timeSeq     = Vector(timestamp, timestamp, timestamp + 1L)
      val timeSeqIter = timeSeq.iterator
      val probe       = testKit.createTestProbe[IdGenerated]()
      val idWorker    = testKit.spawn(IdWorker.behavior(dcId1, workerId1, seqId, lastTime)(_ => timeSeqIter.next()))
      idWorker ! GenerateId(probe.ref)
      val id1 = probe.expectMessageType[IdGenerated].id
      assert(extractTimestamp(id1) === timestamp + 1)
      assert(extractSequenceId(id1) === 0L)
    }

    "generate only unique ids" in {
      val probe    = testKit.createTestProbe[IdGenerated]()
      val idWorker = testKit.spawn(IdWorker.behavior(DatacenterId(3L), WorkerId(3L)))
      idWorker ! GenerateId(probe.ref)
      probe.expectMessageType[IdGenerated]
    }

    "generate ids over 50 billion" in {
      val n        = 2_000_000
      val probe    = testKit.createTestProbe[IdGenerated]()
      val idWorker = testKit.spawn(IdWorker.behavior(DatacenterId(0L), WorkerId(0L)))
      1 to n foreach { _ =>
        idWorker ! GenerateId(probe.ref)
      }
      val generated = probe.receiveMessages(n, 10.seconds).collect { case IdGenerated(id) =>
        id
      }
      assert(generated.size === n)
    }
  }

  private def extractTimestamp(value: Long): Long = {
    val datacenterIdBits = 5L
    val workerIdBits     = 5L
    val sequenceBits     = 12L
    val twepoch          = 1288834974657L
    (value >> (datacenterIdBits + workerIdBits + sequenceBits)) + twepoch
  }

  private def extractSequenceId(value: Long): Long = {
    val sequenceBits = 12L
    val mask         = ~(-1L << sequenceBits)
    value & mask
  }

}
