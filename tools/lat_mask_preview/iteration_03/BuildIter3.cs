using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Globalization;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

public static class LatIter3c {
  static PointF Add(PointF a, PointF b) { return new PointF(a.X + b.X, a.Y + b.Y); }
  static PointF Sub(PointF a, PointF b) { return new PointF(a.X - b.X, a.Y - b.Y); }
  static PointF Mul(PointF a, float s) { return new PointF(a.X * s, a.Y * s); }

  public static void Run(string root, string outDir) {
    Directory.CreateDirectory(outDir);
    PointF[] left = {
      new PointF(370,420), new PointF(402,426), new PointF(436,430), new PointF(456,438),
      new PointF(472,490), new PointF(478,542), new PointF(466,592),
      new PointF(454,616), new PointF(446,624),
      new PointF(438,622), new PointF(418,574), new PointF(400,530), new PointF(386,478)
    };
    PointF[] right = {
      new PointF(654,420), new PointF(622,426), new PointF(588,430), new PointF(568,438),
      new PointF(552,490), new PointF(546,542), new PointF(558,592),
      new PointF(570,616), new PointF(578,624),
      new PointF(586,622), new PointF(606,574), new PointF(624,530), new PointF(638,478)
    };
    PointF[] v2L = {
      new PointF(370,422), new PointF(400,426), new PointF(438,430), new PointF(462,436),
      new PointF(480,490), new PointF(488,540), new PointF(470,600),
      new PointF(460,628), new PointF(452,644), new PointF(442,624), new PointF(432,582),
      new PointF(400,530), new PointF(364,472)
    };
    PointF[] v2R = {
      new PointF(654,422), new PointF(624,426), new PointF(586,430), new PointF(556,436),
      new PointF(544,490), new PointF(534,540), new PointF(552,600),
      new PointF(562,628), new PointF(570,644), new PointF(580,624), new PointF(590,582),
      new PointF(616,532), new PointF(660,472)
    };
    string back = Path.Combine(root, @"app\src\main\res\drawable-nodpi\muscle_back_base.png");
    string official = Path.Combine(root, @"app\src\main\assets\muscle_masks\back\latissimus_dorsi.png");
    string maskDir = Path.Combine(root, @"app\src\main\assets\muscle_masks\back");
    int w, h;
    bool[] body;
    byte[] bb; int bs;
    Load(back, out bb, out bs, out body, out w, out h);
    bool[] cand = Raster(left, right, w, h);
    bool[] v2 = Raster(v2L, v2R, w, h);
    bool[] off = LoadMask(official, w, h);
    SaveMask(cand, w, h, Path.Combine(outDir, "latissimus_dorsi_candidate.png"));
    SavePreview(bb, bs, body, cand, w, h, Path.Combine(outDir, "latissimus_dorsi_candidate_preview.png"));
    SaveEdges(bb, bs, cand, w, h, Path.Combine(outDir, "latissimus_dorsi_candidate_edges.png"));
    SavePair(bb, bs, body, off, cand, w, h, Path.Combine(outDir, "compare_official_candidate.png"));
    SavePair(bb, bs, body, v2, cand, w, h, Path.Combine(outDir, "compare_v2_candidate.png"));
    var sb = new StringBuilder();
    sb.AppendLine(Stats("official", off, body, w, h));
    sb.AppendLine(Stats("v2", v2, body, w, h));
    sb.AppendLine(Stats("candidate", cand, body, w, h));
    string[] neighbors = { "teres_major", "trapezius_lower", "trapezius_middle", "trapezius_upper", "erector_spinae", "gluteus_medius" };
    foreach (var name in neighbors) {
      bool[] n = LoadMask(Path.Combine(maskDir, name + ".png"), w, h);
      sb.AppendLine(Overlap(name, off, v2, cand, n));
    }
    File.WriteAllText(Path.Combine(outDir, "metrics.txt"), sb.ToString(), new UTF8Encoding(true));
  }

