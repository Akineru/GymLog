from pathlib import Path

src = Path(r"C:\Users\75192\AndroidStudioProjects\GymLog\design\calendar-color-styles-compare.html")
s = src.read_text(encoding="utf-8")

s = s.replace(
    "日历部位配色 · 鲜明 / 柔和参考",
    "日历部位配色 · 新鲜明 / 柔和（当前）",
)
s = s.replace(
    "相同日期、相同训练部位，只比较颜色。柔和为低饱和候选，可继续调整。",
    "相同日期、相同训练部位。鲜明改为更亮、更饱和的候选；柔和直接使用现在的颜色。",
)
s = s.replace(
    '<div class="label"><span class="dot"></span>鲜明</div>',
    '<div class="label"><span class="dot"></span>鲜明（新亮色）</div>',
)
s = s.replace(
    '<div class="label soft"><span class="dot"></span>柔和</div>',
    '<div class="label soft"><span class="dot"></span>柔和（当前）</div>',
)
s = s.replace(
    ".v1{background:#E57373}.v2{background:#64B5F6}.v3{background:#FFB74D}.v4{background:#81C784}.v5{background:#BA68C8}.v6{background:#90A4AE}",
    ".v1{background:#FF6B6B}.v2{background:#4FC3F7}.v3{background:#FFC107}.v4{background:#66BB6A}.v5{background:#AB47BC}.v6{background:#78909C}",
)
s = s.replace(
    ".s1{background:#E7B8B8}.s2{background:#B7D4EA}.s3{background:#EAD4B0}.s4{background:#BFD9C2}.s5{background:#D5C0DD}.s6{background:#C6D0D8}",
    ".s1{background:#E57373}.s2{background:#64B5F6}.s3{background:#FFB74D}.s4{background:#81C784}.s5{background:#BA68C8}.s6{background:#90A4AE}",
)
s = s.replace(
    "柔和候选色：胸 #E7B8B8 · 背 #B7D4EA · 肩 #EAD4B0 · 腿 #BFD9C2 · 手臂 #D5C0DD · 其他 #C6D0D8。深色模式下会保持同一组色相，只做适配。",
    "鲜明新色：胸 #FF6B6B · 背 #4FC3F7 · 肩 #FFC107 · 腿 #66BB6A · 手臂 #AB47BC · 其他 #78909C。柔和 = 当前配色，不做修改。",
)

out = Path(r"C:\Users\75192\AndroidStudioProjects\GymLog\design\calendar-colors-bright-vs-current.html")
out.write_text(s, encoding="utf-8")
print(out)
