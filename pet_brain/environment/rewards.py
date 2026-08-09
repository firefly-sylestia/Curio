"""Reward shaping used by the small curriculum environment.

These are deliberately modest shaping signals. The policy still has to learn
which primitive actions work; reward terms do not directly select behavior.
"""
from dataclasses import dataclass


@dataclass
class RewardBreakdown:
    need: float = 0.0
    social: float = 0.0
    exploration: float = 0.0
    failure: float = 0.0
    curiosity: float = 0.0

    @property
    def total(self) -> float:
        return self.need + self.social + self.exploration + self.failure + self.curiosity


def curiosity_reward(predicted: list[float], actual: list[float], novelty: float) -> float:
    if not predicted or not actual:
        return 0.0
    error = sum((a - b) ** 2 for a, b in zip(predicted, actual)) / len(actual)
    return min(0.25, 0.05 * error + 0.05 * novelty)
