package net.codingstuffs.abilene.analytics

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.ConfigFactory
import net.codingstuffs.abilene.analytics.AnalyticsGenerationActor.Generate
import net.codingstuffs.abilene.analytics.DataAggregatorActor.{ActorDataPoint, ActorRawDataPoint, DataAggregate}
import net.codingstuffs.abilene.simulation.Group.GroupDataPoint
import org.apache.spark.sql.SparkSession

object AnalyticsGenerationActor {
  def props: Props = Props[AnalyticsGenerationActor]

  case object Generate

}

class AnalyticsGenerationActor extends Actor with ActorLogging {
  private val config = ConfigFactory.load()

  private val sparkSession: SparkSession = SparkSession.builder()
    .config("spark.cores.max", 8)
    .config("driver-memory", "16g")
    .config("spark.executor.cores", 2)
    .master("local[*]").getOrCreate()

  var actorDataPoints: Seq[ActorDataPoint] = Seq()
  var actorRawDataPoints: Seq[ActorRawDataPoint] = Seq()
  var groupDataPoints: Seq[GroupDataPoint] = Seq()

  override def receive: Receive = {
    case receipt: DataAggregate =>
      actorDataPoints = actorDataPoints ++ receipt.actorDataPoints
      actorRawDataPoints = actorRawDataPoints ++ receipt.actorRawDataPoints
      groupDataPoints = groupDataPoints ++ receipt.groupDataPoints

      if (groupDataPoints.size == config.getInt("group.count"))
        self ! Generate

    case Generate =>
      import sparkSession.implicits._
      val memberStats = actorDataPoints.toDF
      val memberPreferenceStats = actorRawDataPoints.toDF
      val groupDecisionStats = groupDataPoints.toDF

      memberStats.describe().show
      memberPreferenceStats.describe().show
      groupDecisionStats.describe().show
    //    groupDecisionCompositionAnalytics.decisionParadoxes.write.csv(s"
    //    ./data/decision_composition/$jobRunAtDateTime/decisionParadoxStats")
    //    groupDecisionStats.coalesce(1).write.json(s"
    //    ./data/decision_composition/$jobRunAtDateTime/yes_vote_counts")
    //    memberStats.write.json(s"./data/member_behavior/$jobRunAtDateTime/full")

  }
}