package org.bargsten.fsrs

import org.scalatest.funsuite.AnyFunSuite
import org.bargsten.fsrs.Rating.Easy
import org.bargsten.fsrs.{Card, Parameters, Rating, Review, Scheduler, State}

import java.time.{Clock, Instant, OffsetDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

class SchedulerSuite extends AnyFunSuite {
  val now = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
  given Clock = Clock.fixed(now.toInstant, ZoneOffset.UTC)
  val params = Parameters(withFuzzing = false)
  val scheduler = new Scheduler(params)

  test("new card with Good rating initializes stability and difficulty") {
    val card = Card()
    val review = Review(Rating.Good, now)
    val (updated, _) = scheduler.reviewCard(card, review)

    assert(updated.stability > 0)
    assert(updated.difficulty > 0)
    assert(updated.lastReview.contains(now))
    assert(updated.firstReview.contains(now))
    assert(updated.state == State.Learning)
    assert(updated.step == 1)
  }

  test("new card with Easy rating transitions to Review") {
    val card = Card()
    val review = Review(Easy, now)
    val (updated, _) = scheduler.reviewCard(card, review)

    assert(updated.state == State.Review)
    assert(updated.step == 0)
    assert(updated.due.isAfter(now))
  }

  test("learning card progresses through steps with Good") {
    val card = Card()
    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good, now))
    assert(c1.state == State.Learning)
    assert(c1.step == 1)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, now.plusMinutes(10)))
    assert(c2.state == State.Review)
  }

  test("learning card resets step on Again") {
    val card = Card()
    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good, now))
    assert(c1.step == 1)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Again, now.plusMinutes(10)))
    assert(c2.step == 0)
    assert(c2.state == State.Learning)
  }

  test("review card transitions to Relearning on Again") {
    val reviewCard = Card(
      state = State.Review,
      stability = 10.0,
      difficulty = 5.0,
      lastReview = Some(now.minusDays(10))
    )
    val (updated, _) = scheduler.reviewCard(reviewCard, Review(Rating.Again, now))

    assert(updated.state == State.Relearning)
    assert(updated.step == 0)
  }

  test("review card stays in Review on Good") {
    val reviewCard = Card(
      state = State.Review,
      stability = 10.0,
      difficulty = 5.0,
      lastReview = Some(now.minusDays(10))
    )
    val review = Review(Rating.Good, now)
    val (updated, _) = scheduler.reviewCard(reviewCard, review)

    assert(updated.state == State.Review)
    assert(updated.stability > reviewCard.stability)
  }

  test("relearning card transitions to Review on Good at last step") {
    val relearningCard = Card(
      state = State.Relearning,
      stability = 5.0,
      difficulty = 5.0,
      step = 0,
      lastReview = Some(now.minusMinutes(10))
    )
    val review = Review(Rating.Good, now)
    val (updated, _) = scheduler.reviewCard(relearningCard, review)

    assert(updated.state == State.Review)
  }

  test("stability increases on successful review") {
    val reviewCard = Card(
      state = State.Review,
      stability = 10.0,
      difficulty = 5.0,
      lastReview = Some(now.minusDays(10))
    )
    val review = Review(Rating.Good, now)
    val (updated, _) = scheduler.reviewCard(reviewCard, review)
    assert(updated.stability > reviewCard.stability)
  }

  test("difficulty decreases on Easy rating") {
    val reviewCard = Card(
      state = State.Review,
      stability = 10.0,
      difficulty = 5.0,
      lastReview = Some(now.minusDays(10))
    )
    val review = Review(Rating.Easy, now)
    val (updated, _) = scheduler.reviewCard(reviewCard, review)
    assert(updated.difficulty < reviewCard.difficulty)
  }

  test("difficulty increases on Again rating") {
    val reviewCard = Card(
      state = State.Review,
      stability = 10.0,
      difficulty = 5.0,
      lastReview = Some(now.minusDays(10))
    )
    val review = Review(Rating.Again, now)
    val (updated, _) = scheduler.reviewCard(reviewCard, review)
    assert(updated.difficulty > reviewCard.difficulty)
  }

  test("review log entry is created correctly") {
    val card = Card()
    val duration = Some(5.seconds)
    val review = Review(Rating.Good, now, duration)
    val (_, logEntry) = scheduler.reviewCard(card, review)

    assert(logEntry.cardId == card.id)
    assert(logEntry.rating == Rating.Good)
    assert(logEntry.reviewedAt == now)
    assert(logEntry.duration == duration)
  }

  test("empty learning steps transitions directly to Review") {
    val paramsNoSteps = Parameters(learningSteps = List.empty, withFuzzing = false)
    val schedulerNoSteps = new Scheduler(paramsNoSteps)
    val card = Card()

    val review = Review(Rating.Good, now)
    val (updated, _) = schedulerNoSteps.reviewCard(card, review)
    assert(updated.state == State.Review)
  }

  test("retrievability of new card is 0") {
    val card = Card()
    assert(scheduler.calcRetrievability(card) == 0.0)
  }

  test("retrievability with zero stability returns 0") {
    val card = Card(lastReview = Some(now.minusDays(5)), stability = 0.0)
    assert(scheduler.calcRetrievability(card, now) == 0.0)
  }

  test("interval history matches Python reference") {
    val testRatings = List(
      Rating.Good,
      Rating.Good,
      Rating.Good,
      Rating.Good,
      Rating.Good,
      Rating.Good,
      Rating.Again,
      Rating.Again,
      Rating.Good,
      Rating.Good,
      Rating.Good,
      Rating.Good,
      Rating.Good
    )
    val pythonParams = Parameters(withFuzzing = false)
    val pythonScheduler = new Scheduler(pythonParams)

    var card = Card()
    var reviewTime = OffsetDateTime.of(2022, 11, 29, 12, 30, 0, 0, ZoneOffset.UTC)
    val intervals = scala.collection.mutable.ListBuffer[Long]()

    for (rating <- testRatings) {
      val review = Review(rating, reviewTime)
      val (updated, _) = pythonScheduler.reviewCard(card, review)
      val intervalDays = ChronoUnit.DAYS.between(updated.lastReview.get, updated.due)
      intervals += intervalDays
      reviewTime = updated.due
      card = updated
    }

    assert(intervals.toList == List(0L, 2L, 11L, 46L, 163L, 498L, 0L, 0L, 2L, 4L, 7L, 12L, 21L))
  }

  test("stability and difficulty match Python after review sequence") {
    val ratings = List(Rating.Again, Rating.Good, Rating.Good, Rating.Good, Rating.Good, Rating.Good)
    val ivlHistory = List(0, 0, 1, 3, 8, 21)

    val pythonParams = Parameters(withFuzzing = false)
    val pythonScheduler = new Scheduler(pythonParams)

    var card = Card()
    var reviewTime = OffsetDateTime.of(2022, 11, 29, 12, 30, 0, 0, ZoneOffset.UTC)

    for ((rating, ivl) <- ratings.zip(ivlHistory)) {
      reviewTime = reviewTime.plusDays(ivl)
      val review = Review(rating, reviewTime)
      val (updated, _) = pythonScheduler.reviewCard(card, review)
      card = updated
    }

    assert(math.abs(card.stability - 53.62691) < 1e-4)
    assert(math.abs(card.difficulty - 6.3574867) < 1e-4)
  }

  test("invalid weights count throws") {
    val tooFew = Parameters.defaultWeights.take(10)
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = tooFew))
    }
  }

  test("out-of-bounds weight throws") {
    val badWeights = Parameters.defaultWeights.updated(0, -1.0)
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = badWeights))
    }
  }

  test("weights at bounds accepted") {
    val atLower = Parameters.lowerBounds
    val atUpper = Parameters.upperBounds
    new Scheduler(Parameters(weights = atLower, withFuzzing = false))
    new Scheduler(Parameters(weights = atUpper, withFuzzing = false))
  }
}
