$ErrorActionPreference = "Stop"
$inv = [Globalization.CultureInfo]::InvariantCulture
$center = 400.0

function F([double]$n) { $n.ToString("0.##", $inv) }

function Mirror-D([string]$d) {
    $tokens = [regex]::Matches($d, "[A-Za-z]|-?\d*\.?\d+") | ForEach-Object { $_.Value }
    $sb = New-Object System.Text.StringBuilder
    $i = 0
    $cmd = ""
    while ($i -lt $tokens.Count) {
        if ($tokens[$i] -match "^[A-Za-z]$") {
            $cmd = $tokens[$i]
            [void]$sb.Append($cmd).Append(" ")
            $i++
            if ($cmd -eq "Z") { continue }
        }
        switch ($cmd) {
            { $_ -in "M", "L" } {
                $x = [double]$tokens[$i]; $y = [double]$tokens[$i + 1]; $i += 2
                [void]$sb.Append((F (800 - $x))).Append(" ").Append((F $y)).Append(" ")
            }
            "C" {
                for ($k = 0; $k -lt 3; $k++) {
                    $x = [double]$tokens[$i]; $y = [double]$tokens[$i + 1]; $i += 2
                    [void]$sb.Append((F (800 - $x))).Append(" ").Append((F $y)).Append(" ")
                }
            }
            default { throw "unsupported $cmd" }
        }
    }
    return $sb.ToString().Trim()
}

function Outline([double]$sx, [double]$sy, $segs) {
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append("M ").Append((F $sx)).Append(" ").Append((F $sy)).Append(" ")
    foreach ($s in $segs) {
        [void]$sb.Append("C ")
        [void]$sb.Append((F $s.x1)).Append(" ").Append((F $s.y1)).Append(" ")
        [void]$sb.Append((F $s.x2)).Append(" ").Append((F $s.y2)).Append(" ")
        [void]$sb.Append((F $s.x)).Append(" ").Append((F $s.y)).Append(" ")
    }
    $px = $sx; $py = $sy
    for ($i = $segs.Count - 1; $i -ge 0; $i--) {
        $s = $segs[$i]
        $endx = if ($i -eq 0) { $sx } else { $segs[$i - 1].x }
        $endy = if ($i -eq 0) { $sy } else { $segs[$i - 1].y }
        [void]$sb.Append("C ")
        [void]$sb.Append((F (800 - $s.x2))).Append(" ").Append((F $s.y2)).Append(" ")
        [void]$sb.Append((F (800 - $s.x1))).Append(" ").Append((F $s.y1)).Append(" ")
        [void]$sb.Append((F (800 - $endx))).Append(" ").Append((F $endy)).Append(" ")
        $px = $endx; $py = $endy
    }
    [void]$sb.Append("Z")
    return $sb.ToString()
}

function Seg($x1, $y1, $x2, $y2, $x, $y) {
    return @{ x1 = [double]$x1; y1 = [double]$y1; x2 = [double]$x2; y2 = [double]$y2; x = [double]$x; y = [double]$y }
}

$arm = @(
    (Seg 330 308 255 328 190 368),
    (Seg 145 400 108 450 98 515),
    (Seg 88 590 78 680 86 770),
    (Seg 94 860 100 950 112 1035),
    (Seg 104 1085 86 1125 96 1168),
    (Seg 108 1198 124 1184 138 1158),
    (Seg 132 1205 140 1258 158 1268),
    (Seg 174 1278 186 1248 182 1212),
    (Seg 186 1248 202 1272 214 1258),
    (Seg 224 1264 232 1230 226 1196),
    (Seg 222 1155 210 1100 204 1055),
    (Seg 198 980 192 900 200 820),
    (Seg 208 740 214 650 228 575),
    (Seg 240 510 228 445 252 395)
)
$frontTorso = @(
    (Seg 258 445 268 520 272 610),
    (Seg 278 700 292 785 286 870),
    (Seg 274 950 248 1015 228 1080)
)
$backTorso = @(
    (Seg 210 470 188 560 196 660),
    (Seg 208 760 230 850 248 940),
    (Seg 236 1010 220 1055 214 1100)
)
$leg = @(
    (Seg 206 1175 188 1285 198 1395),
    (Seg 206 1475 214 1545 226 1605),
    (Seg 214 1655 202 1695 218 1738),
    (Seg 226 1772 196 1792 208 1824),
    (Seg 228 1852 268 1876 312 1864),
    (Seg 332 1884 352 1892 368 1868),
    (Seg 382 1896 398 1868 378 1836),
    (Seg 372 1808 358 1782 346 1758),
    (Seg 342 1655 350 1615 358 1570),
    (Seg 366 1495 374 1395 384 1285),
    (Seg 392 1185 398 1135 400 1105)
)
$frontBody = Outline 362 318 ($arm + $frontTorso + $leg)
$backBody = Outline 362 318 ($arm + $backTorso + $leg)

