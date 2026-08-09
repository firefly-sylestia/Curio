from dataclasses import dataclass, field


@dataclass
class Transition:
    observation: list[float]
    action: int
    log_probability: float
    value: float
    reward: float
    done: bool
    next_observation: list[float]
    curiosity_reward: float = 0.0
    info: dict = field(default_factory=dict)


@dataclass
class Trajectory:
    transitions: list[Transition] = field(default_factory=list)
    initial_hidden: object | None = None
    final_hidden: object | None = None

    def append(self, transition: Transition) -> None:
        self.transitions.append(transition)

    def __len__(self) -> int:
        return len(self.transitions)
