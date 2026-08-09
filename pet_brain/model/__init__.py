"""Model contracts and lazy neural-model exports."""
from .schema import Action, EmotionChannel, FEATURE_NAMES, InputSchema, NeedChannel

__all__ = [
    "Action", "EmotionChannel", "EmotionalPetActorCritic", "FEATURE_NAMES",
    "InputSchema", "NeedChannel", "build_model",
]


def __getattr__(name: str):
    if name in {"EmotionalPetActorCritic", "build_model"}:
        from .pet_brain import EmotionalPetActorCritic, build_model
        return {"EmotionalPetActorCritic": EmotionalPetActorCritic, "build_model": build_model}[name]
    raise AttributeError(name)
