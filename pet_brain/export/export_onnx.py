"""Export a trained recurrent policy for later ONNX Runtime Mobile use.

This script requires the optional ``onnx`` package in addition to PyTorch.
The exported interface is one sequence at a time and makes recurrent state
explicit, which keeps Android responsible for persistence between calls.
"""
from __future__ import annotations

import argparse
from pathlib import Path

import torch
from torch import nn

from ..model.checkpoint import load_checkpoint


class OnnxInferenceWrapper(nn.Module):
    def __init__(self, model: nn.Module) -> None:
        super().__init__()
        self.model = model

    def forward(self, observations: torch.Tensor, hidden: torch.Tensor):
        output = self.model(observations, hidden)
        return output.action_logits, output.value, output.emotion, output.needs, output.memory, output.hidden


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    model, _ = load_checkpoint(args.checkpoint)
    model.eval()
    wrapper = OnnxInferenceWrapper(model)
    observations = torch.zeros(1, 1, model.config["input_size"])
    hidden = model.initial_hidden(1)
    torch.onnx.export(
        wrapper,
        (observations, hidden),
        args.output,
        input_names=["observations", "hidden_in"],
        output_names=["action_logits", "value", "emotion", "needs", "memory", "hidden_out"],
        dynamic_axes={"observations": {0: "batch", 1: "sequence"}, "hidden_in": {1: "batch"}, "action_logits": {0: "batch", 1: "sequence"}, "value": {0: "batch", 1: "sequence"}, "emotion": {0: "batch", 1: "sequence"}, "needs": {0: "batch", 1: "sequence"}, "memory": {0: "batch", 1: "sequence"}, "hidden_out": {1: "batch"}},
        opset_version=17,
    )
    print(f"wrote {args.output}")


if __name__ == "__main__":
    main()
