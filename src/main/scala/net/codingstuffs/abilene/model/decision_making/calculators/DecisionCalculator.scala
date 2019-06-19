package net.codingstuffs.abilene.model.decision_making.calculators

import net.codingstuffs.abilene.model.decision_making.Models._
import net.codingstuffs.abilene.model.decision_making.calculators.fuzzy.AgentFuzzifier
import net.codingstuffs.abilene.model.decision_making.generators.AgentParamGenerator.DecisionParams

object DecisionCalculator {
  def get(implicit model: DecisionMakingModel, params: DecisionParams): Boolean = {
    val groupMembers = params.groupWeights.keySet

    val adjustedParams = ModelParamAdjuster.adjust

    val self_val = adjustedParams.selfParams._2 * adjustedParams.selfParams._3

    val group_val = groupMembers
      .map(member =>
        adjustedParams.groupWeights(member) * adjustedParams.groupPreferences(member))
      .sum / groupMembers.size

    model match {
      case NaiveRoundup => self_val > 0.5
      case SimpleSociotropyAutonomy(sociotropy, autonomy) =>
        val agentifiedGroup = DecisionParams(("group", group_val, 1), adjustedParams.groupPreferences, adjustedParams.groupWeights)

        val compromise = AgentFuzzifier.getIntersect(model.asInstanceOf[SimpleSociotropyAutonomy],
          (adjustedParams, agentifiedGroup))

        if (compromise.y > autonomy - compromise.y) compromise.x > 0.5 else self_val > 0.5

      case WeightedSociotropyAutonomy(sociotropy, autonomy) =>
        val agentifiedGroup = DecisionParams(("group", group_val, 1), adjustedParams.groupPreferences, adjustedParams.groupWeights)

        val compromise = AgentFuzzifier.getIntersect(model.asInstanceOf[WeightedSociotropyAutonomy],
          (adjustedParams, agentifiedGroup))

        if (compromise.y > autonomy - compromise.y) compromise.x > 0.5 else self_val > 0.5

      case FuzzyCentroid =>
        val sumAreas = groupMembers
          .map(member =>
            adjustedParams.groupWeights(member) * adjustedParams.groupPreferences(member) / 2)
          .sum + (adjustedParams.selfParams._2 * adjustedParams.selfParams._3 / 2)

        val areaUntilCentroid = sumAreas / 2


    }
  }
}