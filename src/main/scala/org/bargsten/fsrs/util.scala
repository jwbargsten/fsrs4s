package org.bargsten.fsrs

import java.time.{Clock, OffsetDateTime, Period, Duration as JDuration}
import scala.concurrent.duration.*

// import scala.util.chaining.*

private[fsrs] object util {
  def between(startInclusive: OffsetDateTime, endExclusive: OffsetDateTime): FiniteDuration =
    Duration(JDuration.between(startInclusive, endExclusive).toMillis, MILLISECONDS)

  // https://contributors.scala-lang.org/t/new-function-pipeif-proposal/5223/3
  extension [A](a: A) {
    def cond[B >: A](pf: PartialFunction[A, B]): B = pf.applyOrElse(a, identity)
  }
}
