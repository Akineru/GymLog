"""Read-only mapping analysis between the dataset and GymLog."""
from pathlib import Path
import json
import re
from collections import Counter, defaultdict

ROOT = Path(r"C:\Users\75192\AndroidStudioProjects\GymLog")
DATASET = Path(r"D:\exercises-dataset-zh-master\exercises-dataset-zh-master")
OUT = Path(__file__).resolve().parent
records = json.loads((DATASET / "data" / "exercises.json").read_text(encoding="utf-8"))
name_zh = json.loads((DATASET / "data" / "name_zh.json").read_text(encoding="utf-8"))

BODY_MAP = {
    "chest": "CHEST",
    "shoulders": "SHOULDERS",
    "back": "BACK",
    "upper legs": "LEGS",
    "lower legs": "LEGS",
    "upper arms": "ARMS",
    "lower arms": "ARMS",
    "waist": "ABS",
    "cardio": "OTHER",
    "neck": "OTHER",
}
EQUIPMENT_MAP = {
    "body weight": "FREE_WEIGHT",
    "dumbbell": "FREE_WEIGHT",
    "barbell": "FREE_WEIGHT",
    "ez barbell": "FREE_WEIGHT",
    "olympic barbell": "FREE_WEIGHT",
    "trap bar": "FREE_WEIGHT",
    "kettlebell": "FREE_WEIGHT",
    "weighted": "FREE_WEIGHT",
    "band": "FREE_WEIGHT",
    "resistance band": "FREE_WEIGHT",
    "medicine ball": "FREE_WEIGHT",
    "rope": "FREE_WEIGHT",
    "roller": "OTHER",
    "wheel roller": "OTHER",
    "hammer": "OTHER",
    "stability ball": "OTHER",
    "bosu ball": "OTHER",
    "assisted": "OTHER",
    "cable": "MACHINE",
    "leverage machine": "MACHINE",
    "smith machine": "MACHINE",
    "sled machine": "MACHINE",
    "stationary bike": "MACHINE",
    "elliptical machine": "MACHINE",
    "stepmill machine": "MACHINE",
    "skierg machine": "MACHINE",
    "upper body ergometer": "MACHINE",
}
WEIGHT_MODE_MAP = {
    "body weight": "BODYWEIGHT",
    "assisted": "BODYWEIGHT",
    "weighted": "BODYWEIGHT_PLUS",
}
MUSCLE_MAP = {
    "pectorals": ["pectoralis_major"],
    "chest": ["pectoralis_major"],
    "upper chest": ["pectoralis_major"],
    "biceps": ["biceps_brachii"],
    "triceps": ["triceps_brachii"],
    "brachialis": ["brachialis"],
    "forearms": ["forearm_flexors", "forearm_extensors"],
    "wrist flexors": ["forearm_flexors"],
    "wrist extensors": ["forearm_extensors"],
    "lats": ["latissimus_dorsi"],
    "latissimus dorsi": ["latissimus_dorsi"],
    "rhomboids": ["rhomboids"],
    "trapezius": ["trapezius_upper", "trapezius_middle", "trapezius_lower"],
    "traps": ["trapezius_upper", "trapezius_middle", "trapezius_lower"],
    "upper back": ["trapezius_middle"],
    "spine": ["erector_spinae"],
    "lower back": ["erector_spinae"],
    "abs": ["rectus_abdominis"],
    "abdominals": ["rectus_abdominis"],
    "lower abs": ["rectus_abdominis"],
    "core": ["rectus_abdominis"],
    "obliques": ["external_oblique"],
    "delts": ["deltoid_anterior", "deltoid_lateral", "deltoid_posterior"],
    "deltoids": ["deltoid_anterior", "deltoid_lateral", "deltoid_posterior"],
    "shoulders": ["deltoid_anterior", "deltoid_lateral", "deltoid_posterior"],
    "rear deltoids": ["deltoid_posterior"],
    "glutes": ["gluteus_maximus"],
    "quadriceps": ["quadriceps"],
    "quads": ["quadriceps"],
    "hamstrings": ["hamstrings"],
    "adductors": ["adductors"],
    "inner thighs": ["adductors"],
    "calves": ["gastrocnemius", "soleus"],
    "soleus": ["soleus"],
    "shins": ["tibialis_anterior"],
}
UNMAPPED_MUSCLES = {
    "hip flexors", "cardiovascular system", "ankles", "ankle stabilizers",
    "feet", "serratus anterior", "abductors", "wrists", "hands",
    "levator scapulae", "sternocleidomastoid", "groin", "grip muscles",
    "rotator cuff",
}

# Parse PresetExercises and PresetMuscleMappings enough for a read-only overlap.
preset_src = (ROOT / "app/src/main/java/com/example/gymlog/data/PresetExercises.kt").read_text(encoding="utf-8")
pattern = re.compile(
    r'preset\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*BodyPart\.([A-Z_]+)\s*,\s*'
    r'EquipmentType\.([A-Z_]+)\s*,\s*WeightMode\.([A-Z_]+)\s*,\s*"([^"]*)"',
)
presets = []
for m in pattern.finditer(preset_src):
    presets.append({
        "id": "preset_" + m.group(1),
        "key": m.group(1),
        "nameZh": m.group(2),
        "bodyPart": m.group(3),
        "equipmentType": m.group(4),
        "weightMode": m.group(5),
        "aliases": [x.strip() for x in m.group(6).split(",") if x.strip()],
    })