  static string Overlap(string name, bool[] off, bool[] v2, bool[] cand, bool[] n) {
    int nn = 0, o = 0, a = 0, c = 0;
    for (int i = 0; i < n.Length; i++) {
      if (!n[i]) continue;
      nn++;
      if (off[i]) o++;
      if (v2[i]) a++;
      if (cand[i]) c++;
    }
    return name + " neighbor=" + nn
      + " officialOverlap=" + Pct(o, nn) + " ofNeighbor " + Pct(o, Count(off)) + " ofOfficial"
      + " v2Overlap=" + Pct(a, nn) + " ofNeighbor"
      + " candidateOverlap=" + Pct(c, nn) + " ofNeighbor " + Pct(c, Count(cand)) + " ofCandidate";
  }

  static string Stats(string name, bool[] on, bool[] body, int w, int h) {
    int opaque = 0, outside = 0, left = 0, right = 0;
    int minX = w, minY = h, maxX = -1, maxY = -1;
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
      if (!on[y * w + x]) continue;
      opaque++;
      if (x < 512) left++; else right++;
      if (!body[y * w + x]) outside++;
      if (x < minX) minX = x; if (y < minY) minY = y;
      if (x > maxX) maxX = x; if (y > maxY) maxY = y;
    }
    return name + " opaque=" + opaque + " left=" + left + " right=" + right
      + " outside=" + outside + " (" + Pct(outside, opaque) + ")"
      + " bbox=" + minX + "," + minY + "-" + maxX + "," + maxY;
  }

  static int Count(bool[] on) { int n = 0; foreach (var v in on) if (v) n++; return n; }
  static string Pct(int a, int b) { return (100.0 * a / Math.Max(1, b)).ToString("0.0", CultureInfo.InvariantCulture) + "%"; }

  static bool[] Raster(PointF[] left, PointF[] right, int w, int h) {
    using (var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
      using (var g = Graphics.FromImage(bmp)) {
        g.SmoothingMode = SmoothingMode.AntiAlias;
        g.PixelOffsetMode = PixelOffsetMode.Half;
        g.FillPath(Brushes.White, PathOf(left));
        g.FillPath(Brushes.White, PathOf(right));
      }
      return Alpha(bmp, 128);
    }
  }

  static bool[] LoadMask(string path, int w, int h) {
    using (var bmp = new Bitmap(path)) return Alpha(bmp, 200);
  }

  static bool[] Alpha(Bitmap bmp, int threshold) {
    int w = bmp.Width, h = bmp.Height;
    var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
    byte[] b = new byte[data.Stride * h];
    Marshal.Copy(data.Scan0, b, 0, b.Length);
    bmp.UnlockBits(data);
    var on = new bool[w * h];
    int lw = Math.Min(w, bmp.Width), lh = Math.Min(h, bmp.Height);
    for (int y = 0; y < lh; y++) for (int x = 0; x < lw; x++)
      if (b[y * data.Stride + x * 4 + 3] >= threshold) on[y * w + x] = true;
    return on;
  }

  static void Load(string path, out byte[] b, out int stride, out bool[] body, out int w, out int h) {
    using (var bmp = new Bitmap(path)) {
      w = bmp.Width; h = bmp.Height;
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
      stride = data.Stride;
      b = new byte[stride * h];
      Marshal.Copy(data.Scan0, b, 0, b.Length);
      bmp.UnlockBits(data);
      body = new bool[w * h];
      for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
        if (b[y * stride + x * 4 + 3] >= 16) body[y * w + x] = true;
    }
  }

  static GraphicsPath PathOf(PointF[] pts) {
    var path = new GraphicsPath();
    int n = pts.Length;
    for (int i = 0; i < n; i++) {
      PointF p0 = pts[(i - 1 + n) % n], p1 = pts[i], p2 = pts[(i + 1) % n], p3 = pts[(i + 2) % n];
      PointF c1 = Add(p1, Mul(Sub(p2, p0), 1f / 6f));
      PointF c2 = Sub(p2, Mul(Sub(p3, p1), 1f / 6f));
      path.AddBezier(p1, c1, c2, p2);
    }
    path.CloseFigure();
    return path;
  }

  static void SaveMask(bool[] on, int w, int h, string path) {
    using (var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
      byte[] b = new byte[data.Stride * h];
      for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (on[y * w + x]) {
        int i = y * data.Stride + x * 4;
        b[i] = b[i + 1] = b[i + 2] = b[i + 3] = 255;
      }
      Marshal.Copy(b, 0, data.Scan0, b.Length);
      bmp.UnlockBits(data);
      bmp.Save(path, ImageFormat.Png);
    }
  }

  static void SavePreview(byte[] bb, int bs, bool[] body, bool[] on, int w, int h, string path) {
    using (var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
      byte[] o = new byte[data.Stride * h];
      Paint(bb, bs, body, on, w, h, o, data.Stride, true);
      Marshal.Copy(o, 0, data.Scan0, o.Length);
      bmp.UnlockBits(data);
      bmp.Save(path, ImageFormat.Png);
    }
  }

  static void SaveEdges(byte[] bb, int bs, bool[] on, int w, int h, string path) {
    using (var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
      byte[] o = new byte[data.Stride * h];
      for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
        int si = y * bs + x * 4;
        int di = y * data.Stride + x * 4;
        if (bb[si + 3] < 16 && !(on[y * w + x] && IsEdge(on, x, y, w, h))) continue;
        bool edge = on[y * w + x] && IsEdge(on, x, y, w, h);
        if (edge) { o[di] = 40; o[di + 1] = 220; o[di + 2] = 40; o[di + 3] = 255; }
        else if (bb[si + 3] >= 16) { o[di] = bb[si]; o[di + 1] = bb[si + 1]; o[di + 2] = bb[si + 2]; o[di + 3] = 255; }
      }
      Marshal.Copy(o, 0, data.Scan0, o.Length);
      bmp.UnlockBits(data);
      bmp.Save(path, ImageFormat.Png);
    }
  }

  static bool IsEdge(bool[] on, int x, int y, int w, int h) {
    if (x == 0 || y == 0 || x == w - 1 || y == h - 1) return true;
    int p = y * w + x;
    return !on[p - 1] || !on[p + 1] || !on[p - w] || !on[p + w];
  }

  static void SavePair(byte[] bb, int bs, bool[] body, bool[] a, bool[] c, int w, int h, string path) {
    using (var bmp = new Bitmap(w * 2, h, PixelFormat.Format32bppArgb)) {
      var data = bmp.LockBits(new Rectangle(0, 0, w * 2, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
      byte[] o = new byte[data.Stride * h];
      byte[] left = new byte[w * 4 * h];
      byte[] right = new byte[w * 4 * h];
      int fakeStride = w * 4;
      Paint(bb, bs, body, a, w, h, left, fakeStride, true);
      Paint(bb, bs, body, c, w, h, right, fakeStride, true);
      for (int y = 0; y < h; y++) {
        Buffer.BlockCopy(left, y * fakeStride, o, y * data.Stride, fakeStride);
        Buffer.BlockCopy(right, y * fakeStride, o, y * data.Stride + fakeStride, fakeStride);
      }
      Marshal.Copy(o, 0, data.Scan0, o.Length);
      bmp.UnlockBits(data);
      bmp.Save(path, ImageFormat.Png);
    }
  }

  static void Paint(byte[] bb, int bs, bool[] body, bool[] on, int w, int h, byte[] o, int stride, bool showOutside) {
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
      int si = y * bs + x * 4;
      int di = y * stride + x * 4;
      bool m = on[y * w + x];
      bool inside = body[y * w + x];
      if (!inside && !m) continue;
      if (m && !inside && showOutside) {
        o[di] = 40; o[di + 1] = 40; o[di + 2] = 230; o[di + 3] = 255;
        continue;
      }
      if (!inside) continue;
      byte r = bb[si + 2], g = bb[si + 1], b = bb[si];
      if (m) {
        o[di] = (byte)(b * 58 / 100 + 33);
        o[di + 1] = (byte)(g * 58 / 100 + 29);
        o[di + 2] = (byte)(r * 58 / 100 + 101);
      } else { o[di] = b; o[di + 1] = g; o[di + 2] = r; }
      o[di + 3] = 255;
    }
  }
}
