package net.codingstuffs.abilene.model.decision_making.calculators

import net.codingstuffs.abilene.model.decision_making.Models.{DecisionMakingModel, NaiveRoundup, SimpleSociotropyAutonomy, SocialImpactNSL, WeightedSociotropyAutonomy}
import net.codingstuffs.abilene.model.decision_making.generators.AgentParamGenerator.DecisionParams

object ModelParamAdjuster {

  def adjust(implicit model:DecisionMakingModel, param: DecisionParams): DecisionParams = {
    val groupSize = param.groupWeights.keySet.size + 1

    model match {
      case NaiveRoundup =>
        DecisionParams(
          (param.selfParams._1, param.selfParams._2, 1),
          param.groupPreferences,
          param.groupWeights.map(weights => weights._1 -> 0.0)
        )

      case SimpleSociotropyAutonomy(sociotropy, autonomy) =>
        DecisionParams(
          (param.selfParams._1, param.selfParams._2, 1.0),
          param.groupPreferences,
          param.groupWeights.map(weights => weights._1 -> 1.0)
        )

      case WeightedSociotropyAutonomy(sociotropy, autonomy) =>
        this normalize DecisionParams(
          (param.selfParams._1, param.selfParams._2, 1.0),
          param.groupPreferences,
          param.groupWeights.map(weights => weights._1 -> weights._2 * param.groupPreferences(weights._1))
        )

      case SocialImpactNSL => ???
    }
  }

  def normalize(param: DecisionParams): DecisionParams = {
    def getNormalVal(value: Double)(implicit min_max: (Double, Double)): Double =
      (value - min_max._1) / (min_max._2 - min_max._1)

    implicit val min_max: (Double, Double) =
      ((param.groupWeights.values.toSeq :+ param.selfParams._3).min,
        (param.groupWeights.values.toSeq :+ param.selfParams._3).max)

    DecisionParams(
      (param.selfParams._1, param.selfParams._2, getNormalVal(param.selfParams._3)),
      param.groupPreferences,
      param.groupWeights.map(weights => weights._1 -> getNormalVal(weights._2))
    )
  }
}
