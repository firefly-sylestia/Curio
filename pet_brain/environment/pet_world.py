"""A lightweight standalone pet world for reinforcement-learning experiments."""
from __future__ import annotations

from dataclasses import dataclass
import math
import random
from typing import Any

from .memory import PersistentMemory
from .objects import Owner, WorldObject
from .rewards import RewardBreakdown
from ..model.schema import ACTION_NAMES, Action, encode_features


@dataclass
class WorldConfig:
    max_steps: int = 96
    seed: int = 0
    time_step_hours: float = 0.25


class PetWorld:
    """A tiny room simulator with explicit consequences and no policy rules."""

    def __init__(self, config: WorldConfig | None = None) -> None:
        self.config = config or WorldConfig()
        self.rng = random.Random(self.config.seed)
        self.memory = PersistentMemory()
        self.reset()

    def reset(self, *, seed: int | None = None) -> list[float]:
        if seed is not None:
            self.rng.seed(seed)
        self.step_count = 0
        self.x, self.y = 0.2, 0.7
        self.energy, self.hunger, self.thirst = 0.75, 0.2, 0.2
        self.sleepiness, self.social, self.play_need = 0.15, 0.35, 0.35
        self.comfort, self.stress = 0.7, 0.1
        self.last_action, self.last_result = Action.IDLE, 0.0
        self.last_interaction, self.last_novel = 0, 0.0
        self.action_history = [0] * 16
        self.owner = Owner(x=0.75, y=0.25, present=True)
        self.objects = [
            WorldObject("food", 0.15, 0.18, reward_value=0.9),
            WorldObject("water", 0.45, 0.18, reward_value=0.7),
            WorldObject("bed", 0.82, 0.75, reward_value=0.5),
            WorldObject("toy", 0.55, 0.62, reward_value=0.65, novel=True, moving=True),
        ]
        self.memory.step()
        return self.observation()

    def _distance(self, x: float, y: float) -> float:
        return math.hypot(self.x - x, self.y - y)

    def _nearest(self) -> WorldObject:
        return min(self.objects, key=lambda obj: self._distance(obj.x, obj.y))

    def _time_features(self) -> dict[str, float]:
        phase = (self.step_count * self.config.time_step_hours) % 24.0
        return {
            "time_of_day_morning": float(6 <= phase < 12),
            "time_of_day_day": float(12 <= phase < 18),
            "time_of_day_evening": float(18 <= phase < 22),
            "time_of_day_night": float(phase < 6 or phase >= 22),
            "room_time": phase / 24.0,
            "room_light": 0.2 if phase < 6 or phase >= 22 else 0.9,
        }

    def observation(self) -> list[float]:
        nearest = self._nearest()
        values = {
            "hunger": self.hunger, "thirst": self.thirst, "energy": self.energy,
            "sleepiness": self.sleepiness, "loneliness": self.social, "boredom": self.play_need,
            "curiosity": float(nearest.novel), "comfort": self.comfort, "stimulation": 1.0 - self.play_need,
            "social_need": self.social, "affection": 1.0 - self.social, "stress": self.stress,
            "mood_calm": self.comfort, "mood_happy": 1.0 - (self.hunger + self.thirst + self.stress) / 3.0,
            "mood_sad": (self.hunger + self.social) / 2.0,
            "activity_idle": float(self.last_action == Action.IDLE),
            "activity_sleep": float(self.last_action in (Action.SLEEP, Action.REST)),
            "activity_eat": float(self.last_action == Action.EAT),
            "activity_play": float(self.last_action == Action.PLAY),
            "activity_explore": float(self.last_action in (Action.INSPECT, Action.WANDER)),
            "activity_social": float(self.last_action == Action.APPROACH_OWNER),
            "time_since_interaction": min(1.0, self.last_interaction / 30.0),
            "owner_present": float(self.owner.present), "owner_distance": self._distance(self.owner.x, self.owner.y),
            "pet_x": self.x, "pet_y": self.y, "pet_speed": 0.0, "pet_direction_x": 0.0,
            "last_action": self.last_action / 63.0, "action_result": self.last_result,
            "action_repeat_count": float(self.action_history.count(int(self.last_action))) / 16.0,
            "interaction_duration": 0.0,
            "owner_x": self.owner.x, "owner_y": self.owner.y,
            "environment_owner_present": float(self.owner.present),
            "environment_owner_distance": self._distance(self.owner.x, self.owner.y),
            "food_available": float(self.objects[0].available), "water_available": float(self.objects[1].available),
            "bed_available": float(self.objects[2].available), "toy_available": float(self.objects[3].available),
            "object_count": len(self.objects) / 8.0, "novel_object_count": sum(obj.novel for obj in self.objects) / 4.0,
            "food_x": self.objects[0].x, "food_y": self.objects[0].y,
            "water_x": self.objects[1].x, "water_y": self.objects[1].y,
            "bed_x": self.objects[2].x, "bed_y": self.objects[2].y,
            "toy_x": self.objects[3].x, "toy_y": self.objects[3].y,
            "nearest_object_distance": self._distance(nearest.x, nearest.y),
            "nearest_object_novelty": float(nearest.novel), "nearest_object_moving": float(nearest.moving),
            "room_temperature": 0.5, "other_entity_count": 1.0, "room_cleanliness": 0.8,
            "room_safety": 0.95, "obstacle_density": 0.1, "object_motion_x": 0.0,
            "object_motion_y": 0.0, "food_level": 1.0, "water_level": 1.0,
            "previous_reward_positive": float(self.last_result > 0), "previous_reward_negative": float(self.last_result < 0),
            "previous_reward_magnitude": min(1.0, abs(self.last_result)),
            "previous_outcome_success": float(self.last_result > 0.1), "previous_outcome_failure": float(self.last_result < -0.1),
            "previous_outcome_novel": self.last_novel,
            "sequence_progress": min(1.0, self.step_count / self.config.max_steps),
            "routine_match": float(self.step_count % 24 < 4),
        }
        values.update(self._time_features())
        values.update({f"previous_action_{i}": self.action_history[i] / 63.0 for i in range(16)})
        values.update({f"persistent_memory_{i}": value for i, value in enumerate(self.memory.vector(8))})
        return encode_features(values)

    def _move_toward(self, target_x: float, target_y: float, amount: float = 0.12) -> None:
        dx, dy = target_x - self.x, target_y - self.y
        distance = max(math.hypot(dx, dy), 1e-6)
        self.x = min(1.0, max(0.0, self.x + amount * dx / distance))
        self.y = min(1.0, max(0.0, self.y + amount * dy / distance))

    def step(self, action_index: int) -> tuple[list[float], float, bool, dict[str, Any]]:
        action_index = int(action_index)
        action = Action(action_index) if 0 <= action_index < len(Action) else None
        nearest = self._nearest()
        reward = RewardBreakdown()
        self.last_novel, success, event = 0.0, False, "idle"
        if action in (Action.MOVE, Action.WANDER):
            self._move_toward(self.rng.random(), self.rng.random()); event, success = "moved", True; self.energy -= 0.025
        elif action in (Action.APPROACH, Action.INSPECT, Action.INTERACT_OBJECT, Action.TOUCH):
            self._move_toward(nearest.x, nearest.y)
            if self._distance(nearest.x, nearest.y) < 0.18:
                event, success = f"reached_{nearest.name}", True
                if nearest.novel:
                    self.last_novel, nearest.novel = 1.0, False
                    reward.exploration = 0.35; self.memory.add("discovery", nearest.name, 1.0, 0.8)
                reward.need += nearest.reward_value * 0.15
            else:
                event = "approaching"
            self.energy -= 0.02
        elif action == Action.EAT and self._distance(self.objects[0].x, self.objects[0].y) < 0.2:
            self.hunger = max(0.0, self.hunger - 0.55); success, event, reward.need = True, "ate", 0.8
        elif action == Action.DRINK and self._distance(self.objects[1].x, self.objects[1].y) < 0.2:
            self.thirst = max(0.0, self.thirst - 0.6); success, event, reward.need = True, "drank", 0.8
        elif action in (Action.SLEEP, Action.REST, Action.LIE_DOWN) and self._distance(self.objects[2].x, self.objects[2].y) < 0.25:
            self.energy = min(1.0, self.energy + 0.35); self.sleepiness = max(0.0, self.sleepiness - 0.35)
            success, event, reward.need = True, "rested", 0.55
        elif action == Action.PLAY and self._distance(self.objects[3].x, self.objects[3].y) < 0.25:
            self.play_need = max(0.0, self.play_need - 0.45); self.energy -= 0.06
            success, event, reward.social = True, "played", 0.45; self.memory.add("preference", "toy", 0.8, 0.45)
        elif action == Action.APPROACH_OWNER and self.owner.present:
            self._move_toward(self.owner.x, self.owner.y)
            if self._distance(self.owner.x, self.owner.y) < 0.2:
                self.social = max(0.0, self.social - 0.35); success, event, reward.social = True, "visited_owner", 0.55
        elif action == Action.AVOID_OWNER:
            self._move_toward(1.0 - self.owner.x, 1.0 - self.owner.y); success, event = True, "retreated"
        elif action in (Action.IDLE, Action.LOOK, Action.SIT, Action.MAKE_SOUND, Action.FOLLOW):
            event = ACTION_NAMES[action_index]
        else:
            event, reward.failure = f"unavailable_{action_index}", -0.12
        if not success and action not in (Action.IDLE, Action.LOOK):
            reward.failure -= 0.04
        self.hunger = min(1.0, self.hunger + 0.012); self.thirst = min(1.0, self.thirst + 0.014)
        self.sleepiness = min(1.0, self.sleepiness + 0.009); self.social = min(1.0, self.social + 0.004)
        self.play_need = min(1.0, self.play_need + 0.005); self.energy = max(0.0, min(1.0, self.energy - 0.003))
        self.last_action, self.last_result = action_index, reward.total
        self.last_interaction += 1; self.action_history = ([action_index] + self.action_history)[:16]
        self.step_count += 1; self.memory.step(); self.stress = min(1.0, max(0.0, self.stress + (0.01 if not success else -0.02)))
        done = self.step_count >= self.config.max_steps or self.energy <= 0.0
        info = {
            "event": event,
            "success": success,
            "novel": bool(self.last_novel),
            "reward": reward.__dict__,
            "truncated": self.step_count >= self.config.max_steps and self.energy > 0.0,
            "terminal": self.energy <= 0.0,
        }
        return self.observation(), reward.total, done, info
