from pathlib import Path
import json
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
WORK = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(r"C:\Users\75192\AndroidStudioProjects\GymLog-muscle-redraw-final3-20261018-1420")
INV = json.loads((ROOT/"tools/muscle_redraw/inventory.json").read_text(encoding="utf-8"))
ROWS = INV["muscles"]
BASE = ROOT/"app/src/main/res/drawable-nodpi"
BEFORE = ROOT/"tools/muscle_redraw/previews"
PREVIEW = WORK/"previews"
PREVIEW.mkdir(parents=True, exist_ok=True)


def font(size):
    for p in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\arial.ttf"):
        try: return ImageFont.truetype(p, size)
        except Exception: pass
    return ImageFont.load_default()


def fit(img, box):
    im = img.copy(); im.thumbnail(box, Image.LANCZOS)
    p = Image.new("RGBA", box, (247,249,252,255))
    p.alpha_composite(im, ((box[0]-im.width)//2, (box[1]-im.height)//2))
    return p


def tint(base, mask, alpha=.55):
    out = base.copy(); t = Image.new("RGBA", mask.size, (220,65,75,0))
    t.putalpha(mask.getchannel("A").point(lambda v:int(v*alpha))); out.alpha_composite(t); return out


def individual(side, base):
    folder = PREVIEW/side; folder.mkdir(parents=True, exist_ok=True)
    for row in ROWS:
        mask = Image.open(WORK/side/row[f"{side}File"]).convert("RGBA")
        if mask.getchannel("A").getbbox() is not None:
            image = tint(base, mask)
        else:
            image = base.copy()
        image = fit(image, (512,768))
        d = ImageDraw.Draw(image)
        d.rectangle([0,0,512,32], fill=(255,255,255,235))
        d.text((8,8), f'{side.upper()} #{row["index"]:02d} {row["id"]}', fill=(20,30,50,255), font=font(14))
        if mask.getchannel("A").getbbox() is None:
            d.text((12,740), "EMPTY", fill=(180,40,40,255), font=font(18))
        image.convert("RGB").save(folder/f'{row["id"]}.png')


def partition(side, base):
    full = base.copy().convert("RGBA")
    draw = ImageDraw.Draw(full)
    visible = [r for r in ROWS if (WORK/side/r[f"{side}File"]).exists() and Image.open(WORK/side/r[f"{side}File"]).convert("RGBA").getchannel("A").getbbox() is not None]
    # re-open to avoid PIL lazy handles
    for idx, row in enumerate(visible):
        mask = Image.open(WORK/side/row[f"{side}File"]).convert("RGBA")
        import colorsys
        hue = (idx*.61803398875)%1
        color = tuple(int(c*255) for c in colorsys.hsv_to_rgb(hue,.72,.9))
        t = Image.new("RGBA", mask.size, (*color,0))
        t.putalpha(mask.getchannel("A").point(lambda v:int(v*.48)))
        full.alpha_composite(t)
        cx, cy = row[side]["center"]
        r = 14
        draw.ellipse([cx-r,cy-r,cx+r,cy+r], fill=(*color,255), outline=(255,255,255,255), width=2)
        text = str(row["index"]); bb = draw.textbbox((0,0), text, font=font(15))
        draw.text((cx-(bb[2]-bb[0])/2, cy-(bb[3]-bb[1])/2-2), text, fill=(20,20,20,255), font=font(15))
    legend_w = 520
    canvas = Image.new("RGBA", (full.width+legend_w, full.height), (247,249,252,255))
    canvas.alpha_composite(full, (0,0)); d = ImageDraw.Draw(canvas)
    d.text((full.width+18,18), f"{side.upper()} — {len(visible)} visible masks", fill=(20,30,50,255), font=font(20))
    y=58
    for idx,row in enumerate(visible):
        import colorsys
        color=tuple(int(c*255) for c in colorsys.hsv_to_rgb((idx*.61803398875)%1,.72,.9))
        d.rectangle([full.width+20,y+2,full.width+34,y+16], fill=(*color,255))
        d.text((full.width+42,y), f'{row["index"]:02d} {row["id"]}', fill=(30,40,60,255), font=font(14)); y+=25
    hidden = [r for r in ROWS if r not in visible]
    if hidden:
        y+=8; d.text((full.width+18,y), "no mask on this side:", fill=(150,90,30,255), font=font(14)); y+=22
        for row in hidden:
            d.text((full.width+42,y), f'- {row["id"]}', fill=(120,80,30,255), font=font(14)); y+=22
    canvas.convert("RGB").save(PREVIEW/f"candidate_{side}_partition.png")


def compare_all(side, base, group_size=11):
    for group in range(0, len(ROWS), group_size):
        subset = ROWS[group:group+group_size]
        tw,th,lh=220,330,26
        sheet=Image.new("RGBA",(tw*2,len(subset)*(th+lh)),(247,249,252,255)); d=ImageDraw.Draw(sheet); f=font(12)
        for i,row in enumerate(subset):
            y=i*(th+lh)
            official=Image.open(ROOT/"app/src/main/assets/muscle_masks"/side/row[f"{side}File"]).convert("RGBA")
            candidate=Image.open(WORK/side/row[f"{side}File"]).convert("RGBA")
            before=fit(tint(base,official),(tw,th)); after=fit(tint(base,candidate),(tw,th))
            sheet.alpha_composite(before,(0,y)); sheet.alpha_composite(after,(tw,y))
            d.text((4,y+th+5),f'{row["index"]:02d} {row["id"]} — before',fill=(20,30,50,255),font=f)
            d.text((tw+4,y+th+5),f'{row["index"]:02d} {row["id"]} — candidate',fill=(20,30,50,255),font=f)
        sheet.convert("RGB").save(PREVIEW/f"compare_all_{side}_{group//group_size+1}.png")


def main():
    bf=Image.open(BASE/"muscle_front_base.png").convert("RGBA")
    bb=Image.open(BASE/"muscle_back_base.png").convert("RGBA")
    individual("front",bf); individual("back",bb)
    partition("front",bf); partition("back",bb)
    compare_all("front",bf); compare_all("back",bb)
    print("review previews", PREVIEW)


if __name__ == "__main__":
    main()
