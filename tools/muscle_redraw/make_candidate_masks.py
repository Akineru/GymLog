"""Generate candidate masks in an external work directory.

This is tooling/audit code, not app code. It never writes the official assets.
Usage: python make_candidate_masks.py <work-dir>
"""
from pathlib import Path
import json
import sys

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage as ndi
from scipy.interpolate import splprep, splev

ROOT = Path(__file__).resolve().parents[2]
MASK_DIR = ROOT / "app" / "src" / "main" / "assets" / "muscle_masks"
BASE_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
MANIFEST = ROOT / "app" / "src" / "main" / "assets" / "muscle_manifest.json"
ALPHA_HIT = 16

PARENT_OF = {
    "rectus_femoris": "quadriceps",
    "vastus_lateralis": "quadriceps",
    "vastus_medialis": "quadriceps",
}
CHILDREN_OF = {
    "quadriceps": ["rectus_femoris", "vastus_lateralis", "vastus_medialis"],
}
def load_base(side: str):
    image = np.array(Image.open(BASE_DIR / f"muscle_{side}_base.png").convert("RGBA"))
    alpha = image[:, :, 3]
    rgb = image[:, :, :3].astype(np.float32)
    lum = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
    body = alpha > ALPHA_HIT
    smooth = ndi.gaussian_filter(lum, 1.2)
    grad = ndi.gaussian_gradient_magnitude(smooth, 2.0)
    scale = max(1e-6, float(np.percentile(grad[body], 99)))
    gradient = np.clip(grad / scale * 255.0, 0, 255).astype(np.uint8)
    gradient[~body] = 255
    return image, body, gradient


def clean(mask: np.ndarray, seed: np.ndarray) -> np.ndarray:
    # Smooth the blocky watershed edge while keeping the visible line-art
    # boundary. Large tendon gaps are preserved; only small pinholes are filled.
    mask = ndi.binary_closing(mask, structure=np.ones((3, 3), dtype=bool), iterations=2)
    mask = ndi.binary_opening(mask, structure=np.ones((3, 3), dtype=bool), iterations=1)
    holes = ndi.binary_fill_holes(mask) & ~mask
    hole_labels, hole_count = ndi.label(holes, structure=np.ones((3, 3), dtype=int))
    if hole_count:
        hole_sizes = np.bincount(hole_labels.ravel())
        small_holes = np.isin(hole_labels, np.where(hole_sizes < 350)[0])
        mask |= small_holes
    # Keep components that contain seed pixels, plus substantial nearby parts.
    labeled, count = ndi.label(mask, structure=np.ones((3, 3), dtype=int))
    if count == 0:
        return mask
    keep = np.zeros(count + 1, dtype=bool)
    kept_ids = np.unique(labeled[seed])
    keep[kept_ids[kept_ids > 0]] = True
    sizes = np.bincount(labeled.ravel())
    for index in range(1, count + 1):
        if sizes[index] >= 250:
            keep[index] = True
    return keep[labeled]


def watershed_mask(prior: np.ndarray, body: np.ndarray, gradient: np.ndarray):
    if not prior.any():
        return np.zeros_like(prior)
    ys, xs = np.where(prior)
    y0, y1 = max(0, ys.min() - 40), min(prior.shape[0], ys.max() + 41)
    x0, x1 = max(0, xs.min() - 40), min(prior.shape[1], xs.max() + 41)
    roi = np.zeros_like(prior)
    roi[y0:y1, x0:x1] = True
    seed = ndi.binary_erosion(prior, iterations=3) & body
    if not seed.any():
        seed = prior & body
    ring = roi & body & ~ndi.binary_dilation(prior, iterations=18)
    markers = np.zeros(prior.shape, np.int32)
    markers[seed] = 1
    markers[ring] = 2
    labels = ndi.watershed_ift(gradient, markers)
    result = (labels == 1) & body
    result = clean(result, seed)
    prior_area = int(prior.sum())
    result_area = int(result.sum())
    if result_area < max(80, prior_area * 0.25) or result_area > prior_area * 2.8:
        # Keep the prior when the watershed collapses or floods.
        result = prior & body
    return result


