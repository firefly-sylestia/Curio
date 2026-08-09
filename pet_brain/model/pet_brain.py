"""PyTorch implementation of the trainable Emotional v3 pet brain."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Optional

import torch
from torch import Tensor, nn


@dataclass
class BrainOutput:
    action_logits: Tensor
    value: Tensor
    emotion: Tensor
    needs: Tensor
    memory: Tensor
    predicted_next_state: Tensor
    hidden: Tensor


class EmotionalPetActorCritic(nn.Module):
    """GRU policy/value network initialized from scratch.

    The supplied archive's reference backbone is retained: 128 inputs, a
    1,536-unit single-layer GRU, 24 emotion channels, 16 need channels, 64
    action channels, and a 256-dimensional memory projection.  PPO adds a
    value head and an action-conditioned next-state prediction head.
    """

    def __init__(
        self,
        input_size: int = 128,
        hidden_size: int = 1536,
        emotion_size: int = 24,
        need_size: int = 16,
        action_size: int = 64,
        memory_size: int = 256,
    ) -> None:
        super().__init__()
        self.config = {
            "input_size": input_size,
            "hidden_size": hidden_size,
            "emotion_size": emotion_size,
            "need_size": need_size,
            "action_size": action_size,
            "memory_size": memory_size,
        }
        self.gru = nn.GRU(input_size, hidden_size, batch_first=True)
        self.emotion_head = nn.Linear(hidden_size, emotion_size)
        self.need_head = nn.Linear(hidden_size, need_size)
        self.action_head = nn.Linear(hidden_size, action_size)
        self.memory_head = nn.Linear(hidden_size, memory_size)
        self.value_head = nn.Linear(hidden_size, 1)
        self.action_embedding = nn.Embedding(action_size, 32)
        self.next_state_head = nn.Sequential(
            nn.Linear(hidden_size + 32, hidden_size // 4),
            nn.Tanh(),
            nn.Linear(hidden_size // 4, input_size),
        )
        self.reset_parameters()

    def reset_parameters(self) -> None:
        """Use explicit random initialization; there is no pretrained knowledge."""
        for name, parameter in self.gru.named_parameters():
            if "weight" in name:
                nn.init.xavier_uniform_(parameter)
            else:
                nn.init.zeros_(parameter)
        for module in self.modules():
            if isinstance(module, nn.Linear):
                nn.init.xavier_uniform_(module.weight)
                nn.init.zeros_(module.bias)
            elif isinstance(module, nn.Embedding):
                nn.init.normal_(module.weight, mean=0.0, std=0.02)

    def initial_hidden(self, batch_size: int = 1, device: Optional[torch.device] = None) -> Tensor:
        return torch.zeros(1, batch_size, self.config["hidden_size"], device=device)

    def forward(
        self,
        observations: Tensor,
        hidden: Optional[Tensor] = None,
        actions_for_prediction: Optional[Tensor] = None,
    ) -> BrainOutput:
        if observations.ndim != 3 or observations.shape[-1] != self.config["input_size"]:
            raise ValueError(
                f"observations must be [batch, sequence, {self.config['input_size']}], "
                f"got {tuple(observations.shape)}"
            )
        sequence, next_hidden = self.gru(observations, hidden)
        action_logits = self.action_head(sequence)
        if actions_for_prediction is None:
            actions_for_prediction = action_logits.detach().argmax(dim=-1)
        action_embedding = self.action_embedding(actions_for_prediction)
        prediction_input = torch.cat((sequence, action_embedding), dim=-1)
        return BrainOutput(
            action_logits=action_logits,
            value=self.value_head(sequence).squeeze(-1),
            emotion=torch.tanh(self.emotion_head(sequence)),
            needs=torch.sigmoid(self.need_head(sequence)),
            memory=self.memory_head(sequence),
            predicted_next_state=self.next_state_head(prediction_input),
            hidden=next_hidden,
        )


def build_model(**kwargs: int) -> EmotionalPetActorCritic:
    return EmotionalPetActorCritic(**kwargs)
