package net.codingstuffs.abilene.simulation.generators.random

import net.codingstuffs.abilene.intake.parse.ConfigUtil
import org.apache.commons.math3.distribution.BetaDistribution

import scala.util.Random

object Beta {
  def GENERATOR(alpha: Double, beta: Double) = new Beta(alpha, beta)
}

class Beta(alpha: Double, beta: Double) extends Random {
  self.setSeed(ConfigUtil.GENERATOR_SEED)

  override def nextDouble: Double =
    new BetaDistribution(alpha, beta).inverseCumulativeProbability(super.nextDouble)
}