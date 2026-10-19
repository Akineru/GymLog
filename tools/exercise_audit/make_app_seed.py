from pathlib import Path
import hashlib
import json
from datetime import datetime

ROOT = Path(r"C:\Users\75192\AndroidStudioProjects\GymLog")
DATASET = Path(r"D:\exercises-dataset-zh-master\exercises-dataset-zh-master")
OUT = ROOT / "app/src/main/assets/exercises"
OUT.mkdir(parents=True, exist_ok=True)
records = json.loads((DATASET / "data/exercises.json").read_text(encoding="utf-8"))
name_zh = json.loads((DATASET / "data/name_zh.json").read_text(encoding="utf-8"))
mapping = json.loads((ROOT / "tools/exercise_audit/mapping_audit.json").read_text(encoding="utf-8"))

BODY_MAP = {
    "chest": "CHEST", "shoulders": "SHOULDERS", "back": "BACK",
    "upper legs": "LEGS", "lower legs": "LEGS",
    "upper arms": "ARMS", "lower arms": "ARMS",
    "waist": "ABS", "cardio": "OTHER", "neck": "OTHER",
}
EQUIPMENT_MAP = {
    "body weight": "FREE_WEIGHT", "dumbbell": "FREE_WEIGHT", "barbell": "FREE_WEIGHT",
    "ez barbell": "FREE_WEIGHT", "olympic barbell": "FREE_WEIGHT", "trap bar": "FREE_WEIGHT",
    "kettlebell": "FREE_WEIGHT", "weighted": "FREE_WEIGHT", "band": "FREE_WEIGHT",
    "resistance band": "FREE_WEIGHT", "medicine ball": "FREE_WEIGHT", "rope": "FREE_WEIGHT",
    "cable": "MACHINE", "leverage machine": "MACHINE", "smith machine": "MACHINE",
    "sled machine": "MACHINE", "stationary bike": "MACHINE", "elliptical machine": "MACHINE",
    "stepmill machine": "MACHINE", "skierg machine": "MACHINE", "upper body ergometer": "MACHINE",
    "stability ball": "OTHER", "bosu ball": "OTHER", "assisted": "OTHER",
    "roller": "OTHER", "wheel roller": "OTHER", "hammer": "OTHER", "tire": "OTHER",
}
WEIGHT_MODE = {
    "body weight": "BODYWEIGHT", "assisted": "BODYWEIGHT", "weighted": "BODYWEIGHT_PLUS",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def epoch_millis(value: str) -> int:
    return int(datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp() * 1000)


seed_records = []
for index, r in enumerate(records, 1):
    zh = name_zh.get(r["name"], "")
    seed_records.append({
        "datasetId": r["id"],
        "name": zh or r["name"],
        "nameEn": r["name"],
        "bodyPart": BODY_MAP[r["body_part"]],
        "bodyPartDetail": r["body_part"],
        "equipmentType": EQUIPMENT_MAP.get(r["equipment"], "OTHER"),
        "equipmentDetail": r["equipment"],
        "weightMode": WEIGHT_MODE.get(r["equipment"], "WEIGHTED"),
        "target": r["target"],
        "muscleGroup": r["muscle_group"],
        "secondaryMuscles": r["secondary_muscles"],
        "image": r["image"],
        "gif": r["gif_url"],
        "mediaId": r["media_id"],
        "attribution": r["attribution"],
        "imageBytes": (DATASET / r["image"]).stat().st_size,
        "gifBytes": (DATASET / r["gif_url"]).stat().st_size,
        "imageSha256": sha256(DATASET / r["image"]),
        "gifSha256": sha256(DATASET / r["gif_url"]),
        "instructionsZh": (r.get("instructions") or {}).get("zh"),
        "instructionsEn": (r.get("instructions") or {}).get("en"),
        "instructionStepsZh": (r.get("instruction_steps") or {}).get("zh") or [],
        "instructionStepsEn": (r.get("instruction_steps") or {}).get("en") or [],
        "createdAtEpochMillis": epoch_millis(r["created_at"]),
    })
    if index % 100 == 0:
        print("hashed", index)

seed = {
    "source": "exercises-dataset-zh",
    "catalogVersion": 1,
    "sourceRecordCount": len(seed_records),
    "generatedAt": datetime.now().astimezone().isoformat(),
    "attribution": "© Gym visual — https://gymvisual.com/",
    "records": seed_records,
}
(OUT / "dataset_v1.json").write_text(
    json.dumps(seed, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8",
)

candidates = []
for p in mapping["presetMatches"]:
    status = p["status"]
    if status == "UNIQUE":
        candidates.append({
            "presetExerciseId": p["id"],
            "nameZh": p["nameZh"],
            "datasetId": p["match"],
            "candidateDatasetIds": [p["match"]],
            "status": "PENDING_CONFIRMATION",
            "evidence": p.get("evidence"),
        })
    elif status == "AMBIGUOUS":
        candidates.append({
            "presetExerciseId": p["id"],
            "nameZh": p["nameZh"],
            "datasetId": None,
            "candidateDatasetIds": p["match"],
            "status": "AMBIGUOUS",
            "evidence": p.get("evidence"),
        })
    else:
        candidates.append({
            "presetExerciseId": p["id"],
            "nameZh": p["nameZh"],
            "datasetId": None,
            "candidateDatasetIds": [],
            "status": "NO_MATCH",
            "evidence": None,
        })
preset = {
    "source": "exercises-dataset-zh",
    "catalogVersion": 1,
    "generatedAt": seed["generatedAt"],
    "candidates": candidates,
}
(OUT / "preset_match_candidates.json").write_text(
    json.dumps(preset, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8",
)
(OUT / "NOTICE.txt").write_text(
    "Exercise data source: exercises-dataset-zh (fork of hasaneyldrm/exercises-dataset; "
    "ExerciseDB v1 by AscendAPI via Kaggle).\n"
    "Data/tooling: MIT. Media: © Gym visual — https://gymvisual.com/, 180x180, "
    "distributed under Gym visual terms; media is NOT bundled in this APK.\n"
    "Structured records are bundled for import; thumbnails/GIFs are registered only.\n",
    encoding="utf-8",
)
print("seed records", len(seed_records), "bytes", (OUT / "dataset_v1.json").stat().st_size)
print("preset candidates", len(candidates), "bytes", (OUT / "preset_match_candidates.json").stat().st_size)
