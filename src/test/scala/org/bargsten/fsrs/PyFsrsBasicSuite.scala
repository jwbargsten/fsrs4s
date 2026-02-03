package org.bargsten.fsrs

import org.scalatest.funsuite.AnyFunSuite
import org.bargsten.fsrs.DateTimeUtil.Days
import org.bargsten.fsrs.{Card, Parameters, Rating, Review, Scheduler, State}

import java.time.{OffsetDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*
import scala.util.Random

class PyFsrsBasicSuite extends AnyFunSuite {
  val now = OffsetDateTime.of(2022, 11, 29, 12, 30, 0, 0, ZoneOffset.UTC)

  val testRatings1 = List(
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

  test("review card interval history") {
    val scheduler = new Scheduler(Parameters(withFuzzing = false))
    var card = Card()
    var reviewTime = now
    val intervals = scala.collection.mutable.ListBuffer[Long]()

    for (rating <- testRatings1) {
      val (updated, _) = scheduler.reviewCard(card, Review(rating, reviewTime))
      intervals += ChronoUnit.DAYS.between(updated.lastReview.get, updated.due)
      reviewTime = updated.due
      card = updated
    }

    assert(intervals.toList == List(0L, 2L, 11L, 46L, 163L, 498L, 0L, 0L, 2L, 4L, 7L, 12L, 21L))
  }

  test("repeated correct reviews clamp difficulty to 1.0") {
    val scheduler = new Scheduler(Parameters())
    var card = Card()

    for (i <- 0 until 10) {
      val reviewTime = now.plusNanos(i * 1000)
      val (updated, _) = scheduler.reviewCard(card, Review(Rating.Easy, reviewTime))
      card = updated
    }

    assert(card.difficulty == 1.0)
  }

  test("memo state stability and difficulty") {
    val scheduler = new Scheduler(Parameters())
    val ratings = List(Rating.Again, Rating.Good, Rating.Good, Rating.Good, Rating.Good, Rating.Good)
    val ivlHistory = List(0, 0, 1, 3, 8, 21)

    var card = Card()
    var reviewTime = now

    for ((rating, ivl) <- ratings.zip(ivlHistory)) {
      reviewTime = reviewTime.plusDays(ivl)
      val (updated, _) = scheduler.reviewCard(card, Review(rating, reviewTime))
      card = updated
    }

    assert(math.abs(card.stability - 53.62691) < 1e-4)
    assert(math.abs(card.difficulty - 6.3574867) < 1e-4)
  }

  test("custom scheduler args") {
    val scheduler = new Scheduler(
      Parameters(
        desiredRetention = 0.9,
        maximumInterval = Days(36500),
        withFuzzing = false
      )
    )
    var card = Card()
    var reviewTime = now
    val intervals = scala.collection.mutable.ListBuffer[Long]()

    for (rating <- testRatings1) {
      val (updated, _) = scheduler.reviewCard(card, Review(rating, reviewTime))
      intervals += ChronoUnit.DAYS.between(updated.lastReview.get, updated.due)
      reviewTime = updated.due
      card = updated
    }

    assert(intervals.toList == List(0L, 2L, 11L, 46L, 163L, 498L, 0L, 0L, 2L, 4L, 7L, 12L, 21L))

    val customWeights = List(
      0.1456, 0.4186, 1.1104, 4.1315, 5.2417, 1.3098, 0.8975, 0.0010, 1.5674, 0.0567, 0.9661, 2.0275, 0.1592,
      0.2446, 1.5071, 0.2272, 2.8755, 1.234, 0.56789, 0.1437, 0.2
    )
    val scheduler2 = new Scheduler(
      Parameters(
        weights = customWeights,
        desiredRetention = 0.85,
        maximumInterval = Days(3650)
      )
    )
    assert(scheduler2 != null)
  }

  test("retrievability bounds") {
    val scheduler = new Scheduler(Parameters())
    var card = Card()

    assert(card.state == State.Learning)
    assert(scheduler.calcRetrievability(card) == 0)

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good))
    assert(c1.state == State.Learning)
    val r1 = scheduler.calcRetrievability(c1)
    assert(r1 >= 0 && r1 <= 1)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    assert(c2.state == State.Review)
    val r2 = scheduler.calcRetrievability(c2)
    assert(r2 >= 0 && r2 <= 1)

    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Again, c2.due))
    assert(c3.state == State.Relearning)
    val r3 = scheduler.calcRetrievability(c3)
    assert(r3 >= 0 && r3 <= 1)
  }

  test("good learning steps") {
    val scheduler = new Scheduler(Parameters())
    val createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    var card = Card(due = createdAt)

    assert(card.state == State.Learning)
    assert(card.step == 0)

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good, card.due))
    assert(c1.state == State.Learning)
    assert(c1.step == 1)
    val secondsUntilDue1 = ChronoUnit.SECONDS.between(createdAt, c1.due)
    assert(secondsUntilDue1 >= 500 && secondsUntilDue1 <= 700) // ~10 minutes

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    assert(c2.state == State.Review)
    val hoursUntilDue2 = ChronoUnit.HOURS.between(createdAt, c2.due)
    assert(hoursUntilDue2 >= 24)
  }

  test("again learning steps") {
    val scheduler = new Scheduler(Parameters())
    val createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    var card = Card(due = createdAt)

    assert(card.state == State.Learning)
    assert(card.step == 0)

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Again, card.due))
    assert(c1.state == State.Learning)
    assert(c1.step == 0)
    val secondsUntilDue = ChronoUnit.SECONDS.between(createdAt, c1.due)
    assert(secondsUntilDue >= 50 && secondsUntilDue <= 70) // ~1 minute
  }

  test("hard learning steps") {
    val scheduler = new Scheduler(Parameters())
    val createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    var card = Card(due = createdAt)

    assert(card.state == State.Learning)
    assert(card.step == 0)

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Hard, card.due))
    assert(c1.state == State.Learning)
    assert(c1.step == 0)
    val secondsUntilDue = ChronoUnit.SECONDS.between(createdAt, c1.due)
    assert(secondsUntilDue >= 320 && secondsUntilDue <= 340) // ~5.5 minutes (330 seconds)
  }

  test("easy learning steps") {
    val scheduler = new Scheduler(Parameters())
    val createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    var card = Card(due = createdAt)

    assert(card.state == State.Learning)
    assert(card.step == 0)

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Easy, card.due))
    assert(c1.state == State.Review)
    val daysUntilDue = ChronoUnit.DAYS.between(createdAt, c1.due)
    assert(daysUntilDue >= 1)
  }

  test("review state transitions") {
    val scheduler = new Scheduler(Parameters(withFuzzing = false))
    var card = Card()

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good, card.due))
    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    assert(c2.state == State.Review)

    val prevDue = c2.due
    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Good, c2.due))
    assert(c3.state == State.Review)
    val hoursUntilDue = ChronoUnit.HOURS.between(prevDue, c3.due)
    assert(hoursUntilDue >= 24)

    val prevDue2 = c3.due
    val (c4, _) = scheduler.reviewCard(c3, Review(Rating.Again, c3.due))
    assert(c4.state == State.Relearning)
    val minutesUntilDue = ChronoUnit.MINUTES.between(prevDue2, c4.due)
    assert(minutesUntilDue == 10)
  }

  test("relearning state") {
    val scheduler = new Scheduler(Parameters(withFuzzing = false))
    var card = Card()

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good, card.due))
    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Good, c2.due))

    val prevDue = c3.due
    val (c4, _) = scheduler.reviewCard(c3, Review(Rating.Again, c3.due))
    assert(c4.state == State.Relearning)
    assert(c4.step == 0)
    assert(ChronoUnit.MINUTES.between(prevDue, c4.due) == 10)

    val prevDue2 = c4.due
    val (c5, _) = scheduler.reviewCard(c4, Review(Rating.Again, c4.due))
    assert(c5.state == State.Relearning)
    assert(c5.step == 0)
    assert(ChronoUnit.MINUTES.between(prevDue2, c5.due) == 10)

    val prevDue3 = c5.due
    val (c6, _) = scheduler.reviewCard(c5, Review(Rating.Good, c5.due))
    assert(c6.state == State.Review)
    assert(ChronoUnit.HOURS.between(prevDue3, c6.due) >= 24)
  }

  test("no learning steps") {
    val scheduler = new Scheduler(Parameters(learningSteps = List.empty))
    var card = Card()

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Again))
    assert(c1.state == State.Review)
    assert(ChronoUnit.DAYS.between(c1.lastReview.get, c1.due) >= 1)
  }

  test("no relearning steps") {
    val scheduler = new Scheduler(Parameters(relearningSteps = List.empty))
    var card = Card()

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good))
    assert(c1.state == State.Learning)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    assert(c2.state == State.Review)

    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Again, c2.due))
    assert(c3.state == State.Review)
    assert(ChronoUnit.DAYS.between(c3.lastReview.get, c3.due) >= 1)
  }

  test("one card multiple schedulers") {
    val s2Learning = new Scheduler(Parameters(learningSteps = List(1.minute, 10.minutes)))
    val s1Learning = new Scheduler(Parameters(learningSteps = List(1.minute)))
    val s0Learning = new Scheduler(Parameters(learningSteps = List.empty))
    val s2Relearning = new Scheduler(Parameters(relearningSteps = List(1.minute, 10.minutes)))
    val s1Relearning = new Scheduler(Parameters(relearningSteps = List(1.minute)))
    val s0Relearning = new Scheduler(Parameters(relearningSteps = List.empty))

    var card = Card()
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    // learning state tests
    val (c1, _) = s2Learning.reviewCard(card, Review(Rating.Good, now))
    assert(c1.state == State.Learning)
    assert(c1.step == 1)

    val (c2, _) = s1Learning.reviewCard(c1, Review(Rating.Again, now))
    assert(c2.state == State.Learning)
    assert(c2.step == 0)

    val (c3, _) = s0Learning.reviewCard(c2, Review(Rating.Hard, now))
    assert(c3.state == State.Review)

    // relearning state tests
    val (c4, _) = s2Relearning.reviewCard(c3, Review(Rating.Again, now))
    assert(c4.state == State.Relearning)
    assert(c4.step == 0)

    val (c5, _) = s2Relearning.reviewCard(c4, Review(Rating.Good, now))
    assert(c5.state == State.Relearning)
    assert(c5.step == 1)

    val (c6, _) = s1Relearning.reviewCard(c5, Review(Rating.Again, now))
    assert(c6.state == State.Relearning)
    assert(c6.step == 0)

    val (c7, _) = s0Relearning.reviewCard(c6, Review(Rating.Hard, now))
    assert(c7.state == State.Review)
  }

  test("maximum interval") {
    val maxInterval = 100
    val scheduler = new Scheduler(Parameters(maximumInterval = Days(maxInterval)))
    var card = Card()

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Easy, card.due))
    assert(ChronoUnit.DAYS.between(c1.lastReview.get, c1.due) <= maxInterval)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    assert(ChronoUnit.DAYS.between(c2.lastReview.get, c2.due) <= maxInterval)

    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Easy, c2.due))
    assert(ChronoUnit.DAYS.between(c3.lastReview.get, c3.due) <= maxInterval)

    val (c4, _) = scheduler.reviewCard(c3, Review(Rating.Good, c3.due))
    assert(ChronoUnit.DAYS.between(c4.lastReview.get, c4.due) <= maxInterval)
  }

  test("unique card ids") {
    val ids = (0 until 1000).map(_ => Card().id).toSet
    assert(ids.size == 1000)
  }

  test("stability lower bound") {
    val scheduler = new Scheduler(Parameters())
    val minStability = 0.001
    var card = Card()

    for (_ <- 0 until 1000) {
      val (updated, _) = scheduler.reviewCard(card, Review(Rating.Again, card.due.plusDays(1)))
      assert(updated.stability >= minStability)
      card = updated
    }
  }

  test("parameter validation - one too high") {
    val badWeights = Parameters.defaultWeights.updated(6, 100.0)
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = badWeights))
    }
  }

  test("parameter validation - one too low") {
    val badWeights = Parameters.defaultWeights.updated(10, -42.0)
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = badWeights))
    }
  }

  test("parameter validation - two bad") {
    val badWeights = Parameters.defaultWeights.updated(0, 0.0).updated(3, 101.0)
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = badWeights))
    }
  }

  test("parameter validation - empty") {
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = List.empty))
    }
  }

  test("parameter validation - too few") {
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = Parameters.defaultWeights.dropRight(1)))
    }
  }

  test("parameter validation - too many") {
    assertThrows[IllegalArgumentException] {
      new Scheduler(Parameters(weights = Parameters.defaultWeights ++ List(1.0, 2.0, 3.0)))
    }
  }

  test("learning card rate hard one learning step") {
    val firstStep = 10.minutes
    val scheduler = new Scheduler(Parameters(learningSteps = List(firstStep)))
    var card = Card()
    val initialDue = card.due

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Hard, card.due))
    assert(c1.state == State.Learning)

    val intervalMs = ChronoUnit.MILLIS.between(initialDue, c1.due)
    val expectedMs = (firstStep * 1.5).toMillis
    assert(math.abs(intervalMs - expectedMs) < 1000)
  }

  test("learning card rate hard second learning step") {
    val firstStep = 1.minute
    val secondStep = 10.minutes
    val scheduler = new Scheduler(Parameters(learningSteps = List(firstStep, secondStep)))
    var card = Card()

    assert(card.state == State.Learning)
    assert(card.step == 0)

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good, card.due))
    assert(c1.state == State.Learning)
    assert(c1.step == 1)

    val dueAfterFirst = c1.due
    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Hard, dueAfterFirst))
    val dueAfterSecond = c2.due

    assert(c2.state == State.Learning)
    assert(c2.step == 1)

    val intervalMs = ChronoUnit.MILLIS.between(dueAfterFirst, dueAfterSecond)
    val expectedMs = secondStep.toMillis
    assert(math.abs(intervalMs - expectedMs) < 1000)
  }

  test("long term stability learning state") {
    val scheduler = new Scheduler(Parameters())
    var card = Card()

    assert(card.state == State.Learning)

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Easy, card.due))
    assert(c1.state == State.Review)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Again, c1.due))
    assert(c2.state == State.Relearning)

    val nextReviewOneDayLate = c2.due.plusDays(1)
    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Good, nextReviewOneDayLate))
    assert(c3.state == State.Review)
  }

  test("relearning card rate hard one relearning step") {
    val firstStep = 10.minutes
    val scheduler = new Scheduler(Parameters(relearningSteps = List(firstStep)))
    var card = Card()

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Easy, card.due))
    assert(c1.state == State.Review)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Again, c1.due))
    assert(c2.state == State.Relearning)
    assert(c2.step == 0)

    val prevDue = c2.due
    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Hard, prevDue))
    assert(c3.state == State.Relearning)
    assert(c3.step == 0)

    val intervalMs = ChronoUnit.MILLIS.between(prevDue, c3.due)
    val expectedMs = (firstStep * 1.5).toMillis
    assert(math.abs(intervalMs - expectedMs) < 1000)
  }

  test("relearning card rate hard two relearning steps") {
    val firstStep = 1.minute
    val secondStep = 10.minutes
    val scheduler = new Scheduler(Parameters(relearningSteps = List(firstStep, secondStep)))
    var card = Card()

    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Easy, card.due))
    assert(c1.state == State.Review)

    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Again, c1.due))
    assert(c2.state == State.Relearning)
    assert(c2.step == 0)

    val prevDue = c2.due
    val (c3, _) = scheduler.reviewCard(c2, Review(Rating.Hard, prevDue))
    assert(c3.state == State.Relearning)
    assert(c3.step == 0)

    val intervalMs = ChronoUnit.MILLIS.between(prevDue, c3.due)
    val expectedMs = ((firstStep + secondStep) / 2.0).toMillis
    assert(math.abs(intervalMs - expectedMs) < 1000)

    val (c4, _) = scheduler.reviewCard(c3, Review(Rating.Good, c3.due))
    assert(c4.state == State.Relearning)
    assert(c4.step == 1)

    val prevDue2 = c4.due
    val (c5, _) = scheduler.reviewCard(c4, Review(Rating.Hard, prevDue2))
    assert(c5.state == State.Relearning)
    assert(c5.step == 1)

    val intervalMs2 = ChronoUnit.MILLIS.between(prevDue2, c5.due)
    val expectedMs2 = secondStep.toMillis
    assert(math.abs(intervalMs2 - expectedMs2) < 1000)

    val (c6, _) = scheduler.reviewCard(c5, Review(Rating.Easy, prevDue2))
    assert(c6.state == State.Review)
  }

  private def reviewToReviewState(scheduler: Scheduler, startTime: OffsetDateTime): Card = {
    var card = Card()
    val (c1, _) = scheduler.reviewCard(card, Review(Rating.Good, startTime))
    val (c2, _) = scheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    c2
  }

  test("fuzz deterministic with same seed") {
    val startTime = OffsetDateTime.now(ZoneOffset.UTC)

    val scheduler1 = new Scheduler(Parameters(), new Random(42))
    val c1 = reviewToReviewState(scheduler1, startTime)
    val prevDue1 = c1.due
    val (c1After, _) = scheduler1.reviewCard(c1, Review(Rating.Good, c1.due))
    val interval1 = ChronoUnit.DAYS.between(prevDue1, c1After.due)

    val scheduler2 = new Scheduler(Parameters(), new Random(42))
    val c2 = reviewToReviewState(scheduler2, startTime)
    val prevDue2 = c2.due
    val (c2After, _) = scheduler2.reviewCard(c2, Review(Rating.Good, c2.due))
    val interval2 = ChronoUnit.DAYS.between(prevDue2, c2After.due)

    assert(interval1 == interval2)
  }

  test("fuzz different with different seeds") {
    val startTime = OffsetDateTime.now(ZoneOffset.UTC)
    val intervals = scala.collection.mutable.ListBuffer[Long]()

    for (seed <- List(42L, 12345L, 99999L)) {
      val scheduler = new Scheduler(Parameters(), new Random(seed))
      val c = reviewToReviewState(scheduler, startTime)
      val prevDue = c.due
      val (cAfter, _) = scheduler.reviewCard(c, Review(Rating.Good, c.due))
      intervals += ChronoUnit.DAYS.between(prevDue, cAfter.due)
    }

    assert(intervals.toSet.size > 1)
  }

  test("fuzz interval within bounds") {
    val startTime = OffsetDateTime.now(ZoneOffset.UTC)
    val noFuzzScheduler = new Scheduler(Parameters(withFuzzing = false))

    // Get to Review state without fuzzing, then measure unfuzzed third-review interval
    val (c1, _) = noFuzzScheduler.reviewCard(Card(), Review(Rating.Good, startTime))
    val (c2, _) = noFuzzScheduler.reviewCard(c1, Review(Rating.Good, c1.due))
    assert(c2.state == State.Review)
    val (c3, _) = noFuzzScheduler.reviewCard(c2, Review(Rating.Good, c2.due))
    val unfuzzedInterval = ChronoUnit.DAYS.between(c3.lastReview.get, c3.due)

    // Calculate expected fuzz range
    def calcFuzzDelta(intervalDays: Int): Double = {
      var delta = 1.0
      for (range <- Parameters.fuzzRanges)
        delta += range.factor * math.max(math.min(intervalDays, range.end) - range.start, 0.0)
      delta
    }
    val delta = calcFuzzDelta(unfuzzedInterval.toInt)
    val minExpected = math.max(2, math.round(unfuzzedInterval - delta).toInt)
    val maxExpected = math.round(unfuzzedInterval + delta).toInt

    // For each seed, build same card state without fuzzing, then fuzz just the third review
    for (seed <- 0L until 100L) {
      val fuzzScheduler = new Scheduler(Parameters(), new Random(seed))
      // Use the unfuzzed c2 state but review with the fuzz scheduler
      val (fc3, _) = fuzzScheduler.reviewCard(c2, Review(Rating.Good, c2.due))
      val fuzzedInterval = ChronoUnit.DAYS.between(fc3.lastReview.get, fc3.due)

      assert(fuzzedInterval >= minExpected, s"seed=$seed: fuzzed=$fuzzedInterval < minExpected=$minExpected")
      assert(fuzzedInterval <= maxExpected, s"seed=$seed: fuzzed=$fuzzedInterval > maxExpected=$maxExpected")
    }
  }

  test("fuzz not applied to short intervals") {
    val scheduler1 = new Scheduler(Parameters(withFuzzing = false))
    val scheduler2 = new Scheduler(Parameters(), new Random(42))

    var c1 = Card()
    var c2 = Card()
    val startTime = OffsetDateTime.now(ZoneOffset.UTC)

    val (c1a, _) = scheduler1.reviewCard(c1, Review(Rating.Good, startTime))
    val (c2a, _) = scheduler2.reviewCard(c2, Review(Rating.Good, startTime))

    assert(c1a.due == c2a.due)

    val (c1b, _) = scheduler1.reviewCard(c1a, Review(Rating.Good, c1a.due))
    val (c2b, _) = scheduler2.reviewCard(c2a, Review(Rating.Good, c2a.due))

    val interval1 = ChronoUnit.DAYS.between(c1b.lastReview.get, c1b.due)
    val interval2 = ChronoUnit.DAYS.between(c2b.lastReview.get, c2b.due)

    if (interval1 < 3) {
      assert(interval1 == interval2)
    }
  }
}
