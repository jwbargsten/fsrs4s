package org.bargsten.fsrs

import DateTimeUtil.Days
import DateTimeUtil.between
import util.cond

import java.time.{Clock, OffsetDateTime, ZoneOffset}
import scala.concurrent.duration.*
import scala.util.Random

class Scheduler(p: Parameters = Parameters(), rng: Random = Random())(using
    clock: Clock = Clock.systemUTC()
) {
  private val decay = -p.weights.last
  private val factor = Math.pow(0.9, 1 / decay) - 1
  private val minStability = 0.001

  def calcRetrievability(card: Card, now: OffsetDateTime = OffsetDateTime.now(clock)): Double =
    if (card.lastReview.isEmpty || card.stability <= 0.0) {
      0.0
    } else {
      val elapsedDays = Math.max(0L, card.lastReview.map(between(_, now).toDays).getOrElse(0L))
      Math.pow(1 + factor * elapsedDays / card.stability, decay)
    }

  private def updateStabilityDifficulty(review: Review)(card: Card): Card = {
    val rating = review.rating
    val daysSinceLastReview = review.daysSinceLastReview(card.lastReview)
    if (card.isNew) {
      card.copy(
        stability = initStability(rating),
        difficulty = initialDifficulty(rating)
      )
    } else if (daysSinceLastReview.exists(_ < Days.One)) {
      card.copy(
        stability = shortTermStability(card.stability, rating),
        difficulty = nextDifficulty(card.difficulty, rating)
      )
    } else {
      val retrievability = calcRetrievability(card, review.reviewedAt)
      card.copy(
        stability = nextStability(card.difficulty, card.stability, retrievability, rating),
        difficulty = nextDifficulty(card.difficulty, rating)
      )
    }
  }

  private def reviewLearning(card: Card, review: Review) = {
    val baseCard = updateStabilityDifficulty(review)(card)

    if (p.learningSteps.isEmpty || (card.step >= p.learningSteps.size && review.rating.isPositive)) {
      val updatedCard = baseCard.copy(state = State.Review, step = 0)
      val nextInterval = calcNextInterval(updatedCard.stability).toDuration
      (updatedCard, nextInterval)
    } else {
      review.rating match {
        case Rating.Again =>
          val updatedCard = baseCard.copy(step = 0)
          val nextInterval = p.learningSteps.head
          (updatedCard, nextInterval)

        case Rating.Hard =>
          if (card.step == 0 && p.learningSteps.size == 1) {
            val nextInterval = p.learningSteps.head * 1.5
            (baseCard, nextInterval)
          } else if (card.step == 0 && p.learningSteps.size >= 2) {
            val nextInterval = (p.learningSteps(0) + p.learningSteps(1)) / 2.0
            (baseCard, nextInterval)
          } else {
            val nextInterval = p.learningSteps(card.step)
            (baseCard, nextInterval)
          }

        case Rating.Good =>
          if (card.step + 1 == p.learningSteps.size) {
            val updatedCard = baseCard.copy(state = State.Review, step = 0)
            val nextInterval = calcNextInterval(updatedCard.stability).toDuration
            (updatedCard, nextInterval)
          } else {
            val updatedCard = baseCard.copy(step = card.step + 1)
            val nextInterval = p.learningSteps(updatedCard.step)
            (updatedCard, nextInterval)
          }

        case Rating.Easy =>
          val updatedCard = baseCard.copy(state = State.Review, step = 0)
          val nextInterval = calcNextInterval(updatedCard.stability).toDuration
          (updatedCard, nextInterval)
      }
    }
  }

  private def reviewReview(card: Card, review: Review) = {
    val updatedCard = if (review.daysSinceLastReview(card.lastReview).exists(_ < Days.One)) {
      card.copy(
        stability = shortTermStability(card.stability, review.rating),
        difficulty = nextDifficulty(card.difficulty, review.rating)
      )
    } else {
      val retrievability = calcRetrievability(card, review.reviewedAt)
      card.copy(
        stability = nextStability(card.difficulty, card.stability, retrievability, review.rating),
        difficulty = nextDifficulty(card.difficulty, review.rating)
      )
    }

    review.rating match {
      case Rating.Again =>
        if (p.relearningSteps.isEmpty) {
          val nextInterval = calcNextInterval(updatedCard.stability).toDuration
          (updatedCard, nextInterval)
        } else {
          val nextInterval = p.relearningSteps.head
          (updatedCard.copy(state = State.Relearning, step = 0), nextInterval)
        }

      case Rating.Hard | Rating.Good | Rating.Easy =>
        val nextInterval = calcNextInterval(updatedCard.stability).toDuration
        (updatedCard, nextInterval)
    }

  }

  private def reviewRelearning(card: Card, review: Review) = {
    var updatedCard = card
    val daysSinceLastReview = review.daysSinceLastReview(card.lastReview)
    var nextInterval: Duration = Duration.Zero

    if (daysSinceLastReview.exists(_ < Days.One)) {
      updatedCard = updatedCard.copy(
        stability = shortTermStability(card.stability, review.rating),
        difficulty = nextDifficulty(card.difficulty, review.rating)
      )
    } else {
      val retrievability = calcRetrievability(card, review.reviewedAt)
      updatedCard = updatedCard.copy(
        stability = nextStability(card.difficulty, card.stability, retrievability, review.rating),
        difficulty = nextDifficulty(card.difficulty, review.rating)
      )
    }

    if (p.relearningSteps.isEmpty || (card.step >= p.relearningSteps.size && review.rating.isPositive)) {
      updatedCard = updatedCard.copy(state = State.Review, step = 0)
      nextInterval = calcNextInterval(updatedCard.stability).toDuration
    } else {
      review.rating match {
        case Rating.Again =>
          updatedCard = updatedCard.copy(step = 0)
          nextInterval = p.relearningSteps.head

        case Rating.Hard =>
          if (card.step == 0 && p.relearningSteps.size == 1) {
            nextInterval = p.relearningSteps.head * 1.5
          } else if (card.step == 0 && p.relearningSteps.size >= 2) {
            nextInterval = (p.relearningSteps(0) + p.relearningSteps(1)) / 2.0
          } else {
            nextInterval = p.relearningSteps(card.step)
          }

        case Rating.Good =>
          if (card.step + 1 == p.relearningSteps.size) {
            updatedCard = updatedCard.copy(state = State.Review, step = 0)
            nextInterval = calcNextInterval(updatedCard.stability).toDuration
          } else {
            updatedCard = updatedCard.copy(step = card.step + 1)
            nextInterval = p.relearningSteps(updatedCard.step)
          }

        case Rating.Easy =>
          updatedCard = updatedCard.copy(state = State.Review, step = 0)
          nextInterval = calcNextInterval(updatedCard.stability).toDuration
      }
    }

    (updatedCard, nextInterval)
  }

  def reviewCard(card: Card, rating: Rating): (Card, ReviewLogEntry) = reviewCard(card, Review(rating))

  def reviewCard(
      card: Card,
      review: Review
  ): (Card, ReviewLogEntry) = {

    val (updatedCard, nextInterval) = (card.state match {
      case State.Learning   => reviewLearning(card, review)
      case State.Review     => reviewReview(card, review)
      case State.Relearning => reviewRelearning(card, review)
    }).cond {
      case (card, nextInterval) if p.withFuzzing && card.state == State.Review =>
        (card, getFuzzedInterval(nextInterval))
    }

    (
      updatedCard.copy(
        due = review.reviewedAt.plusNanos(nextInterval.toNanos),
        lastReview = Some(review.reviewedAt),
        firstReview = card.firstReview.orElse(Some(review.reviewedAt))
      ),
      ReviewLogEntry.fromReview(card, review)
    )
  }

  private def getFuzzedInterval(interval: Duration): Duration = {
    val intervalDays = interval.toDays.toInt
    if (intervalDays < 2.5) return interval

    def getFuzzRange(intervalDays: Int): (Int, Int) = {
      var delta = 1.0
      for (range <- Parameters.fuzzRanges) {
        delta += range.factor * math.max(math.min(intervalDays, range.end) - range.start, 0.0)
      }
      val minIvl = math.round(intervalDays - delta).toInt
      val maxIvl = math.round(intervalDays + delta).toInt
      val clampedMin = math.max(2, minIvl)
      val clampedMax = math.min(maxIvl, p.maximumInterval.unwrap)
      (math.min(clampedMin, clampedMax), clampedMax)
    }

    val (minIvl, maxIvl) = getFuzzRange(intervalDays)
    val fuzzedDays = math.min(
      math.round(rng.nextDouble() * (maxIvl - minIvl + 1) + minIvl).toInt,
      p.maximumInterval.unwrap
    )
    Days(fuzzedDays).toDuration
  }

  private def initStability(r: Rating) = clampStability(p.weights(r.value - 1))

  private def clampStability(s: Double) = math.max(s, minStability)

  private def initialDifficulty(r: Rating, clamp: Boolean = true): Double = {
    val d = p.weights(4) - math.exp(p.weights(5) * (r.value - 1)) + 1
    if (clamp) clampDifficulty(d) else d
  }

  def clampDifficulty(d: Double): Double = math.min(math.max(d, 1), 10)

  private def calcNextInterval(stability: Double): Days = {
    val newInterval = stability / factor * (math.pow(p.desiredRetention, 1 / decay) - 1)
    Days(math.min(math.max(newInterval.round.toInt, 1), p.maximumInterval.unwrap))
  }

  private def nextDifficulty(difficulty: Double, rating: Rating) = {
    val deltaDifficulty = -(p.weights(6) * (rating.value - 3))
    val linearDamping = (10.0 - difficulty) * deltaDifficulty / 9.0
    val nextD = meanReversion(initialDifficulty(Rating.Easy, clamp = false), difficulty + linearDamping)
    clampDifficulty(nextD)
  }

  private def nextStability(difficulty: Double, stability: Double, retrievability: Double, rating: Rating) = {
    val ns = rating match {
      case Rating.Again => nextForgetStability(difficulty, stability, retrievability)
      case _            => nextRecallStability(difficulty, stability, retrievability, rating)
    }
    clampStability(ns)
  }

  private def shortTermStability(stability: Double, rating: Rating) = {
    var increase =
      math.exp(p.weights(17) * (rating.value - 3 + p.weights(18))) * math.pow(stability, -p.weights(19))
    if (Set(Rating.Good, Rating.Easy).contains(rating)) {
      increase = math.max(increase, 1.0)
    }
    clampStability(stability * increase)
  }

  private def meanReversion(initDifficulty: Double, nextDifficulty: Double) =
    p.weights(7) * initDifficulty + (1 - p.weights(7)) * nextDifficulty

  private def nextForgetStability(difficulty: Double, stability: Double, retrievability: Double) = {
    val longTermParams = p.weights(11) *
      math.pow(difficulty, -p.weights(12)) *
      (math.pow(stability + 1, p.weights(13)) - 1) *
      math.exp((1 - retrievability) * p.weights(14))
    val shortTermParams = stability / math.exp(p.weights(17) * p.weights(18))
    math.min(longTermParams, shortTermParams)
  }

  private def nextRecallStability(
      difficulty: Double,
      stability: Double,
      retrievability: Double,
      rating: Rating
  ) = {
    val hardPenalty = if (rating == Rating.Hard) p.weights(15) else 1
    val easyBonus = if (rating == Rating.Easy) p.weights(16) else 1
    stability * (1 +
      math.exp(p.weights(8)) *
      (11 - difficulty) *
      math.pow(stability, -p.weights(9)) *
      (math.exp((1 - retrievability) * p.weights(10)) - 1) *
      hardPenalty * easyBonus)
  }
}
