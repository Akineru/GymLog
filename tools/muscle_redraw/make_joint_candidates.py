"""Joint watershed candidate masks (disjoint adjacent labels).

Writes only to an external work directory. The parent `quadriceps` mask is
derived from the union of its three child masks.
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
MASK_DIR = ROOT / "app/src/main/assets/muscle_masks"
BASE_DIR = ROOT / "app/src/main/res/drawable-nodpi"
MANIFEST = ROOT / "app/src/main/assets/muscle_manifest.json"
ALPHA = 16
HIDDEN = {"rhomboids", "brachialis", "internal_oblique", "transversus_abdominis"}
CHILDREN = {"quadriceps": ["rectus_femoris", "vastus_lateralis", "vastus_medialis"]}
PARENT = {"rectus_femoris": "quadriceps", "vastus_lateralis": "quadriceps", "vastus_medialis": "quadriceps"}


def load_base(side):
    im = np.array(Image.open(BASE_DIR / f"muscle_{side}_base.png").convert("RGBA"))
    a = im[:, :, 3]
    rgb = im[:, :, :3].astype(np.float32)
    lum = .299*rgb[:,:,0]+.587*rgb[:,:,1]+.114*rgb[:,:,2]
    body = a > ALPHA
    smooth = ndi.gaussian_filter(lum, 1.2)
    grad = ndi.gaussian_gradient_magnitude(smooth, 2.0)
    grad = np.clip(grad / max(1e-6, float(np.percentile(grad[body], 99))) * 255, 0, 255).astype(np.uint8)
    grad[~body] = 255
    return im, body, grad


def smooth_contour(mask):
    contours, hierarchy = cv2.findContours(mask.astype(np.uint8), cv2.RETR_CCOMP, cv2.CHAIN_APPROX_NONE)
    if hierarchy is None:
        return mask
    out = np.zeros(mask.shape, np.uint8)
    for i, c in enumerate(contours):
        if hierarchy[0][i][3] != -1:
            continue
        pts = c[:, 0, :].astype(float)
        if len(pts) < 4:
            continue
        try:
            tck, _ = splprep([pts[:,0], pts[:,1]], s=len(pts)*1.5, per=True)
            u = np.linspace(0, 1, max(120, len(pts)*2), endpoint=True)
            x, y = splev(u, tck)
            poly = np.stack([x,y], axis=1).astype(np.int32)
        except Exception:
            poly = pts.astype(np.int32)
        cv2.fillPoly(out, [poly], 1)
    for i, c in enumerate(contours):
        if hierarchy[0][i][3] != -1 and cv2.contourArea(c) >= 350:
            cv2.fillPoly(out, [c.astype(np.int32)], 0)
    return out.astype(bool)


def save(path, mask):
    mask = smooth_contour(mask)
    soft = ndi.gaussian_filter(mask.astype(np.float32), .9)
    alpha = np.clip(soft*255*1.08, 0, 255).astype(np.uint8)
    rgba = np.zeros((*mask.shape,4), np.uint8)
    rgba[:,:,:3] = 255
    rgba[:,:,3] = alpha
    Image.fromarray(rgba, "RGBA").save(path)


def main():
    work = Path(sys.argv[1]).resolve()
    (work/"front").mkdir(parents=True, exist_ok=True)
    (work/"back").mkdir(parents=True, exist_ok=True)
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    for side in ("front", "back"):
        im, body, grad = load_base(side)
        entries = []
        for item in manifest["muscles"]:
            hint = item["muscleIdHint"]
            app_id = "deltoid_lateral" if hint == "deltoid_middle" else hint
            file = Path(item[f"{side}Mask"]).name
            prior = np.array(Image.open(MASK_DIR/side/file).convert("RGBA").getchannel("A")) >= ALPHA
            entries.append({"id": app_id, "file": file, "prior": prior})
        # Quadriceps is derived from its children, not a separate watershed seed.
        entries = [e for e in entries if e["id"] != "quadriceps"]
        # Smaller, more specific masks claim overlapping seed pixels first.
        entries.sort(key=lambda e: int(e["prior"].sum()))
        labels = np.zeros(body.shape, np.int32)
        id_by_label = {}
        next_label = 1
        for e in entries:
            if not e["prior"].any():
                continue
            seed = ndi.binary_erosion(e["prior"], iterations=3) & body
            if not seed.any():
                seed = e["prior"] & body
            seed &= labels == 0
            if not seed.any():
                # Keep a small center seed if the prior is entirely overlapped.
                ys, xs = np.where(e["prior"] & body & (labels == 0))
                if len(xs) == 0:
                    continue
                seed = np.zeros_like(body)
                seed[ys[len(ys)//2], xs[len(xs)//2]] = True
            labels[seed] = next_label
            id_by_label[next_label] = e
            next_label += 1
        # Background: body pixels outside every prior's local neighborhood.
        covered = np.zeros_like(body)
        for e in entries:
            if e["prior"].any():
                covered |= ndi.binary_dilation(e["prior"], iterations=24)
        labels[body & ~covered] = 2
        ws = ndi.watershed_ift(grad, labels)
        out = {}
        for label, e in id_by_label.items():
            region = (ws == label) & body
            allowed = ndi.binary_dilation(e["prior"], iterations=26) & body
            region &= allowed
            if region.sum() < max(80, e["prior"].sum()*0.2):
                region = e["prior"] & body
            out[e["id"]] = region
        # Adjacent masks must not cover each other. Give the smaller/specific
        # mask the overlap and remove it from the larger mask. Parent/child
        # overlaps are intentional and are handled separately below.
        ids = list(out)
        for i, a in enumerate(ids):
            for b in ids[i + 1:]:
                if PARENT.get(a) == b or PARENT.get(b) == a:
                    continue
                overlap = out[a] & out[b]
                if not overlap.any():
                    continue
                if out[a].sum() <= out[b].sum():
                    out[b] = out[b] & ~overlap
                else:
                    out[a] = out[a] & ~overlap
        # Parent composite: union of children, with non-child neighbors removed.
        heads = [out.get(h) for h in CHILDREN["quadriceps"]]
        heads = [h for h in heads if h is not None]
        if heads:
            union = np.zeros_like(body)
            for h in heads:
                union |= h
            for other_id, other in out.items():
                if other_id in CHILDREN["quadriceps"]:
                    continue
                union &= ~other
            out["quadriceps"] = union
        # Write every manifest file, including intentionally empty sides.
        for item in manifest["muscles"]:
            hint = item["muscleIdHint"]
            app_id = "deltoid_lateral" if hint == "deltoid_middle" else hint
            file = Path(item[f"{side}Mask"]).name
            mask = out.get(app_id, np.zeros_like(body))
            save(work/side/file, mask)
    print("joint candidates written", work)


if __name__ == "__main__":
    main()
