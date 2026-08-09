"""Constrained speech layer; it observes the brain but never selects actions."""
from ..model.schema import Action, EmotionChannel, NeedChannel


def speech_for_state(action: int, emotion: list[float], needs: list[float]) -> str:
    def need(channel: NeedChannel) -> float:
        return needs[channel] if len(needs) > channel else 0.0

    def feeling(channel: EmotionChannel) -> float:
        return emotion[channel] if len(emotion) > channel else 0.0

    selected = Action(action) if action < len(Action) else None
    if selected in (Action.EAT, Action.DRINK) or need(NeedChannel.HUNGER) > 0.75:
        return "Is there something to eat?"
    if selected in (Action.SLEEP, Action.REST, Action.LIE_DOWN) or need(NeedChannel.SLEEPINESS) > 0.75:
        return "Mrr... I think I need a nap."
    if selected == Action.APPROACH_OWNER and feeling(EmotionChannel.AFFECTION) > 0.45:
        return "You're here!"
    if selected in (Action.INSPECT, Action.INTERACT_OBJECT, Action.TOUCH) and feeling(EmotionChannel.CURIOSITY) > 0.45:
        return "What's that?"
    if selected == Action.PLAY or feeling(EmotionChannel.PLAYFULNESS) > 0.55:
        return "Play with me?"
    if selected == Action.SLEEP:
        return "I'm sleepy..."
    return "Hmm..."