def smooth_contour(mask: np.ndarray) -> np.ndarray:
    """Fit a periodic spline to each outer contour and preserve large holes."""
    contours, hierarchy = cv2.findContours(
        mask.astype(np.uint8), cv2.RETR_CCOMP, cv2.CHAIN_APPROX_NONE,
    )
    if hierarchy is None:
        return mask
    out = np.zeros(mask.shape, dtype=np.uint8)
    for index, contour in enumerate(contours):
        if hierarchy[0][index][3] != -1:
            continue
        points = contour[:, 0, :].astype(np.float64)
        if len(points) < 4:
            continue
        try:
            tck, _ = splprep([points[:, 0], points[:, 1]], s=len(points) * 1.5, per=True)
            u = np.linspace(0.0, 1.0, max(120, len(points) * 2), endpoint=True)
            x, y = splev(u, tck)
            polygon = np.stack([x, y], axis=1).astype(np.int32)
        except Exception:
            polygon = points.astype(np.int32)
        cv2.fillPoly(out, [polygon], 1)
    for index, contour in enumerate(contours):
        if hierarchy[0][index][3] == -1:
            continue
        if cv2.contourArea(contour) >= 350:
            cv2.fillPoly(out, [contour.astype(np.int32)], 0)
    return out.astype(bool)


def save_mask(path: Path, mask: np.ndarray):
    mask = smooth_contour(mask)
    # Soft one-pixel edge hides the remaining staircase without moving the
    # underlying boundary. The app reads the alpha channel, not RGB.
    soft = ndi.gaussian_filter(mask.astype(np.float32), 0.9)
    alpha = np.clip(soft * 255.0 * 1.08, 0, 255).astype(np.uint8)
    rgba = np.zeros((*mask.shape, 4), dtype=np.uint8)
    rgba[:, :, :3] = 255
    rgba[:, :, 3] = alpha
    Image.fromarray(rgba, "RGBA").save(path)


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: make_candidate_masks.py <work-dir>")
    work = Path(sys.argv[1]).resolve()
    work.mkdir(parents=True, exist_ok=True)
    (work / "front").mkdir(exist_ok=True)
    (work / "back").mkdir(exist_ok=True)
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    report = {"workDir": str(work), "masks": []}
    bases = {side: load_base(side) for side in ("front", "back")}
    for item in manifest["muscles"]:
        hint = item["muscleIdHint"]
        app_id = "deltoid_lateral" if hint == "deltoid_middle" else hint
        for side in ("front", "back"):
            source_name = Path(item[f"{side}Mask"]).name
            source = MASK_DIR / side / source_name
            prior = np.array(Image.open(source).convert("RGBA").getchannel("A")) >= ALPHA_HIT
            image, body, gradient = bases[side]
            if not prior.any():
                result = np.zeros_like(prior)
            else:
                result = watershed_mask(prior, body, gradient)
            # Keep the existing file name so the replacement step never renames
            # a resource (deltoid_lateral continues to use deltoid_middle.png).
            out_path = work / side / source_name
            save_mask(out_path, result)
            outside = int((result & ~body).sum())
            report["masks"].append({
                "id": app_id,
                "side": side,
                "source": str(source),
                "output": str(out_path),
                "priorPixels": int(prior.sum()),
                "outputPixels": int(result.sum()),
                "outsidePixels": outside,
                "outsidePct": (outside / max(1, int(result.sum()))) * 100.0,
                "parent": PARENT_OF.get(app_id),
                "children": CHILDREN_OF.get(app_id, []),
            })
    # A parent composite should be the union of its three head masks, so the
    # parent and child files cannot disagree.
    for side in ("front", "back"):
        head_paths = [work / side / f"{name}.png" for name in CHILDREN_OF["quadriceps"]]
        if all(p.exists() for p in head_paths):
            union = np.zeros_like(np.array(Image.open(head_paths[0]).convert("RGBA").getchannel("A")))
            for p in head_paths:
                a = np.array(Image.open(p).convert("RGBA").getchannel("A")) >= ALPHA_HIT
                union |= a
            existing = work / side / "quadriceps.png"
            # Only replace if the union is non-empty; the composite remains in
            # the same stable ID/file.
            if union.any():
                save_mask(existing, union)
                for record in report["masks"]:
                    if record["id"] == "quadriceps" and record["side"] == side:
                        body = bases[side][1]
                        record["outputPixels"] = int(union.sum())
                        record["outsidePixels"] = int((union & ~body).sum())
                        record["outsidePct"] = (record["outsidePixels"] / max(1, int(union.sum()))) * 100.0
                        record["compositeOf"] = CHILDREN_OF["quadriceps"]
    (work / "candidate_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"masks": len(report["masks"]), "work": str(work)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
