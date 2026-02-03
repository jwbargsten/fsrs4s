package org.bargsten.fsrs

import java.time.{Clock, OffsetDateTime, Period, Duration as JDuration}
import scala.concurrent.duration.*

object DateTimeUtil {
  opaque type Days = Int

  object Days {
    def apply(d: Int): Days = d
    def apply(d: java.time.Duration): Days = d.toDays.toInt
    def apply(d: Duration): Days = d.toDays.toInt

    val Zero: Days = 0
    val One: Days = 1

    extension (x: Days) {
      def >(y: Days): Boolean = x > y
      def <(y: Days): Boolean = x < y
      def <=(y: Days): Boolean = x <= y
      def >=(y: Days): Boolean = x >= y

      def unwrap: Int = x
      def toPeriod: Period = Period.ofDays(x)
      def toDuration: Duration = x.days
    }
  }

  def between(startInclusive: OffsetDateTime, endExclusive: OffsetDateTime): FiniteDuration =
    Duration(JDuration.between(startInclusive, endExclusive).toMillis, MILLISECONDS)

}
