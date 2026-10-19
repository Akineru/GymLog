from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
import sys

ROOT = Path(__file__).resolve().parents[2]
WORK = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r"C:\Users\75192\AndroidStudioProjects\GymLog-muscle-redraw-work-20261018-1355")
INV = json.loads((ROOT/"tools/muscle_redraw/inventory.json").read_text(encoding="utf-8"))
ROWS = {r["id"]: r for r in INV["muscles"]}
BASE = ROOT/"app/src/main/res/drawable-nodpi"
BEFORE = ROOT/"tools/muscle_redraw/previews"
OUT = WORK/"previews"


def font(size):
    for p in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\arial.ttf"):
        try: return ImageFont.truetype(p,size)
        except Exception: pass
    return ImageFont.load_default()


def tint(base, mask, alpha=0.55):
    out=base.copy(); t=Image.new("RGBA",mask.size,(220,65,75,0))
    t.putalpha(mask.getchannel("A").point(lambda v:int(v*alpha))); out.alpha_composite(t); return out


def panel(img, size):
    im=img.copy(); im.thumbnail(size,Image.LANCZOS)
    p=Image.new("RGBA",size,(247,249,252,255)); p.alpha_composite(im,((size[0]-im.width)//2,(size[1]-im.height)//2)); return p


def make(side, ids, filename, crop_box=None):
    base=Image.open(BASE/f"muscle_{side}_base.png").convert("RGBA")
    rows=[ROWS[i] for i in ids if i in ROWS]
    tw,th,lh=360,540,30
    sheet=Image.new("RGBA",(tw*2, len(rows)*(th+lh)),(247,249,252,255)); d=ImageDraw.Draw(sheet); f=font(14)
    for idx,row in enumerate(rows):
        y=idx*(th+lh)
        # crop around the candidate + prior bbox
        cand_path=WORK/side/row[f"{side}File"]
        cand=Image.open(cand_path).convert("RGBA")
        prior=Image.open(BEFORE/side/f'{row["id"]}.png').convert("RGBA")
        box = cand.getchannel("A").getbbox() or prior.getchannel("A").getbbox()
        if box:
            x0=max(0,box[0]-50); y0=max(0,box[1]-50); x1=min(base.width,box[2]+50); y1=min(base.height,box[3]+50)
        else:
            x0,y0,x1,y1=0,0,base.width,base.height
        b=base.crop((x0,y0,x1,y1))
        # prior full-size mask is only a preview; use official source for accurate before
        source=ROOT/"app/src/main/assets/muscle_masks"/side/row[f"{side}File"]
        bm=Image.open(source).convert("RGBA").crop((x0,y0,x1,y1))
        cand_crop=cand.crop((x0,y0,x1,y1))
        before=tint(b,bm); after=tint(b,cand_crop)
        sheet.alpha_composite(panel(before,(tw,th)),(0,y))
        sheet.alpha_composite(panel(after,(tw,th)),(tw,y))
        d.text((6,y+th+5),f'{row["id"]} — before',fill=(20,30,50,255),font=f)
        d.text((tw+6,y+th+5),f'{row["id"]} — candidate',fill=(20,30,50,255),font=f)
    sheet.convert("RGB").save(OUT/filename)
    print(filename, sheet.size)

make("front", ["pectoralis_major","deltoid_anterior","deltoid_lateral","external_oblique","rectus_abdominis","biceps_brachii"], "compare_front_torso.png")
make("front", ["quadriceps","rectus_femoris","vastus_lateralis","vastus_medialis","sartorius","adductors"], "compare_front_legs.png")
make("front", ["biceps_brachii","brachioradialis","forearm_flexors","forearm_extensors"], "compare_front_arms.png")
make("back", ["latissimus_dorsi","teres_major","trapezius_upper","trapezius_middle","trapezius_lower","erector_spinae"], "compare_back_upper.png")
make("back", ["latissimus_dorsi","gluteus_maximus","gluteus_medius","hamstrings"], "compare_back_lower.png")
make("back", ["triceps_brachii","forearm_extensors","gastrocnemius","soleus"], "compare_back_arms_legs.png")
