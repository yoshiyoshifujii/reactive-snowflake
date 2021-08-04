package com.github.yoshiyoshifujii.reactive.snowflake

case class NextId(id: Option[Long], lastTimestamp: Long, nextSequence: Long)

private[snowflake] trait IdWorkerImpl {

  protected val datacenterId: Long
  protected val workerId: Long

  def nextId(timestamp: Long, lastTimestamp: Long, currentSequence: Long): NextId = {
    val id = Some(1L)
    NextId(id, timestamp, 0L)
  }

}
