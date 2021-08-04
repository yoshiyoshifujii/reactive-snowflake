package com.github.yoshiyoshifujii.reactive.snowflake

import org.scalatest.freespec.AnyFreeSpec

class IdWorkerImplSpec extends AnyFreeSpec {

  "IdWWorkerImpl" - {

    "generate an id" in {
      val worker = new IdWorkerImpl {
        override protected val datacenterId: Long = 1L
        override protected val workerId: Long = 1L
      }

      val timestamp = System.currentTimeMillis()
      val NextId(Some(id), _, _) = worker.nextId(timestamp, timestamp, 0L)
      assert(id > 0L)
    }

  }

}
