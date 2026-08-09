from dataclasses import dataclass


@dataclass(frozen=True)
class CurriculumStage:
    name: str
    start_episode: int
    needs: bool
    novelty: bool
    social: bool
    routine_variation: float


STAGES = (
    CurriculumStage("world", 0, False, False, False, 0.0),
    CurriculumStage("needs", 100, True, False, False, 0.0),
    CurriculumStage("exploration", 300, True, True, False, 0.1),
    CurriculumStage("social", 600, True, True, True, 0.2),
    CurriculumStage("temporal", 1000, True, True, True, 0.5),
    CurriculumStage("personality", 1500, True, True, True, 1.0),
)


def stage_for_episode(episode: int) -> CurriculumStage:
    stage = STAGES[0]
    for candidate in STAGES:
        if episode >= candidate.start_episode:
            stage = candidate
    return stage
