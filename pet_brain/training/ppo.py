"""Small, readable recurrent PPO trainer for the artificial pet world."""
from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Any

import torch
from torch import Tensor
from torch.distributions import Categorical

from ..environment.pet_world import PetWorld
from ..environment.rewards import curiosity_reward
from ..model.pet_brain import EmotionalPetActorCritic
from .replay_buffer import Trajectory, Transition


@dataclass
class PPOConfig:
    gamma: float = 0.99
    gae_lambda: float = 0.95
    clip_epsilon: float = 0.2
    entropy_coefficient: float = 0.01
    value_coefficient: float = 0.5
    prediction_coefficient: float = 0.05
    curiosity_coefficient: float = 0.2
    learning_rate: float = 3e-4
    update_epochs: int = 2
    max_grad_norm: float = 0.5


class PPOTrainer:
    def __init__(self, model: EmotionalPetActorCritic, config: PPOConfig | None = None, device: str = "cpu") -> None:
        self.model = model.to(device)
        self.config = config or PPOConfig()
        self.device = torch.device(device)
        self.optimizer = torch.optim.Adam(self.model.parameters(), lr=self.config.learning_rate)

    @torch.no_grad()
    def choose_action(self, observation: list[float], hidden: Tensor | None) -> tuple[int, float, float, Tensor, Any]:
        obs = torch.tensor(observation, dtype=torch.float32, device=self.device).view(1, 1, -1)
        output = self.model(obs, hidden)
        distribution = Categorical(logits=output.action_logits[:, -1])
        action = distribution.sample()
        # Recompute only the action-conditioned auxiliary prediction. The GRU
        # state/logits remain identical, but curiosity must compare the outcome
        # predicted for the action that was actually sampled.
        output = self.model(obs, hidden, action.view(1, 1))
        return (
            int(action.item()),
            float(distribution.log_prob(action).item()),
            float(output.value[:, -1].item()),
            output.hidden.detach(),
            output,
        )

    def collect_episode(self, world: PetWorld) -> tuple[Trajectory, dict[str, float]]:
        observation = world.reset()
        trajectory = Trajectory()
        hidden = self.model.initial_hidden(1, self.device)
        trajectory.initial_hidden = hidden.detach().clone()
        total_reward = 0.0
        total_curiosity = 0.0
        for _ in range(world.config.max_steps):
            action, old_log_prob, value, next_hidden, output = self.choose_action(observation, hidden)
            next_observation, reward, done, info = world.step(action)
            predicted = output.predicted_next_state[0, -1].detach().cpu().tolist()
            intrinsic = curiosity_reward(predicted, next_observation, float(info["novel"]))
            combined_reward = reward + self.config.curiosity_coefficient * intrinsic
            trajectory.append(Transition(observation, action, old_log_prob, value, combined_reward, done, next_observation, intrinsic, info))
            total_reward += combined_reward
            total_curiosity += intrinsic
            observation, hidden = next_observation, next_hidden
            if done:
                break
        trajectory.final_hidden = hidden.detach().clone()
        return trajectory, {"reward": total_reward, "curiosity_reward": total_curiosity, "steps": float(len(trajectory))}

    def _advantages(self, trajectory: Trajectory) -> tuple[Tensor, Tensor]:
        rewards = [item.reward for item in trajectory.transitions]
        # The final observation is bootstrapped unless the environment ended
        # for a true terminal condition. A max-step cutoff is a time-limit
        # truncation, so retaining its value avoids throwing away the tail.
        final = trajectory.transitions[-1]
        if final.done and final.info.get("terminal", False):
            final_value = 0.0
        else:
            with torch.no_grad():
                observation = torch.tensor(final.next_observation, dtype=torch.float32, device=self.device).view(1, 1, -1)
                hidden = trajectory.final_hidden.detach().to(self.device) if isinstance(trajectory.final_hidden, Tensor) else None
                final_value = float(self.model(observation, hidden).value[:, -1].item())
        values = [item.value for item in trajectory.transitions] + [final_value]
        advantages = [0.0] * len(rewards)
        last = 0.0
        for index in reversed(range(len(rewards))):
            nonterminal = 0.0 if trajectory.transitions[index].done else 1.0
            delta = rewards[index] + self.config.gamma * values[index + 1] * nonterminal - values[index]
            last = delta + self.config.gamma * self.config.gae_lambda * nonterminal * last
            advantages[index] = last
        advantage_tensor = torch.tensor(advantages, dtype=torch.float32, device=self.device)
        returns = advantage_tensor + torch.tensor(values[:-1], dtype=torch.float32, device=self.device)
        return (advantage_tensor - advantage_tensor.mean()) / (advantage_tensor.std() + 1e-8), returns

    def update(self, trajectory: Trajectory) -> dict[str, float]:
        observations = torch.tensor([item.observation for item in trajectory.transitions], dtype=torch.float32, device=self.device).unsqueeze(0)
        actions = torch.tensor([item.action for item in trajectory.transitions], dtype=torch.long, device=self.device).unsqueeze(0)
        old_log_probs = torch.tensor([item.log_probability for item in trajectory.transitions], dtype=torch.float32, device=self.device).unsqueeze(0)
        advantages, returns = self._advantages(trajectory)
        hidden = trajectory.initial_hidden.detach().to(self.device) if isinstance(trajectory.initial_hidden, Tensor) else self.model.initial_hidden(1, self.device)
        metrics: dict[str, float] = {}
        for _ in range(self.config.update_epochs):
            output = self.model(observations, hidden, actions)
            distribution = Categorical(logits=output.action_logits.squeeze(0))
            log_probs = distribution.log_prob(actions.squeeze(0))
            ratio = (log_probs - old_log_probs.squeeze(0)).exp()
            surrogate_a = ratio * advantages
            surrogate_b = ratio.clamp(1.0 - self.config.clip_epsilon, 1.0 + self.config.clip_epsilon) * advantages
            policy_loss = -torch.minimum(surrogate_a, surrogate_b).mean()
            value_loss = 0.5 * (output.value.squeeze(0) - returns).pow(2).mean()
            next_states = torch.tensor([item.next_observation for item in trajectory.transitions], dtype=torch.float32, device=self.device)
            prediction_loss = (output.predicted_next_state.squeeze(0) - next_states).pow(2).mean()
            entropy = distribution.entropy().mean()
            loss = policy_loss + self.config.value_coefficient * value_loss + self.config.prediction_coefficient * prediction_loss - self.config.entropy_coefficient * entropy
            self.optimizer.zero_grad(set_to_none=True)
            loss.backward()
            torch.nn.utils.clip_grad_norm_(self.model.parameters(), self.config.max_grad_norm)
            self.optimizer.step()
            metrics = {"loss": float(loss.item()), "policy_loss": float(policy_loss.item()), "value_loss": float(value_loss.item()), "prediction_loss": float(prediction_loss.item()), "entropy": float(entropy.item())}
        return metrics

    def train_episode(self, world: PetWorld) -> dict[str, float]:
        trajectory, rollout = self.collect_episode(world)
        metrics = self.update(trajectory)
        metrics.update(rollout)
        return metrics
