"""Read-only audit of the third-party exercise dataset."""
from pathlib import Path
import hashlib
import json
import re
from collections import Counter, defaultdict
from datetime import datetime
from statistics import mean

from PIL import Image

DATASET = Path(r"D:\exercises-dataset-zh-master\exercises-dataset-zh-master")
OUT = Path(__file__).resolve().parent
DATA = DATASET / "data" / "exercises.json"
SCHEMA = DATASET / "data" / "exercises.schema.json"
NAME_ZH = DATASET / "data" / "name_zh.json"
IMAGES = DATASET / "images"
VIDEOS = DATASET / "videos"


def norm(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", value.lower())


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    records = json.loads(DATA.read_text(encoding="utf-8"))
    name_zh = json.loads(NAME_ZH.read_text(encoding="utf-8"))
    schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
    image_files = sorted(p for p in IMAGES.iterdir() if p.is_file())
    gif_files = sorted(p for p in VIDEOS.iterdir() if p.is_file())
    image_by_name = {p.name: p for p in image_files}
    gif_by_name = {p.name: p for p in gif_files}
    image_stems = {p.stem for p in image_files}
    gif_stems = {p.stem for p in gif_files}

    ids = [r.get("id") for r in records]
    names = [r.get("name") for r in records]
    media_ids = [r.get("media_id") for r in records]
    duplicate_ids = sorted(k for k, v in Counter(ids).items() if v > 1)
    duplicate_names = sorted(k for k, v in Counter(names).items() if v > 1)
    duplicate_norm_names = sorted(k for k, v in Counter(norm(n) for n in names if n).items() if v > 1)
    duplicate_media_ids = sorted(k for k, v in Counter(media_ids).items() if v > 1)

    missing_fields = defaultdict(list)
    required = schema["$defs"]["exercise"]["required"]
    for r in records:
        for field in required:
            if field not in r or r[field] in (None, "", []):
                missing_fields[field].append(r.get("id"))

    lang_counts = Counter()
    steps_lang_counts = Counter()
    empty_instruction = Counter()
    steps_empty = Counter()
    step_mismatch = []
    for r in records:
        ins = r.get("instructions") or {}
        steps = r.get("instruction_steps") or {}
        lang_counts.update(ins.keys())
        steps_lang_counts.update(steps.keys())
        for lang, text in ins.items():
            if not str(text).strip():
                empty_instruction[lang] += 1
        for lang, arr in steps.items():
            if not arr:
                steps_empty[lang] += 1
        if set(ins) != set(steps):
            step_mismatch.append(r.get("id"))
    expected_langs = set(schema["$defs"]["languageMap"]["required"])
    missing_languages = sorted(expected_langs - set(lang_counts))
    missing_steps_languages = sorted(expected_langs - set(steps_lang_counts))

    referenced_images = []
    referenced_gifs = []
    ref_image_missing = []
    ref_gif_missing = []
    ref_image_extra_name_mismatch = []
    ref_gif_extra_name_mismatch = []
    for r in records:
        image = r.get("image")
        gif = r.get("gif_url")
        if image:
            referenced_images.append(image)
            path = DATASET / image
            if not path.exists():
                ref_image_missing.append((r.get("id"), image))
        if gif:
            referenced_gifs.append(gif)
            path = DATASET / gif
            if not path.exists():
                ref_gif_missing.append((r.get("id"), gif))
        expected_stem = f'{r.get("id")}-{r.get("media_id")}'
        if image and Path(image).stem != expected_stem:
            ref_image_extra_name_mismatch.append((r.get("id"), image, expected_stem))
        if gif and Path(gif).stem != expected_stem:
            ref_gif_extra_name_mismatch.append((r.get("id"), gif, expected_stem))

    image_referenced_names = {Path(x).name for x in referenced_images if x}
    gif_referenced_names = {Path(x).name for x in referenced_gifs if x}
    unreferenced_images = sorted(image_by_name.keys() - image_referenced_names)
    unreferenced_gifs = sorted(gif_by_name.keys() - gif_referenced_names)
    missing_image_by_id = sorted(set(ids) - {Path(x).name.split("-")[0] for x in referenced_images if x})
    missing_gif_by_id = sorted(set(ids) - {Path(x).name.split("-")[0] for x in referenced_gifs if x})

    def file_info(path: Path):
        return {"size": path.stat().st_size, "sha256": sha256(path)}

    image_info = []
    image_corrupt = []
    image_dimensions = Counter()
    for path in image_files:
        try:
            with Image.open(path) as im:
                image_dimensions[im.size] += 1
                image_info.append({"name": path.name, **file_info(path), "width": im.width, "height": im.height, "format": im.format})
        except Exception as exc:
            image_corrupt.append({"name": path.name, "error": str(exc)})
    gif_info = []
    gif_corrupt = []
    gif_dimensions = Counter()
    gif_frames = []
    for path in gif_files:
        try:
            with Image.open(path) as im:
                gif_dimensions[im.size] += 1
                gif_frames.append(getattr(im, "n_frames", 1))
                gif_info.append({"name": path.name, **file_info(path), "width": im.width, "height": im.height, "format": im.format, "frames": getattr(im, "n_frames", 1)})
        except Exception as exc:
            gif_corrupt.append({"name": path.name, "error": str(exc)})

    image_hashes = Counter(x["sha256"] for x in image_info)
    gif_hashes = Counter(x["sha256"] for x in gif_info)
    duplicate_image_hash = sorted(h for h, n in image_hashes.items() if n > 1)
    duplicate_gif_hash = sorted(h for h, n in gif_hashes.items() if n > 1)

    def counts(field):
        return Counter(str(r.get(field, "")).strip() for r in records).most_common()

    body_parts = counts("body_part")
    categories = counts("category")
    equipment = counts("equipment")
    muscle_groups = counts("muscle_group")
    targets = counts("target")
    secondary_counts = Counter()
    for r in records:
        for muscle in r.get("secondary_muscles") or []:
            secondary_counts[str(muscle).strip()] += 1

    chinese_values = list(name_zh.values()) if isinstance(name_zh, dict) else list(name_zh)
    chinese_keys = set(name_zh.keys()) if isinstance(name_zh, dict) else set()
    chinese_duplicates = sorted(k for k, v in Counter(chinese_values).items() if v > 1)
    # name_zh.json is keyed by the English exercise name, not by dataset id.
    missing_chinese_names = sorted(set(names) - chinese_keys)
    extra_chinese_names = sorted(chinese_keys - set(names))
    records_without_chinese = [r["id"] for r in records if r["name"] not in name_zh]
    ambiguous_chinese_records = [
        r["id"] for r in records if len([x for x in records if x["name"] == r["name"]]) > 1
    ]

    created = [r.get("created_at") for r in records if r.get("created_at")]
    created_sorted = sorted(created)
    total_media = sum(p.stat().st_size for p in image_files) + sum(p.stat().st_size for p in gif_files)
    image_sizes = [p.stat().st_size for p in image_files]
    gif_sizes = [p.stat().st_size for p in gif_files]

    summary = {
        "datasetRoot": str(DATASET),
        "generatedAt": datetime.now().astimezone().isoformat(),
        "recordCount": len(records),
        "idRange": [ids[0], ids[-1]] if ids else [],
        "uniqueIds": len(set(ids)),
        "uniqueNames": len(set(names)),
        "duplicateIds": duplicate_ids,
        "duplicateExactNames": duplicate_names,
        "duplicateNormalizedNames": duplicate_norm_names,
        "duplicateMediaIds": duplicate_media_ids,
        "missingRequiredFields": {k: v for k, v in missing_fields.items()},
        "languageCounts": dict(lang_counts),
        "stepLanguageCounts": dict(steps_lang_counts),
        "missingLanguages": missing_languages,
        "missingStepLanguages": missing_steps_languages,
        "emptyInstructions": dict(empty_instruction),
        "emptyStepArrays": dict(steps_empty),
        "instructionStepKeyMismatch": step_mismatch,
        "media": {
            "imageFiles": len(image_files),
            "gifFiles": len(gif_files),
            "imageBytes": sum(image_sizes),
            "gifBytes": sum(gif_sizes),
            "totalBytes": total_media,
            "imageDimensions": {f"{k[0]}x{k[1]}": v for k, v in image_dimensions.items()},
            "gifDimensions": {f"{k[0]}x{k[1]}": v for k, v in gif_dimensions.items()},
            "gifFrames": {
                "min": min(gif_frames) if gif_frames else 0,
                "max": max(gif_frames) if gif_frames else 0,
                "avg": round(mean(gif_frames), 2) if gif_frames else 0,
                "distribution": {str(k): v for k, v in sorted(Counter(gif_frames).items())},
            },
            "imageSizeBytes": {"min": min(image_sizes), "max": max(image_sizes), "avg": round(mean(image_sizes), 1)},
            "gifSizeBytes": {"min": min(gif_sizes), "max": max(gif_sizes), "avg": round(mean(gif_sizes), 1)},
            "imageCorrupt": image_corrupt,
            "gifCorrupt": gif_corrupt,
            "duplicateImageHashes": duplicate_image_hash,
            "duplicateGifHashes": duplicate_gif_hash,
        },
        "references": {
            "imageReferenced": len(referenced_images),
            "gifReferenced": len(referenced_gifs),
            "missingImageRefs": ref_image_missing,
            "missingGifRefs": ref_gif_missing,
            "imageNameMismatch": ref_image_extra_name_mismatch,
            "gifNameMismatch": ref_gif_extra_name_mismatch,
            "unreferencedImages": unreferenced_images,
            "unreferencedGifs": unreferenced_gifs,
            "missingImageId": missing_image_by_id,
            "missingGifId": missing_gif_by_id,
        },
        "categories": {
            "bodyPart": body_parts,
            "category": categories,
            "equipment": equipment,
            "muscleGroup": muscle_groups,
            "target": targets,
            "secondaryMuscles": secondary_counts.most_common(),
        },
        "nameZh": {
            "mappingType": type(name_zh).__name__,
            "keyType": "English exercise name",
            "count": len(name_zh),
            "uniqueEnglishNames": len(set(names)),
            "missingNames": missing_chinese_names,
            "extraNames": extra_chinese_names,
            "recordsWithoutTranslation": records_without_chinese,
            "recordsWithDuplicateEnglishName": ambiguous_chinese_records,
            "duplicateValues": chinese_duplicates,
            "sample": list(name_zh.items())[:8] if isinstance(name_zh, dict) else name_zh[:8],
        },
        "createdAt": {"min": created_sorted[0], "max": created_sorted[-1]} if created_sorted else {},
        "attributionPresent": sum(1 for r in records if r.get("attribution")),
    }
    (OUT / "dataset_audit.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "records": summary["recordCount"],
        "images": summary["media"]["imageFiles"],
        "gifs": summary["media"]["gifFiles"],
        "mediaMiB": round(summary["media"]["totalBytes"] / 1024 / 1024, 2),
        "duplicateIds": len(duplicate_ids),
        "duplicateNames": len(duplicate_names),
        "missingImageRefs": len(ref_image_missing),
        "missingGifRefs": len(ref_gif_missing),
        "nameZh": summary["nameZh"]["count"],
        "missingChineseNames": len(missing_chinese_names),
        "recordsWithoutChinese": len(records_without_chinese),
        "recordsWithDuplicateEnglishName": len(ambiguous_chinese_records),
        "imageDims": summary["media"]["imageDimensions"],
        "gifDims": summary["media"]["gifDimensions"],
    }, ensure_ascii=False, indent=2))
    print("wrote", OUT / "dataset_audit.json")


if __name__ == "__main__":
    main()
