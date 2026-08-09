"""Run PPO training: ``python -m pet_brain.training.train --episodes 10``."""
from __future__ import annotations

import argparse
from pathlib import Path
import random

import torch

from ..environment.pet_world import PetWorld, WorldConfig
from ..model.checkpoint import load_checkpoint, save_checkpoint
from ..model.pet_brain import EmotionalPetActorCritic
from .ppo import PPOConfig, PPOTrainer


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train Curio's artificial pet brain with recurrent PPO.")
    parser.add_argument("--episodes", type=int, default=100)
    parser.add_argument("--steps", type=int, default=None, help="Optional global step budget.")
    parser.add_argument("--resume", type=Path, default=None)
    parser.add_argument("--checkpoint-dir", type=Path, default=Path("pet_brain/checkpoints"))
    parser.add_argument("--seed", type=int, default=7)
    parser.add_argument("--device", default="cpu")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    random.seed(args.seed)
    torch.manual_seed(args.seed)
    model = EmotionalPetActorCritic()
    trainer = PPOTrainer(model, PPOConfig(), device=args.device)
    start_episode = 0
    if args.resume:
        model, payload = load_checkpoint(args.resume, args.device)
        trainer.model = model
        trainer.optimizer = torch.optim.Adam(trainer.model.parameters(), lr=trainer.config.learning_rate)
        if "optimizer_state_dict" in payload:
            trainer.optimizer.load_state_dict(payload["optimizer_state_dict"])
        start_episode = int(payload.get("episode", 0))
    world = PetWorld(WorldConfig(seed=args.seed))
    args.checkpoint_dir.mkdir(parents=True, exist_ok=True)
    total_steps = 0
    for episode in range(start_episode, start_episode + args.episodes):
        metrics = trainer.train_episode(world)
        total_steps += int(metrics["steps"])
        print(
            f"episode={episode + 1} step={total_steps} reward={metrics['reward']:.3f} "
            f"loss={metrics['loss']:.4f} policy={metrics['policy_loss']:.4f} "
            f"value={metrics['value_loss']:.4f} entropy={metrics['entropy']:.3f} "
            f"curiosity={metrics['curiosity_reward']:.3f}"
        )
        save_checkpoint(args.checkpoint_dir / "latest.pt", trainer.model, trainer.optimizer, episode=episode + 1, stats=metrics)
        if (episode + 1) % 10 == 0:
            save_checkpoint(args.checkpoint_dir / f"episode_{episode + 1:04d}.pt", trainer.model, trainer.optimizer, episode=episode + 1, stats=metrics)
        if args.steps is not None and total_steps >= args.steps:
            break


if __name__ == "__main__":
    main()
