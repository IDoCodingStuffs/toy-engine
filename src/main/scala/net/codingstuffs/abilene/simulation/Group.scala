package net.codingstuffs.abilene.simulation

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Props}
import akka.util.Timeout

import scala.concurrent.duration.{Duration, FiniteDuration}
import net.codingstuffs.abilene.analytics.DataAggregatorActor.{ActorDataPoint, ActorRawDataPoint}
import net.codingstuffs.abilene.simulation.Abilene.{husband, mother, system, wife}
import net.codingstuffs.abilene.simulation.Member.{Declare, ReceiveDecision}
import net.codingstuffs.abilene.simulation.decision_making.models.AgentParamGenerator.DecisionParams

import scala.concurrent.{Await, ExecutionContext, Future}

object Group {
  def props: Props = Props[Group]

  def props(members: Set[String], dataDumpGenerator: ActorRef): Props = Props(new Group(members, dataDumpGenerator))

  case class DataPoint(declare: Declare, memberParams: DecisionParams)

}

class Group(members: Set[String], dataDumpGenerator: ActorRef) extends Actor with ActorLogging {

  import Group._

  import akka.pattern.{ask, pipe}

  // implicit ExecutionContext should be in scope
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = Duration.create(5, "seconds")

  val groupId = self.path.name.split("---")(0)
  val group: ActorSelection = system.actorSelection(s"/user/$groupId@@@*")

  def receive: PartialFunction[Any, Unit] = {
    case DataPoint(Declare(decision), params: DecisionParams) =>
      val memberName = sender().path.name.split("@@@")(1)
      var decisionFuture: Future[Any] = null

      dataDumpGenerator !
        ActorDataPoint(groupId, memberName, params.selfParams._2, params.selfParams._3,
          params.groupPreferences.values.toSeq, params.groupWeights.values.toSeq, decision)
      dataDumpGenerator !
        ActorRawDataPoint(groupId, memberName, params, decision)

      group ? ReceiveDecision(memberName, decision)

      if (memberName == "father") decisionFuture = {
        system.actorSelection(s"/user/$groupId@@@wife*") ? Declare
      }
      if (memberName == "wife") decisionFuture = {
        system.actorSelection(s"/user/$groupId@@@husband*") ? Declare
      }
      if (memberName == "husband") decisionFuture = {
        system.actorSelection(s"/user/$groupId@@@mother*") ? Declare
      }
  }
}