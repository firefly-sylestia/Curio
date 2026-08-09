"""Run an episode from a checkpoint: ``python -m pet_brain.inference.run_pet path``."""
from __future__ import annotations

import argparse

from ..environment.pet_world import PetWorld, WorldConfig
from ..model.checkpoint import load_checkpoint
from .speech import speech_for_state


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint")
    parser.add_argument("--steps", type=int, default=32)
    args = parser.parse_args()
    model, payload = load_checkpoint(args.checkpoint)
    world = PetWorld(WorldConfig(max_steps=args.steps, seed=11))
    observation = world.reset()
    hidden = model.initial_hidden()
    total = 0.0
    for step in range(args.steps):
        action, log_prob, value, hidden, output = _choose(model, observation, hidden)
        observation, reward, done, info = world.step(action)
        emotion = output.emotion[0, -1].detach().tolist()
        needs = output.needs[0, -1].detach().tolist()
        total += reward
        print(f"step={step + 1} action={action} event={info['event']} reward={reward:.3f} speech={speech_for_state(action, emotion, needs)}")
        if done:
            break
    print(f"episode_reward={total:.3f} checkpoint_episode={payload.get('episode', 0)}")


def _choose(model, observation, hidden):
    import torch
    from torch.distributions import Categorical
    with torch.no_grad():
        output = model(torch.tensor(observation, dtype=torch.float32).view(1, 1, -1), hidden)
        distribution = Categorical(logits=output.action_logits[:, -1])
        action = distribution.sample()
    return int(action.item()), float(distribution.log_prob(action).item()), float(output.value.item()), output.hidden, output


if __name__ == "__main__":
    main()
