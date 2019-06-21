package net.codingstuffs.abilene.analytics

import java.time.format.DateTimeFormatter
import java.util.Calendar

import akka.actor.{Actor, ActorLogging, Props}
import net.codingstuffs.abilene.analytics.DataAggregatorActor.{ActorDataPoint, CreateDump}
import net.codingstuffs.abilene.model.decision_making.generators.AgentParamGenerator.DecisionParams
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.sum
import org.apache.spark.sql.types.IntegerType

object DataAggregatorActor {
  def props: Props = Props[DataAggregatorActor]

  case class ActorRawDataPoint(groupId: String,
                               decisionParams: DecisionParams,
                               decision: Boolean)

  case class ActorDataPoint(groupId: String,
                            selfPreference: Double,
                            selfWeight: Double,
                            groupPreference: Seq[Double],
                            groupWeights: Seq[Double],
                            decision: Boolean)

  case class CreateDump()

}

class DataAggregatorActor extends Actor with ActorLogging {
  var actorDataPoints: Seq[ActorDataPoint] = Seq()

  val sparkSession: SparkSession = SparkSession.builder()
    .config("spark.cores.max", 8)
    .config("spark.executor.cores", 2)
    .master("local[*]").getOrCreate()

  override def receive: Receive = {
    case dataPoint: ActorDataPoint =>
      actorDataPoints = actorDataPoints :+ dataPoint

    case CreateDump =>
      import sparkSession.implicits._
      val memberStats = actorDataPoints.toDF()

      val groupDecisionCompositionAnalytics = new GroupDecisionComposition(memberStats)
      val memberBehaviorAnalytics = new MemberBehavior(memberStats)

      val jobRunAtDateTime = Calendar.getInstance.getTimeInMillis

      val groupDecisionStats = groupDecisionCompositionAnalytics
        .getYesVoteCounts
        .orderBy("acceptance")

//      val memberBehaviorStats = memberBehaviorAnalytics
//      .averagedPreferenceKnowledge

      memberStats.show(5, truncate = false)
      groupDecisionStats.show(false)
      groupDecisionCompositionAnalytics.preferencePerMember.show(50, truncate = false)

      groupDecisionCompositionAnalytics.decisionParadoxes.show

      val abileneIndex = groupDecisionCompositionAnalytics.decisionParadoxes.agg(sum("counts")).first().get(0).asInstanceOf[Long] /
        groupDecisionCompositionAnalytics.decisionParadoxes.count.toDouble

      log.info(groupDecisionCompositionAnalytics.decisionParadoxes.filter($"counts" === 4).count.toString)
      log.info(abileneIndex.toString)
//    groupDecisionCompositionAnalytics.decisionParadoxes.write.csv(s"./data/decision_composition/$jobRunAtDateTime/decisionParadoxStats")
//    groupDecisionStats.coalesce(1).write.json(s"./data/decision_composition/$jobRunAtDateTime/yes_vote_counts")
//    memberStats.write.json(s"./data/member_behavior/$jobRunAtDateTime/full")

  }
}
