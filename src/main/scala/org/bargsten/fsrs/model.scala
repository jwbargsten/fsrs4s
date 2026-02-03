package org.bargsten.fsrs

import DateTimeUtil.{Days, between}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration.*

opaque type CardId = String

object CardId {
  def apply(uuid: UUID): CardId = uuid.toString
  def apply(value: String): CardId = value

  def generate(): CardId = CardId(UUID.randomUUID())

  extension (id: CardId) {
    def unwrap: String = id
  }
}

case class Review(
    rating: Rating,
    reviewedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    reviewDuration: Option[Duration] = None
) {
  def daysSinceLastReview(lastReview: Option[OffsetDateTime]): Option[Days] =
    lastReview.map(between(_, reviewedAt)).map(Days.apply)
}

enum State(val value: Int):
  case Learning extends State(1)
  case Review extends State(2)
  case Relearning extends State(3)

enum Rating(val value: Int) extends Enum[Rating]:
  case Again extends Rating(1)
  case Hard extends Rating(2)
  case Good extends Rating(3)
  case Easy extends Rating(4)

  def isPositive: Boolean = Rating.positive.contains(this)

object Rating {
  val positive: Set[Rating] = Set(Rating.Hard, Rating.Good, Rating.Easy)
}

case class ReviewLogEntry(
    cardId: CardId,
    rating: Rating,
    reviewedAt: OffsetDateTime,
    duration: Option[Duration]
)
object ReviewLogEntry {
  def fromReview(card: Card, review: Review) =
    ReviewLogEntry(card.id, review.rating, review.reviewedAt, review.reviewDuration)
}

case class Card(
    id: CardId = CardId.generate(),
    state: State = State.Learning,
    due: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    stability: Double = 0.0,
    difficulty: Double = 0.0,
    step: Int = 0,
    lastReview: Option[OffsetDateTime] = None,
    firstReview: Option[OffsetDateTime] = None
) {
  lazy val isNew: Boolean = lastReview.isEmpty
}

case class Parameters(
    weights: Seq[Double] = Parameters.defaultWeights,
    desiredRetention: Double = 0.9,
    learningSteps: List[Duration] = List(1.minute, 10.minutes),
    relearningSteps: List[Duration] = List(10.minutes),
    maximumInterval: Days = Days(36500),
    withFuzzing: Boolean = true
) {
  require(Parameters.validate(weights).isRight, "weights are not valid")
}
case class FuzzRange(start: Double, end: Double, factor: Double)

object Parameters {
  val defaultDecay = 0.1542

  /*
  - w0-w3: initial stability per rating
  - w4-w7: difficulty calculations
  - w8-w16: stability calculations for recall
  - w17-w19: short-term stability parameters
  - w20: decay
   */

  lazy val defaultWeights: Seq[Double] =
    List(
      0.212,
      1.2931,
      2.3065,
      8.2956,
      6.4133,
      0.8334,
      3.0194,
      0.001,
      1.8722,
      0.1666,
      0.796,
      1.4835,
      0.0614,
      0.2629,
      1.6483,
      0.6014,
      1.8729,
      0.5425,
      0.0912,
      0.0658,
      defaultDecay
    )

  val fuzzRanges = List(
    FuzzRange(2.5, 7.0, 0.15),
    FuzzRange(7.0, 20.0, 0.1),
    FuzzRange(20.0, Double.PositiveInfinity, 0.05)
  )

  private val stabilityMin = 0.001
  private val stabilityMax = 100.0

  val lowerBounds: Seq[Double] = List(
    stabilityMin,
    stabilityMin,
    stabilityMin,
    stabilityMin,
    1.0,
    0.001,
    0.001,
    0.001,
    0.0,
    0.0,
    0.001,
    0.001,
    0.001,
    0.001,
    0.0,
    0.0,
    1.0,
    0.0,
    0.0,
    0.0,
    0.1
  )

  val upperBounds: Seq[Double] = List(
    stabilityMax,
    stabilityMax,
    stabilityMax,
    stabilityMax,
    10.0,
    4.0,
    4.0,
    0.75,
    4.5,
    0.8,
    3.5,
    5.0,
    0.25,
    0.9,
    4.0,
    1.0,
    6.0,
    2.0,
    2.0,
    0.8,
    0.8
  )

  def validate(weights: Seq[Double]): Either[List[String], Unit] = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    if (weights.size != lowerBounds.size)
      errors += s"Expected ${lowerBounds.size} weights, got ${weights.size}"
    else
      for ((w, i) <- weights.zipWithIndex) {
        val (lo, hi) = (lowerBounds(i), upperBounds(i))
        if (w < lo || w > hi)
          errors += s"weights($i) = $w out of bounds ($lo, $hi)"
      }
    if (errors.isEmpty) Right(()) else Left(errors.toList)
  }
}
