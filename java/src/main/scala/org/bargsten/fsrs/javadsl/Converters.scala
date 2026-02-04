package org.bargsten.fsrs.javadsl

import org.bargsten.fsrs.{State => ScalaState, Rating => ScalaRating}
import scala.concurrent.duration.{Duration => ScalaDuration, FiniteDuration}
import scala.jdk.DurationConverters.*
import java.time.{Duration => JavaDuration}

private[javadsl] object Converters:
  def toJava(s: ScalaState): State = s match
    case ScalaState.Learning   => State.LEARNING
    case ScalaState.Review     => State.REVIEW
    case ScalaState.Relearning => State.RELEARNING

  def toScala(j: State): ScalaState = j match
    case State.LEARNING   => ScalaState.Learning
    case State.REVIEW     => ScalaState.Review
    case State.RELEARNING => ScalaState.Relearning

  def toJava(r: ScalaRating): Rating = r match
    case ScalaRating.Again => Rating.AGAIN
    case ScalaRating.Hard  => Rating.HARD
    case ScalaRating.Good  => Rating.GOOD
    case ScalaRating.Easy  => Rating.EASY

  def toScala(j: Rating): ScalaRating = j match
    case Rating.AGAIN => ScalaRating.Again
    case Rating.HARD  => ScalaRating.Hard
    case Rating.GOOD  => ScalaRating.Good
    case Rating.EASY  => ScalaRating.Easy

  def toScala(d: JavaDuration): FiniteDuration = d.toScala

  def toJava(d: ScalaDuration): JavaDuration = JavaDuration.ofNanos(d.toNanos)
