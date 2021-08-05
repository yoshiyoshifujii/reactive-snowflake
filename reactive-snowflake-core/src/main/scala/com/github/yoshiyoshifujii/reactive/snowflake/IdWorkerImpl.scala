package com.github.yoshiyoshifujii.reactive.snowflake

@SerialVersionUID(1L)
final case class InvalidSystemClock(message: String, lastTimestamp: Long, lastSequenceId: Long)
    extends Exception(message)

final case class NextId(id: Option[Long], lastTimestamp: Long, nextSequence: Long)

final case class Bits(private val value: Long) {
  def maxId: Long          = -1L ^ (-1L << value)
  def +(other: Bits): Bits = Bits(value + other.value)
  def toLong: Long         = value
}

private[snowflake] trait IdWorkerImpl {

  protected val datacenterId: Long
  protected val workerId: Long

  // 2010-11-04T01:42:54.657Z
  // from https://github.com/twitter-archive/snowflake/blob/snowflake-2010/src/main/scala/com/twitter/service/snowflake/IdWorker.scala#L25
  protected val twepoch                = 1288834974657L
  protected val workerIdBits: Bits     = Bits(5L)
  protected val datacenterIdBits: Bits = Bits(5L)
  protected val sequenceBits: Bits     = Bits(12L)

  private val maxWorkerId     = workerIdBits.maxId
  private val maxDatacenterId = datacenterIdBits.maxId
  private val maxSequenceId   = sequenceBits.maxId

  private val workerIdShift      = sequenceBits.toLong
  private val datacenterIdShift  = (sequenceBits + workerIdBits).toLong
  private val timestampLeftShift = (sequenceBits + workerIdBits + datacenterIdBits).toLong

  private val sequenceMask = sequenceBits.maxId

  def nextId(timestamp: Long, lastTimestamp: Long, currentSequence: Long): NextId = {

    if (timestamp < lastTimestamp) {
      throw InvalidSystemClock(
        s"Clock moved backwards.  Refusing to generate id for ${lastTimestamp - timestamp} milliseconds",
        lastTimestamp,
        currentSequence
      )
    }

    require(currentSequence < sequenceMask + 1)

    val sequence = if (lastTimestamp == timestamp) {
      val s = (currentSequence + 1) & sequenceMask
      if (s == 0) None else Some(s)
    } else {
      Some(0L)
    }

    val id = sequence.map { s =>
      ((timestamp - twepoch) << timestampLeftShift) |
      (datacenterId << datacenterIdShift) |
      (workerId << workerIdShift) |
      s
    }

    NextId(id, timestamp, sequence.getOrElse(maxSequenceId))
  }

}
