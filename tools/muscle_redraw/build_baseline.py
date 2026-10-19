from pathlib import Path
import csv
import hashlib
import json
import math
import colorsys
from datetime import datetime, timezone

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "tools" / "muscle_redraw"
BASELINE = OUT / "baseline"
PREVIEWS = OUT / "previews"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
MASK_DIR = ASSETS / "muscle_masks"
MANIFEST = ASSETS / "muscle_manifest.json"
BASE_FRONT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi" / "muscle_front_base.png"
BASE_BACK = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi" / "muscle_back_base.png"

CANVAS = (1024, 1536)
ALPHA_THRESHOLD = 16
HIDDEN_IDS = {"rhomboids", "brachialis", "internal_oblique", "transversus_abdominis"}
PARENT_OF = {
    "rectus_femoris": "quadriceps",
    "vastus_lateralis": "quadriceps",
    "vastus_medialis": "quadriceps",
}
CHILDREN_OF = {
    "quadriceps": ["rectus_femoris", "vastus_lateralis", "vastus_medialis"],
}
ASSET_ALIAS = {"deltoid_lateral": "deltoid_middle"}

REVIEW_NOTES = {
    "pectoralis_major": "边界可接受；与三角肌前束约 9.6% 邻接重叠。",
    "deltoid_anterior": "与三角肌中束约 24.9% 重叠（中束 61% 落在前束内）。",
    "deltoid_lateral": "应用 ID 使用 deltoid_middle.png；越界约 24.7%，大部分区域与前束重叠。",
    "deltoid_posterior": "越界 0；与肱三头肌约 4.9% 重叠。",
    "trapezius_upper": "越界约 27.2%，外缘伸到肩外侧透明区。",
    "trapezius_middle": "与斜方肌下部约 30.2% 重叠。",
    "trapezius_lower": "与斜方肌中部约 30.2% 重叠，且与竖脊肌约 24.9% 重叠。",
    "latissimus_dorsi": "越界约 25.8%；大圆肌约 38.8% 落在背阔肌遮罩内。",
    "teres_major": "自身越界 0；约 38.8% 落在背阔肌遮罩内。",
    "erector_spinae": "下段覆盖不足，存在 2 个碎片；与下斜方约 24.9% 重叠。",
    "biceps_brachii": "边界可接受；与三角肌前束约 9.3% 邻接重叠。",
    "triceps_brachii": "越界约 11.8%。",
    "brachioradialis": "正面伸肌群 100% 落在肱桡肌内；屈肌群约 25.7% 被覆盖。",
    "forearm_flexors": "越界约 21.1%。",
    "forearm_extensors": "正面 100% 落在肱桡肌内；背面越界约 12.0%。",
    "rectus_abdominis": "边界可接受；分节色块，不是纤维描边。",
    "external_oblique": "越界约 26.7%，下缘提前结束。",
    "gluteus_maximus": "边界可接受；与臀中肌约 5.8% 重叠。",
    "gluteus_medius": "约 25.6% 落在臀大肌内，需人工判断是邻接还是过宽。",
    "quadriceps": "合并遮罩；与缝匠肌约 82.4%、内收肌约 48.9% 重叠；自身越界约 1.7%。",
    "rectus_femoris": "与股内侧肌约 54.3%、缝匠肌约 49.6%、内收肌约 40.6% 重叠。",
    "vastus_lateralis": "边界可接受；100% 位于股四头肌合成遮罩内，属设计关系。",
    "vastus_medialis": "越界约 16.6%；与股直肌约 54.3% 重叠。",
    "hamstrings": "越界约 4.8%，轮廓需人工确认。",
    "adductors": "越界约 30.3%；约 48.9% 落在股四头肌合成遮罩内。",
    "sartorius": "约 82.4% 落在股四头肌合成遮罩内；正面有独立遮罩，背面为空。",
    "gastrocnemius": "边界可接受；与比目鱼肌约 0.4% 重叠。",
    "soleus": "轮廓需人工确认；与腓肠肌几乎不重叠。",
    "tibialis_anterior": "轮廓需人工确认；越界 0。",
    "rhomboids": "现有设计为无独立遮罩（深层/不可见），不映射到邻近表层肌肉。",
    "brachialis": "现有设计为无独立遮罩（深层/不可见），不映射到邻近表层肌肉。",
    "internal_oblique": "现有设计为无独立遮罩（深层/不可见），不映射到邻近表层肌肉。",
    "transversus_abdominis": "现有设计为无独立遮罩（深层/不可见），不映射到邻近表层肌肉。",
}


