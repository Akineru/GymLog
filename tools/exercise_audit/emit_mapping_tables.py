from pathlib import Path
import csv
import json
from collections import Counter

OUT = Path(__file__).resolve().parent
data = json.loads((OUT / "mapping_audit.json").read_text(encoding="utf-8"))
records = json.loads(
    Path(r"D:\exercises-dataset-zh-master\exercises-dataset-zh-master\data\exercises.json").read_text(encoding="utf-8")
)
body_counts = Counter(r["body_part"] for r in records)
equipment_counts = Counter(r["equipment"] for r in records)


def write(name, header, rows):
    with (OUT / name).open("w", newline="", encoding="utf-8-sig") as fh:
        writer = csv.writer(fh)
        writer.writerow(header)
        writer.writerows(rows)


write(
    "muscle_mapping_candidates.csv",
    ["token", "occurrences", "mapped_stable_ids", "status", "notes"],
    [
        [
            token,
            info["count"],
            ";".join(info["ids"]),
            "mapped",
            "generic group token; may map to multiple stable IDs" if len(info["ids"]) > 1 else "",
        ]
        for token, info in sorted(data["muscleTokensMapped"].items(), key=lambda x: -x[1]["count"])
    ] + [
        [token, count, "", "unmapped", "no stable 33-ID equivalent; keep explicit unmapped"]
        for token, count in sorted(data["muscleTokensUnmapped"].items(), key=lambda x: -x[1])
    ],
)

write(
    "body_part_mapping.csv",
    ["dataset_body_part", "mapped_BodyPart", "count", "status"],
    [
        [name, data["bodyPartMap"].get(name, ""), count, "mapped" if name in data["bodyPartMap"] else "unmapped"]
        for name, count in sorted(body_counts.items())
    ],
)

write(
    "equipment_mapping.csv",
    ["dataset_equipment", "mapped_EquipmentType", "count", "status"],
    [
        [name, data["equipmentMap"].get(name, ""), count, "mapped" if name in data["equipmentMap"] else "unmapped"]
        for name, count in sorted(equipment_counts.items())
    ],
)

write(
    "preset_match_candidates.csv",
    ["preset_id", "nameZh", "bodyPart", "equipmentType", "status", "dataset_id_or_ids", "evidence"],
    [
        [
            p["id"], p["nameZh"], p["bodyPart"], p["equipmentType"], p["status"],
            ";".join(p["match"]) if isinstance(p["match"], list) else (p["match"] or ""),
            json.dumps(p.get("evidence", ""), ensure_ascii=False),
        ]
        for p in data["presetMatches"]
    ],
)

write(
    "duplicate_names.csv",
    ["english_name", "dataset_ids", "chinese", "media_ids"],
    [
        [d["name"], ";".join(d["ids"]), d.get("zh") or "", ";".join(d["mediaIds"])]
        for d in data["duplicateNameDetails"]
    ],
)
print("tables written")
