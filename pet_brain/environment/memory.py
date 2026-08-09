"""Bounded long-term memory, separate from the GRU hidden state."""
from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Any


@dataclass
class Memory:
    kind: str
    text: str
    value: float
    importance: float
    age: int = 0


class PersistentMemory:
    def __init__(self, capacity: int = 32, decay: float = 0.995) -> None:
        self.capacity = capacity
        self.decay = decay
        self.items: list[Memory] = []

    def add(self, kind: str, text: str, value: float = 0.0, importance: float = 0.5) -> None:
        existing = next((item for item in self.items if item.kind == kind and item.text == text), None)
        if existing:
            existing.value = 0.8 * existing.value + 0.2 * value
            existing.importance = max(existing.importance, importance)
            existing.age = 0
        else:
            self.items.append(Memory(kind, text, float(value), float(importance)))
        self._trim()

    def step(self) -> None:
        for item in self.items:
            item.age += 1
            item.importance *= self.decay
        self._trim()

    def _trim(self) -> None:
        self.items.sort(key=lambda item: item.importance * math.exp(-item.age / 100.0), reverse=True)
        del self.items[self.capacity :]

    def vector(self, size: int = 8) -> list[float]:
        result = [0.0] * size
        for index, item in enumerate(self.items[:size]):
            result[index] = max(-1.0, min(1.0, item.value))
        return result

    def summary(self) -> list[dict[str, Any]]:
        return [item.__dict__.copy() for item in self.items]
