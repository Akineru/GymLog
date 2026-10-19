"""Validate candidate masks without touching official assets."""
from pathlib import Path
import csv
import json
import hashlib
import sys

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OFFICIAL = ROOT / "app/src/main/assets/muscle_masks"
BASE = ROOT / "app/src/main/res/drawable-nodpi"
INVENTORY = ROOT / "tools/muscle_redraw/inventory.json"
CANVAS = (1024, 1536)
ALPHA_HIT = 16
HIDDEN = {"rhomboids", "brachialis", "internal_oblique", "transversus_abdominis"}
PARENT = {"rectus_femoris": "quadriceps", "vastus_lateralis": "quadriceps", "vastus_medialis": "quadriceps"}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stats(path: Path, body: np.ndarray):
    im = Image.open(path).convert("RGBA")
    alpha = np.array(im.getchannel("A"))
    mask = alpha >= ALPHA_HIT
    alpha_pixels = int(mask.sum())
    outside = int((mask & ~body).sum())
    contours, hierarchy = cv2.findContours(mask.astype(np.uint8), cv2.RETR_CCOMP, cv2.CHAIN_APPROX_SIMPLE)
    holes = 0
    if hierarchy is not None:
        for i in range(len(contours)):
            if hierarchy[0][i][3] != -1 and cv2.contourArea(contours[i]) >= 50:
                holes += 1
    return {
        "size": list(im.size),
        "alphaMax": int(alpha.max()) if alpha.size else 0,
        "alphaPixels": alpha_pixels,
        "outsidePixels": outside,
        "outsidePct": round(outside / max(1, alpha_pixels) * 100.0, 2),
        "holeCount": holes,
        "sha256": sha(path),
        "image": im,
        "mask": mask,
    }


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: validate_candidates.py <work-dir>")
    work = Path(sys.argv[1]).resolve()
    inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    bases = {
        side: np.array(Image.open(BASE / f"muscle_{side}_base.png").convert("RGBA").getchannel("A")) > ALPHA_HIT
        for side in ("front", "back")
    }
    rows = []
    by_side = {"front": [], "back": []}
    for item in inventory["muscles"]:
        for side in ("front", "back"):
            file = item[f"{side}File"]
            official_path = OFFICIAL / side / file
            candidate_path = work / side / file
            prior_stats = stats(official_path, bases[side])
            cand_stats = stats(candidate_path, bases[side])
            prior = prior_stats["mask"]
            cand = cand_stats["mask"]
            prior_area = int(prior.sum())
            cand_area = int(cand.sum())
            inter = int((prior & cand).sum())
            union = int((prior | cand).sum())
            changed = int((prior ^ cand).sum())
            iou = inter / max(1, union)
            area_ratio = cand_area / max(1, prior_area) if prior_area else 0.0
            expected = prior_area > 0
            flags = []
            if cand_stats["size"] != list(CANVAS):
                flags.append("BAD_SIZE")
            if expected and cand_area == 0:
                flags.append("LOST_MASK")
            if not expected and cand_area > 0 and item["id"] in HIDDEN:
                flags.append("UNEXPECTED_VISIBLE")
            if cand_stats["outsidePct"] > 5.0:
                flags.append("OUTSIDE_BODY")
            if expected and area_ratio < 0.55:
                flags.append("TOO_SMALL")
            if expected and area_ratio > 1.8:
                flags.append("TOO_LARGE")
            if cand_stats["holeCount"] > 6:
                flags.append("MANY_HOLES")
            if cand_stats["alphaPixels"] > 0.55 * int(bases[side].sum()):
                flags.append("COVERS_TOO_MUCH_BODY")
            row = {
                "id": item["id"],
                "nameZh": item["nameZh"],
                "side": side,
                "file": file,
                "candidate": str(candidate_path),
                "official": str(official_path),
                "expectedVisible": expected,
                "candidatePixels": cand_area,
                "priorPixels": prior_area,
                "areaRatio": round(area_ratio, 3),
                "outsidePct": cand_stats["outsidePct"],
                "holes": cand_stats["holeCount"],
                "iouWithPrior": round(iou, 3),
                "changedPctOfUnion": round(changed / max(1, union) * 100.0, 2),
                "size": cand_stats["size"],
                "flags": flags,
                "sha256": cand_stats["sha256"],
            }
            rows.append(row)
            by_side[side].append(row)

    # Pairwise overlap between visible candidate masks; parent/child overlaps are
    # explicitly marked as design relationships.
    overlaps = []
    for side, side_rows in by_side.items():
        visible = [r for r in side_rows if r["candidatePixels"] > 0]
        masks = {}
        for r in visible:
            masks[r["id"]] = np.array(Image.open(r["candidate"]).convert("RGBA").getchannel("A")) >= ALPHA_HIT
        for i, a in enumerate(visible):
            for b in visible[i + 1:]:
                inter = int((masks[a["id"]] & masks[b["id"]]).sum())
                if inter == 0:
                    continue
                pct_a = inter / max(1, a["candidatePixels"]) * 100.0
                pct_b = inter / max(1, b["candidatePixels"]) * 100.0
                if max(pct_a, pct_b) >= 8.0:
                    relation = "parent_child" if PARENT.get(a["id"]) == b["id"] or PARENT.get(b["id"]) == a["id"] else "adjacent"
                    overlaps.append({
                        "side": side,
                        "a": a["id"],
                        "b": b["id"],
                        "pixels": inter,
                        "pctA": round(pct_a, 2),
                        "pctB": round(pct_b, 2),
                        "relation": relation,
                    })
    overlaps.sort(key=lambda x: max(x["pctA"], x["pctB"]), reverse=True)

    summary = {
        "generatedAt": __import__("datetime").datetime.now(__import__("datetime").timezone.utc).isoformat(),
        "workDir": str(work),
        "candidateFiles": len(rows),
        "invalidFiles": [r for r in rows if "BAD_SIZE" in r["flags"]],
        "lostMasks": [r for r in rows if "LOST_MASK" in r["flags"]],
        "outsideBody": [r for r in rows if "OUTSIDE_BODY" in r["flags"]],
        "areaOutliers": [r for r in rows if "TOO_SMALL" in r["flags"] or "TOO_LARGE" in r["flags"]],
        "manyHoles": [r for r in rows if "MANY_HOLES" in r["flags"]],
        "unexpectedVisible": [r for r in rows if "UNEXPECTED_VISIBLE" in r["flags"]],
        "reviewStatus": "candidate_pending_human_approval",
    }
    (work / "candidate_quality.json").write_text(
        json.dumps({"summary": summary, "masks": rows, "overlaps": overlaps}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    with (work / "candidate_quality.csv").open("w", newline="", encoding="utf-8-sig") as fh:
        writer = csv.writer(fh)
        writer.writerow([
            "side", "id", "nameZh", "file", "expectedVisible", "candidatePixels",
            "priorPixels", "areaRatio", "outsidePct", "holes", "iouWithPrior",
            "changedPctOfUnion", "flags",
        ])
        for r in rows:
            writer.writerow([
                r["side"], r["id"], r["nameZh"], r["file"], r["expectedVisible"],
                r["candidatePixels"], r["priorPixels"], r["areaRatio"], r["outsidePct"],
                r["holes"], r["iouWithPrior"], r["changedPctOfUnion"], ";".join(r["flags"]),
            ])
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    print("top overlaps:", json.dumps(overlaps[:12], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
