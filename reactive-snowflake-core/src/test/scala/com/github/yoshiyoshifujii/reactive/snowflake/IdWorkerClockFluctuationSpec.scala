package com.github.yoshiyoshifujii.reactive.snowflake

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.github.yoshiyoshifujii.reactive.snowflake.IdWorker.{ DatacenterId, GenerateId, IdGenerated, WorkerId }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec

import scala.concurrent.duration._

class IdWorkerClockFluctuationSpec extends AnyFreeSpec with BeforeAndAfterAll {

  private val testKit = ActorTestKit()

  "idWorker under fluctuating clock" - {

    val dcId1     = DatacenterId(1)
    val workerId1 = WorkerId(1)

    "generate only unique ids, even when time goes backwards" in {
      val n = 1_000

      val probe = testKit.createTestProbe[IdGenerated]
      val idWorker = testKit.spawn(
        IdWorker.behavior(dcId1, workerId1)(_ =>
          System.currentTimeMillis() + (if (math.random() > 0.6) -1 else 0) * 100
        )
      )

      (1 to n).foreach(_ => idWorker ! GenerateId(probe.ref))

      probe.within(10.seconds) {
        probe.receiveMessages(n).foldLeft(0L) { (prev, current) =>
          assert(prev < current.id)
          current.id
        }
      }
    }
  }
}
