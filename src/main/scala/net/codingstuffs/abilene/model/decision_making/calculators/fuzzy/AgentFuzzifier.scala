package net.codingstuffs.abilene.model.decision_making.calculators.fuzzy

import net.codingstuffs.abilene.model.decision_making.Models.SimpleSociotropyAutonomy
import net.codingstuffs.abilene.model.decision_making.generators.AgentParamGenerator.DecisionParams

object AgentFuzzifier {

  case class Point(x: Double, y: Double)

  case class Line(p1: Point, p2: Point)

  def getAgentLines(implicit model: SimpleSociotropyAutonomy, param: DecisionParams): (Line, Line) = {
    val peakPoint = Point(param.selfParams._2, model.autonomy)
    val leftPoint = Point(param.selfParams._2 - model.sociotropy / 2, 0)
    val rightPoint = Point(param.selfParams._2 + model.sociotropy / 2, 0)

    (Line(leftPoint, peakPoint), Line(peakPoint, rightPoint))
  }

  def getIntersect(implicit model: SimpleSociotropyAutonomy,
                   params: (DecisionParams, DecisionParams)): Point = {
    val agent1 =
      if (params._1.selfParams._2 < params._2.selfParams._2) params._1 else params._2
    val agent2 = if (params._1.selfParams._2 >= params._2.selfParams._2) params._1 else params._2

    lineIntersect(getAgentLines(model, agent1)._2, getAgentLines(model, agent2)._1)
  }

  def lineIntersect(l1: Line, l2: Line): Point = {
    val a1 = l1.p2.y - l1.p1.y
    val b1 = l1.p1.x - l1.p2.x
    val c1 = a1 * l1.p1.x + b1 * l1.p1.y

    val a2 = l2.p2.y - l2.p1.y
    val b2 = l2.p1.x - l2.p2.x
    val c2 = a2 * l2.p1.x + b2 * l2.p1.y

    val delta = a1 * b2 - a2 * b1
    // If lines are parallel, intersection point will contain infinite values
    Point((b2 * c1 - b1 * c2) / delta, (a1 * c2 - a2 * c1) / delta)
  }
}