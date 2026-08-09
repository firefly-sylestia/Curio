"""Checkpoint utilities with explicit model metadata."""
from __future__ import annotations

from pathlib import Path
from typing import Any

import torch

from .pet_brain import EmotionalPetActorCritic


def save_checkpoint(
    path: str | Path,
    model: EmotionalPetActorCritic,
    optimizer: torch.optim.Optimizer | None = None,
    *,
    episode: int = 0,
    stats: dict[str, Any] | None = None,
    persistent_hidden: torch.Tensor | None = None,
) -> None:
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    payload: dict[str, Any] = {
        "format": "curio-pet-brain-ppo-v1",
        "model_config": model.config,
        "model_state_dict": model.state_dict(),
        "episode": episode,
        "stats": stats or {},
        "randomly_initialized": True,
    }
    if optimizer is not None:
        payload["optimizer_state_dict"] = optimizer.state_dict()
    if persistent_hidden is not None:
        payload["persistent_hidden"] = persistent_hidden.detach().cpu()
    torch.save(payload, destination)


def load_checkpoint(
    path: str | Path,
    device: str | torch.device = "cpu",
    optimizer: torch.optim.Optimizer | None = None,
) -> tuple[EmotionalPetActorCritic, dict[str, Any]]:
    payload = torch.load(Path(path), map_location=device)
    if not isinstance(payload, dict) or "model_state_dict" not in payload:
        raise ValueError(f"Unsupported checkpoint format: {path}")
    model = EmotionalPetActorCritic(**payload.get("model_config", {})).to(device)
    model.load_state_dict(payload["model_state_dict"])
    if optimizer is not None and "optimizer_state_dict" in payload:
        optimizer.load_state_dict(payload["optimizer_state_dict"])
    return model, payload
