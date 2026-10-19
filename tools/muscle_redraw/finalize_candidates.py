"""Resolve adjacent overlaps in candidate masks and write a final review set.

The parent quadriceps mask is the union of its three child masks; that
parent/child overlap is intentional. Every other pair is made disjoint by
letting the smaller, more specific mask keep the overlap.
"""
from pathlib import Path
import json
import shutil
import sys

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "app/src/main/assets/muscle_manifest.json"
ALPHA = 16
CHILDREN = {"quadriceps": ["rectus_femoris", "vastus_lateralis", "vastus_medialis"]}
PARENT = {"rectus_femoris": "quadriceps", "vastus_lateralis": "quadriceps", "vastus_medialis": "quadriceps"}


def load(path):
    return np.array(Image.open(path).convert("RGBA").getchannel("A")) >= ALPHA


def save(path, mask):
    rgba = np.zeros((*mask.shape, 4), np.uint8)
    rgba[:, :, :3] = 255
    rgba[:, :, 3] = (mask.astype(np.uint8) * 255)
    Image.fromarray(rgba, "RGBA").save(path)


def main():
    if len(sys.argv) != 3:
        raise SystemExit("usage: finalize_candidates.py <source-work> <final-work>")
    source = Path(sys.argv[1]).resolve()
    final = Path(sys.argv[2]).resolve()
    (final / "front").mkdir(parents=True, exist_ok=True)
    (final / "back").mkdir(parents=True, exist_ok=True)
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    for side in ("front", "back"):
        masks = {}
        files = {}
        for item in manifest["muscles"]:
            hint = item["muscleIdHint"]
            app_id = "deltoid_lateral" if hint == "deltoid_middle" else hint
            file = Path(item[f"{side}Mask"]).name
            masks[app_id] = load(source / side / file)
            files[app_id] = file
        ids = [i for i in masks if i != "quadriceps"]
        for i, a in enumerate(ids):
            for b in ids[i + 1:]:
                if PARENT.get(a) == b or PARENT.get(b) == a:
                    continue
                if a in CHILDREN["quadriceps"] and b in CHILDREN["quadriceps"]:
                    # Keep child-head overlap; the parent composite is their union.
                    continue
                overlap = masks[a] & masks[b]
                if not overlap.any():
                    continue
                if masks[a].sum() <= masks[b].sum():
                    masks[b] = masks[b] & ~overlap
                else:
                    masks[a] = masks[a] & ~overlap
        parent = np.zeros_like(next(iter(masks.values())))
        for head in CHILDREN["quadriceps"]:
            parent |= masks.get(head, np.zeros_like(parent))
        for other_id, other in masks.items():
            if other_id == "quadriceps" or other_id in CHILDREN["quadriceps"]:
                continue
            parent &= ~other
        masks["quadriceps"] = parent
        for app_id, file in files.items():
            save(final / side / file, masks[app_id])
    print("final set", final)


if __name__ == "__main__":
    main()
