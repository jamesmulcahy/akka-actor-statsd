package com.deploymentzone.actor.protocol

abstract class CounterMessage[T](val bucket: String)(val value: T, val samplingRate: Double = 1.0) {
  val symbol: String

  override def toString =
    samplingRate match {
      case 1.0  => s"$bucket:$value|$symbol"
      case _    => s"$bucket:$value|$symbol|@$samplingRate"
    }
}

class Count(bucket: String)(value: Int, samplingRate: Double = 1.0)
  extends CounterMessage[Int](bucket)(value, samplingRate) {

  override val symbol = "c"
}

object Count {
  def apply(bucket: String)(value: Int, samplingRate: Double = 1.0) = new Count(bucket)(value, samplingRate)
}

class Increment(bucket: String) extends Count(bucket)(1)

object Increment {
  def apply(bucket: String) = new Increment(bucket)
}

class Decrement(bucket: String) extends Count(bucket)(-1)

object Decrement {
  def apply(bucket: String) = new Decrement(bucket)
}

class Gauge(bucket: String)(value: Long, samplingRate: Double = 1.0)
  extends CounterMessage[Long](bucket)(value, samplingRate) {

  override val symbol = "g"
}

object Gauge {
  def apply(bucket: String)(value: Long, samplingRate: Double = 1.0) = new Gauge(bucket)(value, samplingRate)
}

class Timing(bucket: String)(value: Long, samplingRate: Double = 1.0)
  extends CounterMessage(bucket)(value, samplingRate) {

  override val symbol = "ms"
}

object Timing {
  import scala.concurrent.duration.Duration

  def apply(bucket: String)(value: Duration, samplingRate: Double = 1.0) = new Timing(bucket)(value.toMillis, samplingRate)
}