$head = "M 400 28 C 474 28 500 84 492 140 C 484 194 448 232 400 240 C 352 232 316 194 308 140 C 300 84 326 28 400 28 Z"
$earL = "M 306 124 C 286 136 282 170 302 186 C 316 176 320 146 306 124 Z"
$neck = "M 360 214 C 348 246 346 282 362 328 L 438 328 C 454 282 452 246 440 214 Z"

function Muscle([string]$id, [string]$d) { return @{ id = $id; d = $d } }

$frontMuscles = @(
    (Muscle "adductors_l" "M 352 1125 C 338 1220 332 1340 346 1465 C 356 1540 376 1585 398 1568 C 404 1525 400 1380 392 1260 C 384 1175 370 1128 352 1125 Z"),
    (Muscle "vastus_lateralis_l" "M 236 1110 C 198 1185 178 1320 194 1475 C 206 1575 240 1655 292 1672 C 332 1684 368 1620 356 1495 C 344 1355 328 1225 292 1148 C 270 1100 250 1096 236 1110 Z"),
    (Muscle "rectus_femoris_l" "M 300 1135 C 268 1210 252 1360 274 1520 C 288 1625 328 1700 372 1684 C 406 1670 422 1565 404 1420 C 388 1285 372 1190 338 1138 C 318 1118 300 1135 300 1135 Z"),
    (Muscle "vastus_medialis_l" "M 328 1495 C 304 1555 296 1620 322 1680 C 344 1726 392 1742 426 1708 C 448 1684 442 1615 410 1568 C 384 1530 354 1502 328 1495 Z"),
    (Muscle "tibialis_anterior_l" "M 248 1645 C 224 1700 216 1780 234 1848 C 246 1888 280 1902 310 1882 C 334 1866 340 1790 322 1715 C 306 1655 276 1632 248 1645 Z"),
    (Muscle "external_oblique_l" "M 250 600 C 228 670 220 780 238 890 C 254 980 300 1055 358 1090 C 388 1104 408 1052 394 960 C 380 840 372 720 350 640 C 322 592 278 582 250 600 Z"),
    (Muscle "rectus_abdominis" "M 338 625 C 326 710 320 820 336 940 C 346 1000 372 1024 400 1028 C 428 1024 454 1000 464 940 C 480 820 474 710 462 625 C 446 596 354 596 338 625 Z"),
    (Muscle "forearm_flexors_l" "M 160 810 C 148 870 142 960 156 1065 C 164 1120 192 1155 222 1136 C 244 1120 250 1035 238 940 C 228 860 196 808 160 810 Z"),
    (Muscle "brachioradialis_l" "M 118 790 C 98 840 92 930 104 1035 C 112 1085 136 1115 162 1098 C 184 1082 190 1000 180 910 C 172 840 146 788 118 790 Z"),
    (Muscle "biceps_brachii_l" "M 155 530 C 125 560 112 640 122 730 C 130 800 158 845 196 828 C 224 814 236 740 222 645 C 210 575 188 525 155 530 Z"),
    (Muscle "pectoralis_major_l" "M 300 400 C 246 382 198 425 186 495 C 176 555 204 630 260 672 C 308 708 362 700 390 658 C 404 634 404 555 388 485 C 374 425 346 398 300 400 Z"),
    (Muscle "deltoid_anterior_l" "M 185 385 C 160 405 155 460 178 515 C 196 555 240 572 275 545 C 298 526 304 475 284 430 C 266 392 220 372 185 385 Z"),
    (Muscle "deltoid_lateral_l" "M 150 375 C 112 400 96 455 108 520 C 118 565 150 590 180 568 C 202 552 208 500 192 445 C 178 400 172 372 150 375 Z")
)

