from dataclasses import dataclass


@dataclass
class WorldObject:
    name: str
    x: float
    y: float
    available: bool = True
    novel: bool = False
    moving: bool = False
    reward_value: float = 0.0


@dataclass
class Owner:
    x: float = 0.8
    y: float = 0.2
    present: bool = True
