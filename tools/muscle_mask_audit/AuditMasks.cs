using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Globalization;
using System.IO;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;

public static class MaskAudit1 {
  struct Muscle {
    public string Id, AppId, Zh, En, Region;
    public bool Front, Back, IntentionalEmpty;
    public Muscle(string id, string app, string zh, string en, string region, bool front, bool back, bool empty) {
      Id = id; AppId = app; Zh = zh; En = en; Region = region; Front = front; Back = back; IntentionalEmpty = empty;
    }
  }

  class Side {
    public string Id, View, Path;
    public bool Expected, Exists, Readable;
    public int W, H, Opaque, Soft, Outside, DustParts, SolidParts, Edge, BodyEdge;
    public int MinX, MinY, MaxX, MaxY;
    public string PixelFormat, Error;
    public bool[] On;
  }

  public static void Run(string root, string outDir) {
    Directory.CreateDirectory(outDir);
    Directory.CreateDirectory(Path.Combine(outDir, "previews"));
    var muscles = Muscles();
    string frontBase = Path.Combine(root, @"app\src\main\res\drawable-nodpi\muscle_front_base.png");
    string backBase = Path.Combine(root, @"app\src\main\res\drawable-nodpi\muscle_back_base.png");
    string maskRoot = Path.Combine(root, @"app\src\main\assets\muscle_masks");
    bool[] frontBody, backBody;
    int bw, bh;
    string frontInfo, backInfo;
    LoadBody(frontBase, out frontBody, out bw, out bh, out frontInfo);
    LoadBody(backBase, out backBody, out bw, out bh, out backInfo);
    var sides = new List<Side>();
    foreach (var m in muscles) {
      sides.Add(LoadSide(m, "front", m.Front, Path.Combine(maskRoot, "front", m.Id + ".png"), frontBody, bw, bh));
      sides.Add(LoadSide(m, "back", m.Back, Path.Combine(maskRoot, "back", m.Id + ".png"), backBody, bw, bh));
    }
    var csv = new StringBuilder();
    csv.AppendLine("id,appId,zh,view,expected,exists,readable,w,h,pixelFormat,opaque,softAlpha,outside,outsidePct,dustParts,solidParts,bbox,fill,edgeOnBodyPct,error");
    foreach (var s in sides) {
      var m = Find(muscles, s.Id);
      int area = Math.Max(1, (s.MaxX - s.MinX + 1) * (s.MaxY - s.MinY + 1));
      double fill = s.Opaque == 0 ? 0 : (double)s.Opaque / area;
      double outPct = s.Opaque == 0 ? 0 : 100.0 * s.Outside / s.Opaque;
      double edgePct = s.Edge == 0 ? 0 : 100.0 * s.BodyEdge / s.Edge;
      csv.Append(s.Id).Append(',').Append(m.AppId).Append(',').Append(m.Zh).Append(',').Append(s.View).Append(',');
      csv.Append(s.Expected).Append(',').Append(s.Exists).Append(',').Append(s.Readable).Append(',');
      csv.Append(s.W).Append(',').Append(s.H).Append(',').Append(s.PixelFormat).Append(',');
      csv.Append(s.Opaque).Append(',').Append(s.Soft).Append(',').Append(s.Outside).Append(',');
      csv.Append(outPct.ToString("0.00", CultureInfo.InvariantCulture)).Append(',');
      csv.Append(s.DustParts).Append(',').Append(s.SolidParts).Append(',');
      csv.Append(s.MinX).Append(' ').Append(s.MinY).Append(' ').Append(s.MaxX).Append(' ').Append(s.MaxY).Append(',');
      csv.Append(fill.ToString("0.00", CultureInfo.InvariantCulture)).Append(',');
      csv.Append(edgePct.ToString("0.0", CultureInfo.InvariantCulture)).Append(',');
      csv.Append(s.Error == null ? "" : s.Error.Replace(",", " "));
      csv.AppendLine();
    }
    var overlaps = new StringBuilder();
    overlaps.AppendLine("view,a,b,pixels,pctOfA,pctOfB,note");
    Overlap(sides, "front", overlaps);
    Overlap(sides, "back", overlaps);
    File.WriteAllText(Path.Combine(outDir, "metrics.csv"), csv.ToString(), new UTF8Encoding(true));
    File.WriteAllText(Path.Combine(outDir, "overlaps.csv"), overlaps.ToString(), new UTF8Encoding(true));
    File.WriteAllText(Path.Combine(outDir, "body_check.txt"), frontInfo + Environment.NewLine + backInfo, new UTF8Encoding(true));
    DrawOverview(frontBase, sides, "front", Path.Combine(outDir, "overview_front.png"), bw, bh);
    DrawOverview(backBase, sides, "back", Path.Combine(outDir, "overview_back.png"), bw, bh);
    DrawOverview(frontBase, sides, "front", Path.Combine(outDir, "overview_front_heads.png"), bw, bh, "quadriceps");
    foreach (var s in sides) {
      if (!s.Readable || s.On == null) continue;
      string basePath = s.View == "front" ? frontBase : backBase;
      bool[] body = s.View == "front" ? frontBody : backBody;
      DrawPreview(basePath, s, body, bw, bh, Path.Combine(outDir, "previews", s.Id + "_" + s.View + ".png"));
    }
  }