$frontLines = @(
    "M 368 348 C 320 330 240 348 190 388",
    "M 400 640 L 400 1010",
    "M 350 735 C 378 744 422 744 450 735",
    "M 348 835 C 376 846 424 846 452 835",
    "M 350 930 C 378 940 422 940 450 930",
    "M 246 680 C 292 700 330 692 352 672",
    "M 240 755 C 292 778 336 766 360 742",
    "M 244 830 C 300 852 344 836 368 808",
    "M 250 1240 C 278 1380 300 1520 322 1655",
    "M 318 1400 C 340 1520 360 1620 388 1710",
    "M 118 800 C 150 818 180 812 200 794",
    "M 70 1165 L 78 1235",
    "M 108 1188 L 118 1260",
    "M 142 1185 L 146 1248",
    "M 250 1738 C 280 1775 320 1788 352 1768",
    "M 310 1745 C 335 1782 368 1784 388 1762",
    "M 300 1618 C 332 1598 372 1604 400 1630 C 372 1648 332 1644 300 1618 Z"
)

$backMuscles = @(
    (Muscle "latissimus_dorsi_l" "M 388 520 C 310 555 210 640 188 770 C 168 890 196 1035 252 1135 C 296 1210 360 1185 392 1085 C 414 990 412 680 388 520 Z"),
    (Muscle "erector_spinae" "M 368 680 C 350 810 348 950 372 1105 C 388 1145 412 1145 428 1105 C 452 950 450 810 432 680 C 414 650 386 650 368 680 Z"),
    (Muscle "trapezius_lower" "M 356 520 C 372 640 384 780 400 930 C 416 780 428 640 444 520 C 420 542 380 542 356 520 Z"),
    (Muscle "trapezius_middle" "M 210 420 C 285 388 345 445 400 462 C 455 445 515 388 590 420 C 530 510 455 555 400 564 C 345 555 270 510 210 420 Z"),
    (Muscle "trapezius_upper" "M 400 292 C 340 314 235 358 175 430 C 240 468 325 478 400 466 C 475 478 560 468 625 430 C 565 358 460 314 400 292 Z"),
    (Muscle "teres_major_l" "M 210 560 C 178 595 186 655 238 690 C 288 720 338 688 326 632 C 312 580 252 542 210 560 Z"),
    (Muscle "deltoid_posterior_l" "M 148 378 C 112 412 98 480 114 548 C 126 598 164 628 198 604 C 224 584 230 522 210 462 C 192 410 174 374 148 378 Z"),
    (Muscle "triceps_brachii_l" "M 150 535 C 116 578 104 675 120 780 C 132 850 168 898 208 876 C 238 858 250 768 234 660 C 220 582 186 528 150 535 Z"),
    (Muscle "forearm_extensors_l" "M 118 815 C 96 880 90 980 106 1095 C 116 1160 152 1198 190 1176 C 218 1158 226 1065 212 960 C 200 875 156 808 118 815 Z"),
    (Muscle "gluteus_medius_l" "M 230 1085 C 192 1135 180 1225 208 1305 C 232 1370 300 1400 352 1360 C 388 1332 394 1245 360 1170 C 328 1108 272 1066 230 1085 Z"),
    (Muscle "gluteus_maximus_l" "M 236 1225 C 190 1295 176 1405 208 1505 C 232 1580 304 1630 366 1595 C 408 1570 422 1475 396 1375 C 372 1290 310 1208 236 1225 Z"),
    (Muscle "hamstrings_l" "M 224 1460 C 196 1530 186 1625 210 1705 C 226 1760 280 1790 330 1762 C 366 1740 376 1655 354 1565 C 334 1490 280 1442 224 1460 Z"),
    (Muscle "gastrocnemius_l" "M 220 1665 C 192 1720 184 1785 208 1835 C 226 1870 278 1884 322 1858 C 352 1838 356 1775 332 1720 C 312 1672 268 1650 220 1665 Z"),
    (Muscle "soleus_l" "M 196 1748 C 176 1790 178 1835 206 1862 C 226 1880 250 1864 250 1830 C 250 1794 230 1756 196 1748 Z")
)