def font(size: int):
    for candidate in (
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\arial.ttf",
        r"C:\Windows\Fonts\segoeui.ttf",
    ):
        try:
            return ImageFont.truetype(candidate, size)
        except Exception:
            continue
    return ImageFont.load_default()


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def base_stats(path: Path):
    im = Image.open(path).convert("RGBA")
    alpha = im.getchannel("A")
    hist = alpha.histogram()
    return {
        "size": list(im.size),
        "alpha_gt0": sum(hist[1:]),
        "bbox": list(alpha.getbbox()) if alpha.getbbox() else None,
        "sha256": sha256(path),
    }


def mask_stats(path: Path):
    im = Image.open(path).convert("RGBA")
    alpha = im.getchannel("A")
    hist = alpha.histogram()
    hit = alpha.point(lambda v: 255 if v >= ALPHA_THRESHOLD else 0)
    hit_pixels = sum(hit.histogram()[1:])
    bbox = alpha.getbbox()
    # weighted center of hit pixels
    if hit_pixels:
        pixels = hit.load()
        w, h = hit.size
        sx = sy = count = 0
        for y in range(h):
            for x in range(w):
                if pixels[x, y]:
                    sx += x
                    sy += y
                    count += 1
        center = (sx / count, sy / count)
    else:
        center = None
    return {
        "size": list(im.size),
        "alpha_gt0": sum(hist[1:]),
        "hit_pixels": hit_pixels,
        "bbox": list(bbox) if bbox else None,
        "center": [round(center[0], 2), round(center[1], 2)] if center else None,
        "sha256": sha256(path),
    }


def tint_mask(base: Image.Image, mask: Image.Image, color, alpha=0.55):
    out = base.copy()
    tint = Image.new("RGBA", mask.size, (*color, 0))
    tint.putalpha(mask.getchannel("A").point(lambda v: int(v * alpha)))
    out.alpha_composite(tint)
    return out


def fit_panel(image: Image.Image, box: tuple[int, int]):
    copy = image.copy()
    copy.thumbnail(box, Image.LANCZOS)
    panel = Image.new("RGBA", box, (247, 249, 252, 255))
    panel.alpha_composite(copy, ((box[0] - copy.width) // 2, (box[1] - copy.height) // 2))
    return panel


def draw_contact(side: str, rows, base: Image.Image, out_path: Path):
    cols = 5
    tw, th = 240, 360
    label_h = 34
    line_count = (len(rows) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * tw, line_count * (th + label_h)), (247, 249, 252, 255))
    draw = ImageDraw.Draw(sheet)
    small = font(12)
    for index, row in enumerate(rows):
        x = (index % cols) * tw
        y = (index // cols) * (th + label_h)
        path = MASK_DIR / side / row[f"{side}File"]
        mask = Image.open(path).convert("RGBA")
        if row["hit_pixels"]:
            panel = fit_panel(tint_mask(base, mask, (220, 65, 75), 0.55), (tw, th))
        else:
            panel = fit_panel(base, (tw, th))
            d = ImageDraw.Draw(panel)
            d.text((10, th - 34), "EMPTY", fill=(180, 40, 40, 255), font=small)
        sheet.alpha_composite(panel, (x, y))
        draw.rectangle([x, y + th, x + tw, y + th + label_h], fill=(255, 255, 255, 255))
        color = (180, 40, 40, 255) if row["hit_pixels"] else (110, 110, 110, 255)
        draw.text((x + 6, y + th + 7), f'#{index + 1:02d} {row["id"]}', fill=color, font=small)
    sheet.convert("RGB").save(out_path)
    print("wrote", out_path)


def draw_partition(side: str, rows, base: Image.Image, out_path: Path):
    full = base.copy().convert("RGBA")
    draw_full = ImageDraw.Draw(full)
    title_font = font(20)
    legend_font = font(14)
    number_font = font(16)
    visible = [r for r in rows if r["hit_pixels"]]
    for index, row in enumerate(visible):
        hue = (index * 0.61803398875) % 1.0
        color = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.72, 0.9))
        mask = Image.open(MASK_DIR / side / row[f"{side}File"]).convert("RGBA")
        tint = Image.new("RGBA", mask.size, (*color, 0))
        tint.putalpha(mask.getchannel("A").point(lambda v: int(v * 0.48)))
        full.alpha_composite(tint)
        cx, cy = row["center"]
        r = 15
        draw_full.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(*color, 255), outline=(255, 255, 255, 255), width=2)
        text = str(row["index"])
        bbox = draw_full.textbbox((0, 0), text, font=number_font)
        draw_full.text((cx - (bbox[2] - bbox[0]) / 2, cy - (bbox[3] - bbox[1]) / 2 - 2), text, fill=(20, 20, 20, 255), font=number_font)
    legend_w = 520
    canvas = Image.new("RGBA", (full.width + legend_w, full.height), (247, 249, 252, 255))
    canvas.alpha_composite(full, (0, 0))
    draw = ImageDraw.Draw(canvas)
    draw.text((full.width + 18, 18), f"{side.upper()} — {len(visible)} visible masks", fill=(20, 30, 50, 255), font=title_font)
    y = 58
    for index, row in enumerate(visible):
        hue = (index * 0.61803398875) % 1.0
        color = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.72, 0.9))
        draw.rectangle([full.width + 20, y + 2, full.width + 34, y + 16], fill=(*color, 255))
        draw.text((full.width + 42, y), f'{row["index"]:02d} {row["id"]}', fill=(30, 40, 60, 255), font=legend_font)
        y += 25
    hidden = [r for r in rows if not r["hit_pixels"]]
    if hidden:
        y += 8
        draw.text((full.width + 18, y), "no mask on this side:", fill=(150, 90, 30, 255), font=legend_font)
        y += 22
        for row in hidden:
            draw.text((full.width + 42, y), f'- {row["id"]}', fill=(120, 80, 30, 255), font=legend_font)
            y += 22
    canvas.convert("RGB").save(out_path)
    print("wrote", out_path)


