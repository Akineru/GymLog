from pathlib import Path
import csv
import json
from collections import Counter
from PIL import Image

DATASET = Path(r"D:\exercises-dataset-zh-master\exercises-dataset-zh-master")
OUT = Path(__file__).resolve().parent
records = json.loads((DATASET / "data" / "exercises.json").read_text(encoding="utf-8"))
name_zh = json.loads((DATASET / "data" / "name_zh.json").read_text(encoding="utf-8"))
dup_names = {name for name, count in Counter(r["name"] for r in records).items() if count > 1}

with (OUT / "dataset_records.csv").open("w", newline="", encoding="utf-8-sig") as fh:
    writer = csv.writer(fh)
    writer.writerow([
        "id", "name", "nameZh", "body_part", "equipment", "muscle_group", "target",
        "secondary_muscles", "image", "gif_url", "media_id", "chinese_name_ambiguous",
    ])
    for r in records:
        writer.writerow([
            r["id"], r["name"], name_zh.get(r["name"], ""), r["body_part"], r["equipment"],
            r["muscle_group"], r["target"], ";".join(r["secondary_muscles"]),
            r["image"], r["gif_url"], r["media_id"],
            "yes" if r["name"] in dup_names else "no",
        ])

with (OUT / "dataset_media.csv").open("w", newline="", encoding="utf-8-sig") as fh:
    writer = csv.writer(fh)
    writer.writerow(["kind", "name", "bytes", "width", "height", "frames", "referenced"])
    for folder, kind in (("images", "thumbnail"), ("videos", "gif")):
        for path in sorted((DATASET / folder).iterdir()):
            if not path.is_file():
                continue
            with Image.open(path) as im:
                writer.writerow([
                    kind, path.name, path.stat().st_size, im.width, im.height,
                    getattr(im, "n_frames", 1), "yes",
                ])
print("dataset tables written")
