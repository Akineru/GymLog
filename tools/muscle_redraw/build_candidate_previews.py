from pathlib import Path
import json
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
WORK = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r"C:\Users\75192\AndroidStudioProjects\GymLog-muscle-redraw-work-20261018-1355")
OUT = WORK / "previews"
BEFORE = ROOT / "tools/muscle_redraw/previews"
BASE = ROOT / "app/src/main/res/drawable-nodpi"
INVENTORY = ROOT / "tools/muscle_redraw/inventory.json"


def font(size):
    for candidate in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\arial.ttf"):
        try:
            return ImageFont.truetype(candidate, size)
        except Exception:
            pass
    return ImageFont.load_default()


def tint(base, mask, color=(220, 65, 75), alpha=0.55):
    out = base.copy()
    t = Image.new("RGBA", mask.size, (*color, 0))
    t.putalpha(mask.getchannel("A").point(lambda v: int(v * alpha)))
    out.alpha_composite(t)
    return out


def fit(image, box):
    out = image.copy()
    out.thumbnail(box, Image.LANCZOS)
    panel = Image.new("RGBA", box, (247, 249, 252, 255))
    panel.alpha_composite(out, ((box[0]-out.width)//2, (box[1]-out.height)//2))
    return panel


def contact(rows, side, base, path, candidate=True):
    cols, tw, th, lh = 5, 240, 360, 34
    count = (len(rows)+cols-1)//cols
    sheet = Image.new("RGBA", (cols*tw, count*(th+lh)), (247,249,252,255))
    draw = ImageDraw.Draw(sheet); f = font(12)
    for i,row in enumerate(rows):
        x=(i%cols)*tw; y=(i//cols)*(th+lh)
        mask_path = (WORK if candidate else root_previews(side)) / side / row[f"{side}File"]
        mask = Image.open(mask_path).convert("RGBA")
        if mask.getchannel("A").getbbox() is not None:
            panel = fit(tint(base, mask), (tw,th))
        else:
            panel = fit(base, (tw,th))
            ImageDraw.Draw(panel).text((10,th-34), "EMPTY", fill=(180,40,40,255), font=f)
        sheet.alpha_composite(panel,(x,y))
        draw.rectangle([x,y+th,x+tw,y+th+lh], fill=(255,255,255,255))
        draw.text((x+6,y+th+7), f'#{row["index"]:02d} {row["id"]}', fill=(20,30,50,255), font=f)
    sheet.convert("RGB").save(path)


def root_previews(side):
    return ROOT / "tools/muscle_redraw/previews"


def main():
    OUT.mkdir(exist_ok=True)
    inv = json.loads(INVENTORY.read_text(encoding="utf-8"))
    rows = inv["muscles"]
    bf = Image.open(BASE/"muscle_front_base.png").convert("RGBA")
    bb = Image.open(BASE/"muscle_back_base.png").convert("RGBA")
    contact(rows,"front",bf,OUT/"candidate_front_all.png",True)
    contact(rows,"back",bb,OUT/"candidate_back_all.png",True)
    # per-muscle comparison for the key regions
    key = {"latissimus_dorsi","teres_major","quadriceps","rectus_femoris","vastus_medialis","sartorius","adductors","deltoid_anterior","deltoid_lateral","trapezius_upper","external_oblique","forearm_flexors","forearm_extensors","triceps_brachii"}
    for side, base in (("front",bf),("back",bb)):
        selected=[r for r in rows if r["id"] in key]
        cols=2; tw,th,lh=400,600,34
        sheet=Image.new("RGBA",(cols*tw, len(selected)*(th+lh)),(247,249,252,255)); d=ImageDraw.Draw(sheet); f=font(16)
        for i,row in enumerate(selected):
            y=i*(th+lh)
            before=Image.open(BEFORE/side/f'{row["id"]}.png').convert("RGB") if (BEFORE/side/f'{row["id"]}.png').exists() else None
            after_path=WORK/side/row[f"{side}File"]
            after=Image.open(after_path).convert("RGBA")
            if after.getchannel("A").getbbox() is not None:
                after_panel=fit(tint(base,after),(tw,th))
            else:
                after_panel=fit(base,(tw,th))
            if before:
                before_panel=fit(before.convert("RGBA"),(tw,th))
                sheet.alpha_composite(before_panel,(0,y))
            sheet.alpha_composite(after_panel,(tw,y))
            d.text((6,y+th+6),f'{row["id"]} — BEFORE',fill=(20,30,50,255),font=f)
            d.text((tw+6,y+th+6),f'{row["id"]} — CANDIDATE ({side})',fill=(20,30,50,255),font=f)
        sheet.convert("RGB").save(OUT/f"compare_{side}_key.png")
    print("done", OUT)


if __name__ == "__main__":
    main()