$backLines = @(
    "M 400 330 L 400 1120",
    "M 230 455 C 300 500 350 522 400 530",
    "M 358 600 C 378 740 390 880 400 1000",
    "M 210 760 C 270 830 330 875 375 860",
    "M 196 940 C 260 1020 330 1065 382 1028",
    "M 120 820 C 158 838 196 830 218 808",
    "M 175 1080 C 225 1130 280 1110 318 1068",
    "M 268 1320 C 320 1440 358 1540 388 1620",
    "M 280 1620 C 315 1700 345 1765 368 1825",
    "M 250 1785 C 290 1850 330 1862 362 1832",
    "M 62 1188 L 74 1255",
    "M 108 1210 L 118 1270",
    "M 140 1208 L 146 1252",
    "M 248 1765 C 280 1805 325 1815 358 1790",
    "M 310 1775 C 335 1812 365 1812 384 1788"
)

function Xml-Path([string]$id, [string]$d, [string]$fill, [string]$stroke, [double]$sw, [string]$extra) {
    $fillAttr = if ($fill) { " fill=`"$fill`"" } else { " fill=`"none`"" }
    $strokeAttr = if ($stroke) { " stroke=`"$stroke`" stroke-width=`"$sw`" stroke-linecap=`"round`" stroke-linejoin=`"round`"" } else { "" }
    return "    <path id=`"$id`"$fillAttr$strokeAttr$extra d=`"$d`"/>"
}

function Emit-Sheet([string]$file, [string]$title, [string]$bodyPath, $muscles, $lines) {
    $skin = New-Object System.Collections.Generic.List[string]
    $muscleXml = New-Object System.Collections.Generic.List[string]
    $skin.Add((Xml-Path "skin-body" $bodyPath "#F4F5F8" $null 0 ""))
    foreach ($m in $muscles) {
        $muscleXml.Add((Xml-Path $m.id $m.d "#D7DBE3" "#3A4250" 2.6 ""))
        if ($m.id.EndsWith("_l")) {
            $right = $m.id.Substring(0, $m.id.Length - 2) + "_r"
            $muscleXml.Add((Xml-Path $right (Mirror-D $m.d) "#D7DBE3" "#3A4250" 2.6 ""))
        }
    }
    $n = 0
    foreach ($line in $lines) {
        $n++
        $muscleXml.Add((Xml-Path "line-$n" $line $null "#5C6573" 2.4 ""))
        $muscleXml.Add((Xml-Path "line-$n-m" (Mirror-D $line) $null "#5C6573" 2.4 ""))
    }
    $cap = New-Object System.Collections.Generic.List[string]
    $cap.Add((Xml-Path "skin-neck" $neck "#F7F8FA" "#3E4654" 2.4 ""))
    $cap.Add((Xml-Path "skin-head" $head "#F7F8FA" "#3E4654" 3.2 ""))
    $cap.Add((Xml-Path "skin-ear-l" $earL "#E6E8EE" "#3E4654" 2.2 ""))
    $cap.Add((Xml-Path "skin-ear-r" (Mirror-D $earL) "#E6E8EE" "#3E4654" 2.2 ""))
    $cap.Add((Xml-Path "skin-outline" $bodyPath "none" "#2C333E" 3.6 ""))
    $bodyXml = ($skin -join "`n") + "`n  <g clip-path=`"url(#figure)`">`n" + ($muscleXml -join "`n") + "`n  </g>`n" + ($cap -join "`n")
    $svg = @"
<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1920" role="img" aria-label="$title">
  <desc>$title. Fills are the idle preview. The app recolors each muscle path by its id.</desc>
  <defs>
    <clipPath id="figure">
      <path d="$bodyPath"/>
    </clipPath>
  </defs>
$bodyXml
</svg>
"@
    [IO.File]::WriteAllText($file, $svg.Trim() + "`n", [Text.UTF8Encoding]::new($false))
}

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $PSScriptRoot) { $root = "C:\Users\75192\AndroidStudioProjects\GymLog" }
$outDir = "C:\Users\75192\AndroidStudioProjects\GymLog\app\src\main\assets\anatomy"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
Emit-Sheet (Join-Path $outDir "front.svg") "Front anatomical figure" $frontBody $frontMuscles $frontLines
Emit-Sheet (Join-Path $outDir "back.svg") "Back anatomical figure" $backBody $backMuscles $backLines
Write-Output "wrote $outDir"
