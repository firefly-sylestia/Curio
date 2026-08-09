"""Dependency-free checks that can run before installing PyTorch."""
from .model.schema import ACTION_NAMES, FEATURE_NAMES


def main() -> None:
    assert len(FEATURE_NAMES) == 128
    assert len(set(FEATURE_NAMES)) == 128
    assert len(ACTION_NAMES) == 64
    assert len(set(ACTION_NAMES)) == 64
    print("schema_pass features=128 actions=64 unique_names=true")


if __name__ == "__main__":
    main()