def draw_individual(side: str, rows, base: Image.Image):
    folder = PREVIEWS / side
    folder.mkdir(parents=True, exist_ok=True)
    for index, row in enumerate(rows):
        path = MASK_DIR / side / row[f"{side}File"]
        mask = Image.open(path).convert("RGBA")
        if row["hit_pixels"]:
            image = tint_mask(base, mask, (220, 65, 75), 0.55)
        else:
            image = base.copy()
        image = fit_panel(image, (512, 768))
        draw = ImageDraw.Draw(image)
        draw.rectangle([0, 0, 512, 32], fill=(255, 255, 255, 235))
        draw.text((8, 8), f'{side.upper()} #{index + 1:02d} {row["id"]}', fill=(20, 30, 50, 255), font=font(14))
        if not row["hit_pixels"]:
            draw.text((12, 740), "EMPTY", fill=(180, 40, 40, 255), font=font(18))
        image.convert("RGB").save(folder / f'{row["id"]}.png')


def main():
    BASELINE.mkdir(parents=True, exist_ok=True)
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    rows = []
    for index, item in enumerate(manifest["muscles"]):
        hint = item["muscleIdHint"]
        app_id = "deltoid_lateral" if hint == "deltoid_middle" else hint
        front_file = Path(item["frontMask"]).name
        back_file = Path(item["backMask"]).name
        front_path = MASK_DIR / "front" / front_file
        back_path = MASK_DIR / "back" / back_file
        front_stats = mask_stats(front_path)
        back_stats = mask_stats(back_path)
        front_visible = front_stats["hit_pixels"] > 0
        back_visible = back_stats["hit_pixels"] > 0
        if not front_visible and not back_visible and app_id in HIDDEN_IDS:
            overall = "no_independent_mask"
        elif front_visible or back_visible:
            overall = "drawn"
        else:
            overall = "pending_review"
        row = {
            "index": index + 1,
            "id": app_id,
            "manifestId": hint,
            "nameZh": item["nameZh"],
            "nameEn": item["nameEn"],
            "frontFile": front_file,
            "backFile": back_file,
            "frontStatus": "drawn" if front_visible else "empty_on_this_side",
            "backStatus": "drawn" if back_visible else "empty_on_this_side",
            "overallStatus": overall,
            "front": front_stats,
            "back": back_stats,
            "parent": PARENT_OF.get(app_id),
            "children": CHILDREN_OF.get(app_id, []),
            "relation": "composite" if app_id in CHILDREN_OF else ("child" if app_id in PARENT_OF else "single"),
            "assetAlias": ASSET_ALIAS.get(app_id),
            "reviewStatus": "pending_human_review",
            "reviewNotes": REVIEW_NOTES.get(app_id, ""),
        }
        if not front_visible and not back_visible:
            row["frontStatus"] = "no_independent_mask"
            row["backStatus"] = "no_independent_mask"
        rows.append(row)

    front_pngs = sorted(p.name for p in (MASK_DIR / "front").glob("*.png"))
    back_pngs = sorted(p.name for p in (MASK_DIR / "back").glob("*.png"))
    front_svgs = sorted(p.name for p in (MASK_DIR / "front").glob("*.svg"))
    back_svgs = sorted(p.name for p in (MASK_DIR / "back").glob("*.svg"))
    inventory = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "projectRoot": str(ROOT),
        "canvas": {"width": CANVAS[0], "height": CANVAS[1], "alphaThreshold": ALPHA_THRESHOLD},
        "paths": {
            "frontBase": str(BASE_FRONT.relative_to(ROOT)),
            "backBase": str(BASE_BACK.relative_to(ROOT)),
            "frontMasks": str((MASK_DIR / "front").relative_to(ROOT)),
            "backMasks": str((MASK_DIR / "back").relative_to(ROOT)),
            "manifest": str(MANIFEST.relative_to(ROOT)),
        },
        "bases": {
            "front": base_stats(BASE_FRONT),
            "back": base_stats(BASE_BACK),
            "manifestSha256": sha256(MANIFEST),
        },
        "counts": {
            "muscles": len(rows),
            "frontPng": len(front_pngs),
            "backPng": len(back_pngs),
            "frontSvg": len(front_svgs),
            "backSvg": len(back_svgs),
            "frontDrawn": sum(1 for r in rows if r["front"]["hit_pixels"] > 0),
            "backDrawn": sum(1 for r in rows if r["back"]["hit_pixels"] > 0),
            "noIndependentMask": sum(1 for r in rows if r["overallStatus"] == "no_independent_mask"),
        },
        "files": {
            "frontPng": front_pngs,
            "backPng": back_pngs,
            "frontSvg": front_svgs,
            "backSvg": back_svgs,
        },
        "muscles": rows,
    }
    (OUT / "inventory.json").write_text(json.dumps(inventory, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with (OUT / "inventory.csv").open("w", newline="", encoding="utf-8-sig") as fh:
        writer = csv.writer(fh)
        writer.writerow([
            "id", "nameZh", "nameEn", "frontFile", "backFile",
            "frontStatus", "backStatus", "frontPixels", "backPixels",
            "frontBbox", "backBbox", "parent", "children", "assetAlias",
            "reviewStatus", "reviewNotes",
        ])
        for row in rows:
            writer.writerow([
                row["id"], row["nameZh"], row["nameEn"], row["frontFile"], row["backFile"],
                row["frontStatus"], row["backStatus"],
                row["front"]["hit_pixels"], row["back"]["hit_pixels"],
                row["front"]["bbox"], row["back"]["bbox"],
                row["parent"] or "", ";".join(row["children"]), row["assetAlias"] or "",
                row["reviewStatus"], row["reviewNotes"],
            ])
    base_front = Image.open(BASE_FRONT).convert("RGBA")
    base_back = Image.open(BASE_BACK).convert("RGBA")
    side_rows = {
        side: [
            {
                **row,
                "file": row[f"{side}File"],
                "hit_pixels": row[side]["hit_pixels"],
                "center": row[side]["center"],
            }
            for row in rows
        ]
        for side in ("front", "back")
    }
    draw_contact("front", side_rows["front"], base_front, BASELINE / "front_all_masks.png")
    draw_contact("back", side_rows["back"], base_back, BASELINE / "back_all_masks.png")
    draw_partition("front", side_rows["front"], base_front, BASELINE / "front_partition.png")
    draw_partition("back", side_rows["back"], base_back, BASELINE / "back_partition.png")
    draw_individual("front", side_rows["front"], base_front)
    draw_individual("back", side_rows["back"], base_back)
    print(json.dumps(inventory["counts"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
