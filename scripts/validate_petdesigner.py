"""State-machine validator for the v8.34 pet designer feature.

Checks:
1. Balanced braces/brackets/parens (string/comment aware, handles escapes).
2. All 16x16 pet design grids contain exactly-16-char rows.
"""
import re
import sys

PATHS = [
    "app/src/main/java/com/curio/app/data/PetDesign.kt",
    "app/src/main/java/com/curio/app/data/AppPreferences.kt",
    "app/src/main/java/com/curio/app/ui/pet/CurioPetSprite.kt",
    "app/src/main/java/com/curio/app/features/petdesigner/PetDesignerScreen.kt",
    "app/src/main/java/com/curio/app/navigation/CurioRoutes.kt",
    "app/src/main/java/com/curio/app/navigation/CurioNavHost.kt",
    "app/src/main/java/com/curio/app/features/settings/SettingsHubScreen.kt",
    "app/src/main/java/com/curio/app/ui/theme/CurioIcons.kt",
]


def check_braces(path: str) -> bool:
    src = open(path, encoding="utf-8").read()
    i, n = 0, len(src)
    in_str = False
    in_line = in_block = False
    stack = []
    pairs = {"(": ")", "[": "]", "{": "}"}
    line = 1
    ok = True
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        if c == "\n":
            line += 1
            if in_line:
                in_line = False
        if in_str:
            if c == "\\":
                i += 2
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if in_line:
            i += 1
            continue
        if in_block:
            if c == "*" and nxt == "/":
                in_block = False
                i += 2
                continue
            i += 1
            continue
        if c == "/" and nxt == "/":
            in_line = True
            i += 2
            continue
        if c == "/" and nxt == "*":
            in_block = True
            i += 2
            continue
        if c == '"':
            in_str = True
            i += 1
            continue
        if c in pairs:
            stack.append((c, line))
        elif c in pairs.values():
            if not stack or pairs[stack.pop()[0]] != c:
                print(f"{path}:{line} BRACE MISMATCH near {src[max(0, i - 30):i + 30]!r}")
                ok = False
                break
        i += 1
    if ok and stack:
        print(f"{path}:{stack[-1][1]} UNCLOSED {[c for c, _ in stack[-5:]]}")
        ok = False
    if ok:
        print(f"{path} braces OK")
    return ok


def check_rows() -> bool:
    src = open("app/src/main/java/com/curio/app/data/PetDesign.kt", encoding="utf-8").read()
    # Only the quoted grid constants (not the kdoc) matter for correctness.
    grid_blocks = re.findall(r'val (DEFAULT_BODY|DEFAULT_CURLED).*?listOf\((.*?)\)', src, flags=re.S)
    ok = True
    for name, block in grid_blocks:
        rows = re.findall(r'"([^"]+)"', block)
        for r in rows:
            if len(r) != 16:
                print(f"DEFAULT {name} row len {len(r)}: {r!r}")
                ok = False
        print(f"{name}: {len(rows)} rows, all 16 chars" if ok else f"{name}: ROW LENGTH ERRORS")
    return ok


def main() -> None:
    all_ok = True
    for p in PATHS:
        all_ok = check_braces(p) and all_ok
    all_ok = check_rows() and all_ok
    print("ALL CHECKS PASSED" if all_ok else "CHECKS FAILED")
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
