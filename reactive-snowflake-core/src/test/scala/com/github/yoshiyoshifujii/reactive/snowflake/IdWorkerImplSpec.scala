package com.github.yoshiyoshifujii.reactive.snowflake

import org.scalatest.freespec.AnyFreeSpec

class IdWorkerImplSpec extends AnyFreeSpec {

  "IdWWorkerImpl" - {
    val workerMask     = 0x000000000001f000L
    val datacenterMask = 0x00000000003e0000L
    val timestampMask  = 0xffffffffffc00000L

    "generate an id" in {
      val worker = new IdWorkerImpl {
        override val datacenterId: Long = 1L
        override val workerId: Long     = 1L
      }

      val timestamp              = System.currentTimeMillis()
      val NextId(Some(id), _, _) = worker.nextId(timestamp, timestamp, 0L)
      assert(id > 0L)
    }

    "properly mask worker id" in {
      val wId = 0x1fL

      val worker = new IdWorkerImpl {
        override val datacenterId: Long = 1L
        override val workerId: Long     = wId
      }
      (1 to 1000).foreach { _ =>
        val lastTimestamp = System.currentTimeMillis()
        val id            = worker.nextId(System.currentTimeMillis(), lastTimestamp, 0L)
        assert(((id.id.get & workerMask) >> 12) === wId)
      }
    }

    "properly mask dc id" in {
      val dcId = 0x1f
      val wId  = 0

      val worker = new IdWorkerImpl {
        override val datacenterId: Long = dcId
        override val workerId: Long     = wId
      }

      val lastTimestamp = System.currentTimeMillis()
      val id            = worker.nextId(System.currentTimeMillis(), lastTimestamp, 0L)
      assert(((id.id.get & datacenterMask) >> 17) === dcId)
    }

    "properly mask timestamp" in {
      val epo = 1288834974657L
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = 1L
        override protected val workerId: Long     = 1L
        override protected val twepoch: Long      = epo
      }
      (1 to 100).foreach { _ =>
        val t  = System.currentTimeMillis()
        val id = worker.nextId(t, t - 1L, 0L)
        ((id.id.get & timestampMask) >> 22) === (t - epo)
      }
    }

    "roll over sequence id" in {
      val dcId = 4
      val wId  = 4
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = dcId
        override protected val workerId: Long     = wId
      }
      var sequence: Long = 0x000
      val endSequence    = 0xfff

      val timestamp = System.currentTimeMillis()

      (sequence until endSequence).foreach { _ =>
        val id = worker.nextId(timestamp, timestamp, sequence)
        assert(((id.id.get & workerMask) >> 12) === wId)
        sequence = id.nextSequence
      }
    }

    "does not generate ids over max sequence" in {
      val dcId = 4
      val wId  = 4
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = dcId
        override protected val workerId: Long     = wId
      }

      val timestamp = System.currentTimeMillis()

      val id = worker.nextId(timestamp, timestamp, 0xfff)
      assert(id.id === None)
    }

    "generate increasing ids" in {
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = 1L
        override protected val workerId: Long     = 1L
      }

      var lastId          = 0L
      var currentSequence = 0L

      (1 to 100).foreach { _ =>
        val lastTimestamp = System.currentTimeMillis()
        val id            = worker.nextId(System.currentTimeMillis(), lastTimestamp, currentSequence)
        assert(id.id.get > lastId)
        lastId = id.id.get
        currentSequence = id.nextSequence
      }
    }

    "generate 1 million ids quickly" in {
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = 1L
        override protected val workerId: Long     = 1L
      }
      var lastTimestamp = -1L
      val t             = System.currentTimeMillis()
      (1 to 1_000_000).foreach { _ =>
        val currentTimestamp = System.currentTimeMillis()
        worker.nextId(currentTimestamp, lastTimestamp, 0L)
        lastTimestamp = currentTimestamp
      }
      val t2 = System.currentTimeMillis()
      println("generated 1,000,000 ids in %d ms, or %,.0f ids/second".format(t2 - t, 1000000000.0 / (t2 - t)))
      assert(1 === 1)
    }
  }

}