  static void LoadBody(string path, out bool[] body, out int w, out int h, out string info) {
    using (var bmp = new Bitmap(path)) {
      w = bmp.Width; h = bmp.Height;
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
      byte[] b = new byte[data.Stride * h];
      Marshal.Copy(data.Scan0, b, 0, b.Length);
      bmp.UnlockBits(data);
      body = new bool[w * h];
      int opaque = 0, partial = 0;
      for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
        int a = b[y * data.Stride + x * 4 + 3];
        if (a >= 16) { body[y * w + x] = true; opaque++; if (a < 255) partial++; }
      }
      info = Path.GetFileName(path) + " " + w + "x" + h + " pixelFormat=" + bmp.PixelFormat
        + " alpha>=16=" + opaque + " partialAlpha=" + partial
        + " note=alpha<16 is the transparent background of this illustration, including gaps between limbs.";
    }
  }

  static Side LoadSide(Muscle m, string view, bool expected, string path, bool[] body, int bw, int bh) {
    var s = new Side();
    s.Id = m.Id; s.View = view; s.Path = path; s.Expected = expected;
    s.MinX = bw; s.MinY = bh; s.MaxX = -1; s.MaxY = -1;
    if (!File.Exists(path)) { s.Exists = false; s.Error = "missing"; return s; }
    s.Exists = true;
    try {
      using (var bmp = new Bitmap(path)) {
        s.Readable = true;
        s.W = bmp.Width; s.H = bmp.Height;
        s.PixelFormat = bmp.PixelFormat.ToString();
        var data = bmp.LockBits(new Rectangle(0, 0, s.W, s.H), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        byte[] b = new byte[data.Stride * s.H];
        Marshal.Copy(data.Scan0, b, 0, b.Length);
        bmp.UnlockBits(data);
        int n = bw * bh;
        s.On = new bool[n];
        int limitW = Math.Min(s.W, bw), limitH = Math.Min(s.H, bh);
        for (int y = 0; y < limitH; y++) for (int x = 0; x < limitW; x++) {
          int a = b[y * data.Stride + x * 4 + 3];
          if (a >= 16 && a < 200) s.Soft++;
          if (a < 200) continue;
          s.Opaque++;
          s.On[y * bw + x] = true;
          if (x < s.MinX) s.MinX = x; if (y < s.MinY) s.MinY = y;
          if (x > s.MaxX) s.MaxX = x; if (y > s.MaxY) s.MaxY = y;
          if (!body[y * bw + x]) s.Outside++;
        }
        Components(s, bw, bh);
        Edges(s, body, bw, bh);
      }
    } catch (Exception ex) {
      s.Readable = false;
      s.Error = ex.GetType().Name;
    }
    return s;
  }

  static void Components(Side s, int w, int h) {
    if (s.On == null || s.Opaque == 0) return;
    var seen = new bool[w * h];
    var q = new int[w * h];
    for (int i = 0; i < w * h; i++) {
      if (!s.On[i] || seen[i]) continue;
      int qs = 0, qe = 0, area = 0;
      q[qe++] = i; seen[i] = true;
      while (qs < qe) {
        int p = q[qs++]; area++;
        int x = p % w, y = p / w;
        if (x > 0 && s.On[p - 1] && !seen[p - 1]) { seen[p - 1] = true; q[qe++] = p - 1; }
        if (x + 1 < w && s.On[p + 1] && !seen[p + 1]) { seen[p + 1] = true; q[qe++] = p + 1; }
        if (y > 0 && s.On[p - w] && !seen[p - w]) { seen[p - w] = true; q[qe++] = p - w; }
        if (y + 1 < h && s.On[p + w] && !seen[p + w]) { seen[p + w] = true; q[qe++] = p + w; }
      }
      if (area < 64) s.DustParts++; else s.SolidParts++;
    }
  }

  static void Edges(Side s, bool[] body, int w, int h) {
    if (s.On == null) return;
    for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
      int p = y * w + x;
      if (!s.On[p]) continue;
      if (s.On[p - 1] && s.On[p + 1] && s.On[p - w] && s.On[p + w]) continue;
      s.Edge++;
      bool near = false;
      for (int dy = -2; dy <= 2 && !near; dy++) for (int dx = -2; dx <= 2; dx++) {
        int xx = x + dx, yy = y + dy;
        if (xx < 0 || yy < 0 || xx >= w || yy >= h || !body[yy * w + xx]) near = true;
      }
      if (near) s.BodyEdge++;
    }
  }

  static void Overlap(List<Side> sides, string view, StringBuilder sb) {
    var list = new List<Side>();
    foreach (var s in sides) if (s.View == view && s.On != null && s.Opaque > 0) list.Add(s);
    for (int i = 0; i < list.Count; i++) for (int j = i + 1; j < list.Count; j++) {
      int both = 0;
      var a = list[i]; var b = list[j];
      for (int p = 0; p < a.On.Length; p++) if (a.On[p] && b.On[p]) both++;
      if (both == 0) continue;
      double pa = 100.0 * both / a.Opaque, pb = 100.0 * both / b.Opaque;
      if (pa < 0.5 && pb < 0.5) continue;
      string note = "";
      if (IsQuadPair(a.Id, b.Id)) note = "quad-design";
      sb.Append(view).Append(',').Append(a.Id).Append(',').Append(b.Id).Append(',').Append(both).Append(',');
      sb.Append(pa.ToString("0.0", CultureInfo.InvariantCulture)).Append(',');
      sb.Append(pb.ToString("0.0", CultureInfo.InvariantCulture)).Append(',').Append(note).AppendLine();
    }
  }

  static bool IsQuadPair(string a, string b) {
    string[] heads = { "rectus_femoris", "vastus_lateralis", "vastus_medialis" };
    bool ah = false, bh = false;
    foreach (var h in heads) { if (a == h) ah = true; if (b == h) bh = true; }
    return (a == "quadriceps" && bh) || (b == "quadriceps" && ah);
  }

  static void DrawOverview(string basePath, List<Side> sides, string view, string outPath, int w, int h, string skipId) {
    using (var bmp = new Bitmap(basePath))
    using (var g = Graphics.FromImage(bmp)) {
      int n = 0;
      foreach (var s in sides) if (s.View == view && s.On != null && s.Opaque > 0 && s.Id != skipId) n++;
      int k = 0;
      foreach (var s in sides) {
        if (s.View != view || s.On == null || s.Opaque == 0 || s.Id == skipId) continue;
        Color c = ColorFromIndex(k, n);
        k++;
        using (var layer = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
          var data = layer.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
          byte[] buf = new byte[data.Stride * h];
          for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (!s.On[y * w + x]) continue;
            int i = y * data.Stride + x * 4;
            buf[i] = c.B; buf[i + 1] = c.G; buf[i + 2] = c.R; buf[i + 3] = 150;
          }
          Marshal.Copy(buf, 0, data.Scan0, buf.Length);
          layer.UnlockBits(data);
          g.DrawImage(layer, 0, 0);
        }
      }
      using (var small = new Bitmap(512, 768))
      using (var sg = Graphics.FromImage(small)) {
        sg.InterpolationMode = System.Drawing.Drawing2D.InterpolationMode.HighQualityBicubic;
        sg.DrawImage(bmp, 0, 0, 512, 768);
        small.Save(outPath, ImageFormat.Png);
      }
    }
  }

  static void DrawOverview(string basePath, List<Side> sides, string view, string outPath, int w, int h) {
    DrawOverview(basePath, sides, view, outPath, w, h, null);
  }

  static void DrawPreview(string basePath, Side s, bool[] body, int w, int h, string outPath) {
    using (var bmp = new Bitmap(basePath)) {
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
      byte[] b = new byte[data.Stride * h];
      Marshal.Copy(data.Scan0, b, 0, b.Length);
      for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
        int i = y * data.Stride + x * 4;
        if (b[i + 3] < 16) continue;
        if (!s.On[y * w + x]) continue;
        bool outside = !body[y * w + x];
        byte r = b[i + 2], g = b[i + 1], bl = b[i];
        if (outside) { b[i] = 40; b[i + 1] = 40; b[i + 2] = 230; }
        else { b[i] = (byte)(bl * 58 / 100); b[i + 1] = (byte)(g * 58 / 100 + 28); b[i + 2] = (byte)(r * 58 / 100 + 101); }
        b[i + 3] = 255;
      }
      Marshal.Copy(b, 0, data.Scan0, b.Length);
      bmp.UnlockBits(data);
      int pad = 24;
      int x0 = Math.Max(0, s.MinX - pad), y0 = Math.Max(0, s.MinY - pad);
      int x1 = Math.Min(w - 1, s.MaxX + pad), y1 = Math.Min(h - 1, s.MaxY + pad);
      if (s.Opaque == 0) { x0 = 280; y0 = 200; x1 = 760; y1 = 1300; }
      using (var crop = bmp.Clone(new Rectangle(x0, y0, x1 - x0 + 1, y1 - y0 + 1), PixelFormat.Format32bppArgb))
        crop.Save(outPath, ImageFormat.Png);
    }
  }

  static Color ColorFromIndex(int i, int n) {
    double hue = (i * 360.0 / Math.Max(1, n));
    return ColorFromHsv(hue, 0.72, 0.95);
  }

  static Color ColorFromHsv(double h, double s, double v) {
    double c = v * s, x = c * (1 - Math.Abs((h / 60.0) % 2 - 1)), m = v - c;
    double r = 0, g = 0, b = 0;
    if (h < 60) { r = c; g = x; } else if (h < 120) { r = x; g = c; } else if (h < 180) { g = c; b = x; }
    else if (h < 240) { g = x; b = c; } else if (h < 300) { r = x; b = c; } else { r = c; b = x; }
    return Color.FromArgb(255, (int)((r + m) * 255), (int)((g + m) * 255), (int)((b + m) * 255));
  }

  static Muscle Find(Muscle[] muscles, string id) {
    foreach (var m in muscles) if (m.Id == id) return m;
    return muscles[0];
  }

  static string Hash(string path) {
    using (var sha = SHA256.Create())
    using (var stream = File.OpenRead(path)) {
      byte[] hash = sha.ComputeHash(stream);
      var sb = new StringBuilder();
      foreach (var b in hash) sb.Append(b.ToString("x2"));
      return sb.ToString();
    }
  }

  public static void WriteHashes(string root, string outPath) {
    string[] rel = {
      @"app\src\main\assets\muscle_manifest.json",
      @"app\src\main\res\drawable-nodpi\muscle_front_base.png",
      @"app\src\main\res\drawable-nodpi\muscle_back_base.png",
      @"app\src\main\assets\muscle_masks\front\latissimus_dorsi.png",
      @"app\src\main\assets\muscle_masks\back\latissimus_dorsi.png",
      @"app\src\main\assets\muscle_masks\front\quadriceps.png",
      @"app\src\main\java\com\example\gymlog\ui\muscle\MuscleMaskCatalog.kt"
    };
    var sb = new StringBuilder();
    foreach (var r in rel) {
      string p = Path.Combine(root, r);
      var info = new FileInfo(p);
      sb.Append(r).Append('\t').Append(info.Length).Append('\t').Append(info.LastWriteTimeUtc.ToString("o")).Append('\t').Append(Hash(p)).AppendLine();
    }
    File.WriteAllText(outPath, sb.ToString(), new UTF8Encoding(true));
  }

  static Muscle[] Muscles() {
    return new Muscle[] {
      new Muscle("pectoralis_major","pectoralis_major","胸大肌","Pectoralis major","胸部",true,false,false),
      new Muscle("deltoid_anterior","deltoid_anterior","三角肌前束","Anterior deltoid","肩部",true,false,false),
      new Muscle("deltoid_middle","deltoid_lateral","三角肌中束","Lateral deltoid","肩部",true,false,false),
      new Muscle("deltoid_posterior","deltoid_posterior","三角肌后束","Posterior deltoid","肩部",false,true,false),
      new Muscle("trapezius_upper","trapezius_upper","斜方肌上部","Upper trapezius","背部",false,true,false),
      new Muscle("trapezius_middle","trapezius_middle","斜方肌中部","Middle trapezius","背部",false,true,false),
      new Muscle("trapezius_lower","trapezius_lower","斜方肌下部","Lower trapezius","背部",false,true,false),
      new Muscle("latissimus_dorsi","latissimus_dorsi","背阔肌","Latissimus dorsi","背部",false,true,false),
      new Muscle("teres_major","teres_major","大圆肌","Teres major","背部",false,true,false),
      new Muscle("rhomboids","rhomboids","菱形肌","Rhomboids","背部",false,false,true),
      new Muscle("erector_spinae","erector_spinae","竖脊肌","Erector spinae","背部",false,true,false),
      new Muscle("biceps_brachii","biceps_brachii","肱二头肌","Biceps brachii","手臂",true,false,false),
      new Muscle("brachialis","brachialis","肱肌","Brachialis","手臂",false,false,true),
      new Muscle("triceps_brachii","triceps_brachii","肱三头肌","Triceps brachii","手臂",false,true,false),
      new Muscle("brachioradialis","brachioradialis","肱桡肌","Brachioradialis","手臂",true,false,false),
      new Muscle("forearm_flexors","forearm_flexors","前臂屈肌群","Forearm flexors","手臂",true,false,false),
      new Muscle("forearm_extensors","forearm_extensors","前臂伸肌群","Forearm extensors","手臂",true,true,false),
      new Muscle("rectus_abdominis","rectus_abdominis","腹直肌","Rectus abdominis","核心",true,false,false),
      new Muscle("external_oblique","external_oblique","腹外斜肌","External oblique","核心",true,false,false),
      new Muscle("internal_oblique","internal_oblique","腹内斜肌","Internal oblique","核心",false,false,true),
      new Muscle("transversus_abdominis","transversus_abdominis","腹横肌","Transversus abdominis","核心",false,false,true),
      new Muscle("gluteus_maximus","gluteus_maximus","臀大肌","Gluteus maximus","臀部",false,true,false),
      new Muscle("gluteus_medius","gluteus_medius","臀中肌","Gluteus medius","臀部",false,true,false),
      new Muscle("quadriceps","quadriceps","股四头肌","Quadriceps","腿部",true,false,false),
      new Muscle("rectus_femoris","rectus_femoris","股直肌","Rectus femoris","腿部",true,false,false),
      new Muscle("vastus_lateralis","vastus_lateralis","股外侧肌","Vastus lateralis","腿部",true,false,false),
      new Muscle("vastus_medialis","vastus_medialis","股内侧肌","Vastus medialis","腿部",true,false,false),
      new Muscle("hamstrings","hamstrings","腘绳肌","Hamstrings","腿部",false,true,false),
      new Muscle("adductors","adductors","内收肌群","Adductors","腿部",true,false,false),
      new Muscle("sartorius","sartorius","缝匠肌","Sartorius","腿部",true,false,false),
      new Muscle("gastrocnemius","gastrocnemius","腓肠肌","Gastrocnemius","小腿",false,true,false),
      new Muscle("soleus","soleus","比目鱼肌","Soleus","小腿",false,true,false),
      new Muscle("tibialis_anterior","tibialis_anterior","胫骨前肌","Tibialis anterior","小腿",true,false,false)
    };
  }
}
