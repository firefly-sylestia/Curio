"""Stable model I/O contracts.

Every Android or simulator observation must use this exact order. The
observation is made of 32 internal channels, 32 interaction channels, 32 world
channels, and 32 compressed history/memory channels. Missing optional signals
are encoded as zero; unknown names fail loudly.
"""
from enum import IntEnum


_INTERNAL = [
    "hunger", "thirst", "energy", "sleepiness", "loneliness", "boredom",
    "curiosity", "comfort", "stimulation", "social_need", "affection", "stress",
    "mood_calm", "mood_happy", "mood_sad", "activity_idle", "activity_sleep",
    "activity_eat", "activity_play", "activity_explore", "activity_social",
    "time_since_interaction", "time_of_day_morning", "time_of_day_day",
    "time_of_day_evening", "time_of_day_night", "owner_present", "owner_distance",
    "pet_x", "pet_y", "pet_speed", "pet_direction_x",
]
_INTERACTION = [
    "tap", "long_press", "swipe", "swipe_left", "swipe_right", "swipe_up",
    "swipe_down", "swipe_velocity", "swipe_duration", "drag", "drag_dx", "drag_dy",
    "repeated_interaction", "interaction_frequency", "interaction_distance",
    "target_food", "target_toy", "target_bed", "target_object", "target_owner",
    "feeding", "playing", "petting", "talking", "object_interaction",
    "touch_pressure", "interaction_success", "interaction_age", "last_action",
    "action_result", "action_repeat_count", "interaction_duration",
]
_ENVIRONMENT = [
    "food_available", "water_available", "bed_available", "toy_available",
    "object_count", "novel_object_count", "food_x", "food_y", "water_x", "water_y",
    "bed_x", "bed_y", "toy_x", "toy_y", "nearest_object_distance",
    "nearest_object_novelty", "nearest_object_moving", "room_light", "room_time",
    "room_temperature", "environment_owner_present", "owner_x", "owner_y",
    "environment_owner_distance", "other_entity_count", "room_cleanliness",
    "room_safety", "obstacle_density", "object_motion_x", "object_motion_y",
    "food_level", "water_level",
]
_HISTORY = [
    *(f"previous_action_{i}" for i in range(16)),
    "previous_reward_positive", "previous_reward_negative", "previous_reward_magnitude",
    "previous_outcome_success", "previous_outcome_failure", "previous_outcome_novel",
    "sequence_progress", "routine_match", "persistent_memory_0", "persistent_memory_1",
    "persistent_memory_2", "persistent_memory_3", "persistent_memory_4",
    "persistent_memory_5", "persistent_memory_6", "persistent_memory_7",
]

FEATURE_NAMES = tuple(_INTERNAL + _INTERACTION + _ENVIRONMENT + _HISTORY)
assert len(FEATURE_NAMES) == 128, len(FEATURE_NAMES)
InputSchema = IntEnum("InputSchema", {name.upper(): i for i, name in enumerate(FEATURE_NAMES)})


class EmotionChannel(IntEnum):
    HAPPINESS = 0; SADNESS = 1; FEAR = 2; ANGER = 3; AFFECTION = 4; EXCITEMENT = 5
    CURIOSITY = 6; BOREDOM = 7; LONELINESS = 8; CONTENTMENT = 9; SURPRISE = 10
    TRUST = 11; SHYNESS = 12; CONFIDENCE = 13; FRUSTRATION = 14; PLAYFULNESS = 15
    CALM = 16; ALERTNESS = 17; JEALOUSY = 18; ANTICIPATION = 19; COMFORT = 20
    DISCOMFORT = 21; PRIDE = 22; ATTACHMENT = 23


class NeedChannel(IntEnum):
    HUNGER = 0; THIRST = 1; SLEEPINESS = 2; ENERGY = 3; SOCIAL = 4; PLAY = 5
    COMFORT = 6; EXPLORATION = 7; ATTENTION = 8; SAFETY = 9; CLEANLINESS = 10
    STIMULATION = 11; REST = 12; AFFECTION = 13; NOVELTY = 14; ROUTINE = 15


class Action(IntEnum):
    IDLE = 0; LOOK = 1; MOVE = 2; APPROACH = 3; RETREAT = 4; SLEEP = 5
    EAT = 6; DRINK = 7; PLAY = 8; INSPECT = 9; TOUCH = 10; FOLLOW = 11
    SIT = 12; LIE_DOWN = 13; MAKE_SOUND = 14; INTERACT_OBJECT = 15
    APPROACH_OWNER = 16; AVOID_OWNER = 17; REST = 18; WANDER = 19


ACTION_NAMES = tuple(action.name.lower() for action in Action) + tuple(
    f"learned_channel_{i}" for i in range(20, 64)
)
assert len(ACTION_NAMES) == 64


def encode_features(values: dict[str, float]) -> list[float]:
    unknown = set(values) - set(FEATURE_NAMES)
    if unknown:
        raise KeyError(f"Unknown observation features: {sorted(unknown)}")
    return [float(values.get(name, 0.0)) for name in FEATURE_NAMES]
