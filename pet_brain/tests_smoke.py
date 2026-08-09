"""Run with ``python -m pet_brain.tests_smoke`` after installing torch."""
from __future__ import annotations

from pathlib import Path
import tempfile

try:
    import torch
except ModuleNotFoundError as error:  # pragma: no cover - environment dependent
    raise SystemExit(
        "PyTorch is required for the smoke test. Install a matching build from "
        "https://pytorch.org/get-started/locally/ and run this command again."
    ) from error

from .environment.pet_world import PetWorld, WorldConfig
from .model.checkpoint import load_checkpoint, save_checkpoint
from .model.pet_brain import EmotionalPetActorCritic
from .training.ppo import PPOTrainer


def main() -> None:
    torch.manual_seed(3)
    model = EmotionalPetActorCritic()
    trainer = PPOTrainer(model)
    world = PetWorld(WorldConfig(max_steps=8, seed=3))
    before = model.gru.weight_ih_l0.detach().clone()
    metrics = trainer.train_episode(world)
    assert metrics["steps"] > 0
    assert not torch.equal(before, model.gru.weight_ih_l0.detach())
    hidden = model.initial_hidden()
    observation = world.reset()
    first = model(torch.tensor(observation).float().view(1, 1, -1), hidden)
    second = model(torch.tensor(observation).float().view(1, 1, -1), first.hidden)
    assert first.hidden.shape == second.hidden.shape
    assert not torch.equal(first.hidden, second.hidden)
    with tempfile.TemporaryDirectory() as directory:
        path = Path(directory) / "smoke.pt"
        save_checkpoint(path, model, trainer.optimizer, episode=1, stats=metrics)
        restored, payload = load_checkpoint(path)
        assert payload["episode"] == 1
        restored_output = restored(torch.tensor(observation).float().view(1, 1, -1))
        assert restored_output.action_logits.shape == (1, 1, 64)
    print("smoke_pass model_forward hidden_state gradients checkpoint_reload episode")


if __name__ == "__main__":
    main()
