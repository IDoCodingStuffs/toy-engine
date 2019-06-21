package net.codingstuffs.abilene.analytics

import java.util.Calendar

import akka.actor.{Actor, ActorLogging, Props}
import net.codingstuffs.abilene.analytics.DataAggregatorActor.{ActorDataPoint, CreateDump}
import net.codingstuffs.abilene.model.decision_making.generators.AgentParamGenerator.DecisionParams
import org.apache.spark.sql.SparkSession

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
      memberStats.show(5, truncate = false)

      val groupDecisionCompositionAnalytics = new GroupDecisionComposition(memberStats)
      val memberBehaviorAnalytics = new MemberBehavior(memberStats)

      val jobRunAtDateTime = Calendar.getInstance.getTime

      val groupDecisionStats = groupDecisionCompositionAnalytics
        .getYesVoteCounts
        .orderBy("acceptance")

//      val memberBehaviorStats = memberBehaviorAnalytics
//      .averagedPreferenceKnowledge

      memberStats.show(5, truncate = false)

    //groupDecisionStats.write.csv(s"data/decision_composition/$jobRunAtDateTime/yes_vote_counts.csv")
  }
}
