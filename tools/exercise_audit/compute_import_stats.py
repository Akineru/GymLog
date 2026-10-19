from pathlib import Path
import json
import re

root = Path(r"C:\Users\75192\AndroidStudioProjects\GymLog")
seed = json.loads((root / "app/src/main/assets/exercises/dataset_v1.json").read_text(encoding="utf-8"))
mapping = (root / "app/src/main/java/com/example/gymlog/data/DatasetMuscleMapping.kt").read_text(encoding="utf-8")
direct = set(re.findall(r'"([^"]+)" to mapped\(', mapping))
print("direct token count", len(direct))
token_rows = 0
unmapped = 0
links = 0
token_set = set()
records_with_no_direct = 0
for record in seed["records"]:
    rows = {}
    for bucket, token in [("TARGET", record["target"]), ("MUSCLE_GROUP", record["muscleGroup"])] + [
        ("SECONDARY", token) for token in record["secondaryMuscles"]
    ]:
        key = (bucket, token)
        if key in rows:
            continue
        rows[key] = token
        token_rows += 1
        token_set.add(token)
        if token not in direct:
            unmapped += 1
    direct_ids = {token for (_, token) in rows if token in direct}
    links += len(direct_ids)
    if not direct_ids:
        records_with_no_direct += 1
print("token rows", token_rows)
print("unique tokens used", len(token_set))
print("unmapped token occurrences", unmapped)
print("direct exercise_muscles links", links)
print("records with no direct mapped token", records_with_no_direct)
