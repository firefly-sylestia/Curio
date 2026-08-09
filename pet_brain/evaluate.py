"""Evaluate adaptation probes; results are measurements, not claims of learning."""
from __future__ import annotations

import argparse
from statistics import mean

import torch
from torch.distributions import Categorical

from .environment.pet_world import PetWorld, WorldConfig
from .model.checkpoint import load_checkpoint


def run_episode(model, world: PetWorld, steps: int) -> tuple[float, list[str]]:
    observation = world.reset()
    hidden = model.initial_hidden()
    reward, events = 0.0, []
    for _ in range(steps):
        with torch.no_grad():
            output = model(torch.tensor(observation).float().view(1, 1, -1), hidden)
            action = Categorical(logits=output.action_logits[:, -1]).sample()
            hidden = output.hidden
        observation, step_reward, done, info = world.step(int(action.item()))
        reward += step_reward
        events.append(info["event"])
        if done:
            break
    return reward, events


def main() -> None:
    parser = argparse.ArgumentParser(description="Run non-scripted adaptation probes.")
    parser.add_argument("checkpoint")
    parser.add_argument("--episodes", type=int, default=5)
    parser.add_argument("--steps", type=int, default=96)
    args = parser.parse_args()
    model, _ = load_checkpoint(args.checkpoint)
    model.eval()
    scores = []
    for seed in range(args.episodes):
        score, events = run_episode(model, PetWorld(WorldConfig(max_steps=args.steps, seed=seed)), args.steps)
        scores.append(score)
        print(f"seed={seed} reward={score:.3f} unique_events={len(set(events))}")
    print(f"mean_reward={mean(scores):.3f}")


if __name__ == "__main__":
    main()
