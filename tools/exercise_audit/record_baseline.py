from pathlib import Path
import json
import re

root = Path(r"C:\Users\75192\AndroidStudioProjects\GymLog")
presets_src = (root / "app/src/main/java/com/example/gymlog/data/PresetExercises.kt").read_text(encoding="utf-8")
mapping_src = (root / "app/src/main/java/com/example/gymlog/data/PresetMuscleMappings.kt").read_text(encoding="utf-8")
preset_ids = ["preset_" + value for value in re.findall(r'preset\(\s*"([^"]+)"', presets_src)]
mapping_ids = re.findall(r'p\("([^"]+)"', mapping_src)
out = {
    "generatedAt": "2026-10-18T14:35:00+08:00",
    "roomVersion": 12,
    "backupVersion": 2,
    "presetCount": len(preset_ids),
    "presetIds": preset_ids,
    "presetMuscleMappingCount": len(mapping_ids),
    "muscleIdCount": 33,
    "accessibleUserDatabase": False,
    "note": "No user gymlog.db is present in the workspace; custom action, set and PR counts cannot be measured without an exported database or backup.",
    "files": {
        "database": "app/src/main/java/com/example/gymlog/data/GymLogDatabase.kt",
        "backup": "app/src/main/java/com/example/gymlog/data/BackupCodec.kt",
    },
}
(root / "tools/exercise_audit/baseline_before_phase2.json").write_text(
    json.dumps(out, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
print("presets", len(preset_ids), "mappings", len(mapping_ids))