# Dataset lookups.
records_by_name = defaultdict(list)
records_by_zh = defaultdict(list)
for r in records:
    records_by_name[r["name"]].append(r)
    zh = name_zh.get(r["name"])
    if zh:
        records_by_zh[zh].append(r)

def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())

records_by_norm = defaultdict(list)
for r in records:
    records_by_norm[norm(r["name"])].append(r)

preset_matches = []
for p in presets:
    candidates = []
    for r in records_by_zh.get(p["nameZh"], []):
        candidates.append((r, "zh_name"))
    for alias in p["aliases"]:
        for r in records_by_norm.get(norm(alias), []):
            candidates.append((r, "alias:" + alias))
    for r in records_by_norm.get(norm(p["key"].replace("_", " ")), []):
        candidates.append((r, "key"))
    # De-duplicate by dataset id while preserving evidence.
    evidence = defaultdict(list)
    for r, why in candidates:
        evidence[r["id"]].append(why)
    if not evidence:
        preset_matches.append({**p, "match": None, "status": "NO_MATCH"})
        continue
    if len(evidence) == 1:
        dataset_id = next(iter(evidence))
        preset_matches.append({**p, "match": dataset_id, "status": "UNIQUE", "evidence": evidence[dataset_id]})
    else:
        preset_matches.append({
            **p, "match": sorted(evidence), "status": "AMBIGUOUS",
            "evidence": {k: v for k, v in evidence.items()},
        })

# Chinese mapping by English name.
zh_missing = []
zh_ambiguous = []
for r in records:
    zh = name_zh.get(r["name"])
    if not zh:
        zh_missing.append(r["id"])
    if len(records_by_name[r["name"]]) > 1:
        zh_ambiguous.append(r["id"])

# Duplicate name details.
dup_name_details = []
for name, rows in records_by_name.items():
    if len(rows) > 1:
        dup_name_details.append({
            "name": name,
            "ids": [r["id"] for r in rows],
            "zh": name_zh.get(name),
            "mediaIds": [r["media_id"] for r in rows],
        })

# Category mapping.
body_counts = Counter()
body_unmapped = Counter()
for r in records:
    mapped = BODY_MAP.get(r["body_part"])
    if mapped:
        body_counts[mapped] += 1
    else:
        body_unmapped[r["body_part"]] += 1

equipment_counts = Counter()
equipment_unmapped = Counter()
for r in records:
    mapped = EQUIPMENT_MAP.get(r["equipment"])
    if mapped:
        equipment_counts[mapped] += 1
    else:
        equipment_unmapped[r["equipment"]] += 1

weight_counts = Counter()
for r in records:
    weight_counts[WEIGHT_MODE_MAP.get(r["equipment"], "WEIGHTED")] += 1

muscle_counts = Counter()
for r in records:
    for token in [r["target"], r["muscle_group"], *r["secondary_muscles"]]:
        token = str(token).strip()
        if token:
            muscle_counts[token] += 1

mapped_tokens = {}
unmapped_tokens = {}
for token, count in muscle_counts.items():
    if token in MUSCLE_MAP:
        mapped_tokens[token] = {"count": count, "ids": MUSCLE_MAP[token]}
    else:
        unmapped_tokens[token] = count

# Count exercises with at least one mapped target and all three token types.
target_mapped = 0
any_mapped = 0
for r in records:
    target = str(r["target"]).strip()
    if target in MUSCLE_MAP:
        target_mapped += 1
    tokens = [r["target"], r["muscle_group"], *r["secondary_muscles"]]
    if any(str(t).strip() in MUSCLE_MAP for t in tokens):
        any_mapped += 1

summary = {
    "presetCount": len(presets),
    "presetMatchStatus": dict(Counter(p["status"] for p in preset_matches)),
    "presetMatches": preset_matches,
    "datasetRecords": len(records),
    "datasetUniqueNames": len(records_by_name),
    "chineseMappingEntries": len(name_zh),
    "chineseMissingByName": zh_missing,
    "chineseAmbiguousByDuplicateName": zh_ambiguous,
    "duplicateNameDetails": dup_name_details,
    "bodyPartMap": BODY_MAP,
    "bodyPartMappedCounts": body_counts,
    "bodyPartUnmapped": body_unmapped,
    "equipmentMap": EQUIPMENT_MAP,
    "equipmentMappedCounts": equipment_counts,
    "equipmentUnmapped": equipment_unmapped,
    "weightModeDerivedCounts": weight_counts,
    "muscleTokenCount": len(muscle_counts),
    "muscleTokensMapped": mapped_tokens,
    "muscleTokensUnmapped": unmapped_tokens,
    "recordsWithMappedTarget": target_mapped,
    "recordsWithAnyMappedMuscle": any_mapped,
}
(OUT / "mapping_audit.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({
    "presets": len(presets),
    "presetStatus": summary["presetMatchStatus"],
    "uniqueNames": len(records_by_name),
    "zhEntries": len(name_zh),
    "zhMissing": len(zh_missing),
    "zhAmbiguous": len(zh_ambiguous),
    "bodyMapped": dict(body_counts),
    "bodyUnmapped": dict(body_unmapped),
    "equipmentMapped": dict(equipment_counts),
    "equipmentUnmapped": dict(equipment_unmapped),
    "weightMode": dict(weight_counts),
    "muscleTokens": len(muscle_counts),
    "mappedTokens": len(mapped_tokens),
    "unmappedTokens": unmapped_tokens,
    "targetMapped": target_mapped,
    "anyMapped": any_mapped,
}, ensure_ascii=False, indent=2))
