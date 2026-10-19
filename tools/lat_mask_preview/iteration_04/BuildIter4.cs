using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Globalization;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

public static class LatIter4 {
  static PointF Add(PointF a, PointF b) { return new PointF(a.X + b.X, a.Y + b.Y); }
  static PointF Sub(PointF a, PointF b) { return new PointF(a.X - b.X, a.Y - b.Y); }
  static PointF Mul(PointF a, float s) { return new PointF(a.X * s, a.Y * s); }

  public static void Run(string root, string outDir) {
    PointF[] left = {
      new PointF(374,414), new PointF(404,426), new PointF(432,440), new PointF(452,450),
      new PointF(470,492), new PointF(476,544), new PointF(464,594),
      new PointF(456,622), new PointF(448,642), new PointF(440,650), new PointF(430,640),
      new PointF(418,574), new PointF(400,530), new PointF(386,478)
    };
    PointF[] right = {
      new PointF(650,414), new PointF(620,426), new PointF(592,440), new PointF(572,450),
      new PointF(554,492), new PointF(548,544), new PointF(560,594),
      new PointF(568,622), new PointF(576,642), new PointF(584,650), new PointF(594,640),
      new PointF(606,574), new PointF(624,530), new PointF(638,478)
    };
    string back = Path.Combine(root, @"app\src\main\res\drawable-nodpi\muscle_back_base.png");
    string oldPng = Path.Combine(root, @"tools\lat_mask_preview\iteration_03\latissimus_dorsi_candidate.png");
    string teres = Path.Combine(root, @"app\src\main\assets\muscle_masks\back\teres_major.png");
    string maskDir = Path.Combine(root, @"app\src\main\assets\muscle_masks\back");
    int w, h, bs;
    byte[] bb; bool[] body;
    Load(back, out bb, out bs, out body, out w, out h);
    bool[] neu = Raster(left, right, w, h);
    bool[] old = LoadMask(oldPng, 200);
    SaveMask(neu, w, h, Path.Combine(outDir, "latissimus_dorsi_candidate.png"));
    SavePaint(bb, bs, body, neu, w, h, Path.Combine(outDir, "latissimus_dorsi_candidate_preview.png"));
    SaveEdges(bb, bs, neu, w, h, Path.Combine(outDir, "latissimus_dorsi_candidate_edges.png"));
    SavePair(bb, bs, body, old, neu, w, h, Path.Combine(outDir, "compare_iter03_iter04.png"));
    using (var teresBmp = new Bitmap(teres)) {
      bool[] ter = LoadMask(teres, 200);
      SaveJunction(bb, bs, ter, old, w, h, Path.Combine(outDir, "zoom_junction_iter03.png"));
      SaveJunction(bb, bs, ter, neu, w, h, Path.Combine(outDir, "zoom_junction_iter04.png"));
    }
    SaveRegionPair(bb, bs, body, old, neu, w, h, 330, 360, 200, 170, Path.Combine(outDir, "zoom_junction.png"));
    SaveRegionPair(bb, bs, body, old, neu, w, h, 390, 560, 150, 160, Path.Combine(outDir, "zoom_tip.png"));
    SaveRegionPair(bb, bs, body, old, neu, w, h, 320, 430, 140, 160, Path.Combine(outDir, "zoom_lateral.png"));
    var sb = new StringBuilder();
    sb.AppendLine("size=" + w + "x" + h);
    sb.AppendLine(Stats("iter03", old, body, w, h));
    sb.AppendLine(Stats("iter04", neu, body, w, h));
    foreach (var name in new[] { "teres_major", "trapezius_lower", "trapezius_middle", "trapezius_upper", "erector_spinae", "gluteus_medius" }) {
      bool[] n = LoadMask(Path.Combine(maskDir, name + ".png"), 200);
      sb.AppendLine(Overlap(name, old, neu, n));
    }
    File.WriteAllText(Path.Combine(outDir, "metrics.txt"), sb.ToString(), new UTF8Encoding(true));
  }

  static void SaveRegionPair(byte[] bb, int bs, bool[] body, bool[] oldM, bool[] neu, int w, int h, int x, int y, int cw, int ch, string path) {
    using (var left = PaintBmp(bb, bs, body, oldM, w, h))
    using (var right = PaintBmp(bb, bs, body, neu, w, h))
    using (var big = new Bitmap(cw * 6 + 16, ch * 3))
    using (var g = Graphics.FromImage(big)) {
      g.Clear(Color.Black);
      g.InterpolationMode = InterpolationMode.NearestNeighbor;
      g.PixelOffsetMode = PixelOffsetMode.Half;
      g.DrawImage(left, new Rectangle(0, 0, cw * 3, ch * 3), new Rectangle(x, y, cw, ch), GraphicsUnit.Pixel);
      g.DrawImage(right, new Rectangle(cw * 3 + 16, 0, cw * 3, ch * 3), new Rectangle(x, y, cw, ch), GraphicsUnit.Pixel);
      using (var font = new Font("Consolas", 12f, FontStyle.Bold)) {
        g.DrawString("iter03", font, Brushes.White, 8, 6);
        g.DrawString("iter04", font, Brushes.White, cw * 3 + 24, 6);
      }
      big.Save(path, ImageFormat.Png);
    }
  }

  static void SaveJunction(byte[] bb, int bs, bool[] ter, bool[] lat, int w, int h, string path) {
    using (var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
      byte[] o = new byte[data.Stride * h];
      for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
        int si = y * bs + x * 4;
        int di = y * data.Stride + x * 4;
        if (bb[si + 3] < 16) continue;
        byte r = bb[si + 2], g = bb[si + 1], b = bb[si];
        bool t = ter[y * w + x], m = lat[y * w + x];
        if (t && m) { o[di] = 30; o[di + 1] = 40; o[di + 2] = 220; }
        else if (t) { o[di] = (byte)(b * 55 / 100); o[di + 1] = (byte)(g * 55 / 100 + 30); o[di + 2] = (byte)(r * 55 / 100 + 100); }
        else if (m) { o[di] = (byte)(b * 58 / 100 + 33); o[di + 1] = (byte)(g * 58 / 100 + 29); o[di + 2] = (byte)(r * 58 / 100 + 101); }
        else { o[di] = b; o[di + 1] = g; o[di + 2] = r; }
        o[di + 3] = 255;
      }
      Marshal.Copy(o, 0, data.Scan0, o.Length);
      bmp.UnlockBits(data);
      using (var big = new Bitmap(600, 480))
      using (var g = Graphics.FromImage(big)) {
        g.InterpolationMode = InterpolationMode.NearestNeighbor;
        g.DrawImage(bmp, new Rectangle(0, 0, 600, 480), new Rectangle(330, 360, 200, 160), GraphicsUnit.Pixel);
        big.Save(path, ImageFormat.Png);
      }
    }
  }

  static Bitmap PaintBmp(byte[] bb, int bs, bool[] body, bool[] on, int w, int h) {
    var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb);
    var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
    byte[] o = new byte[data.Stride * h];
    Paint(bb, bs, body, on, w, h, o, data.Stride);
    Marshal.Copy(o, 0, data.Scan0, o.Length);
    bmp.UnlockBits(data);
    return bmp;
  }

  static string Overlap(string name, bool[] oldM, bool[] neu, bool[] n) {
    int nn = 0, a = 0, c = 0;
    for (int i = 0; i < n.Length; i++) {
      if (!n[i]) continue;
      nn++;
      if (oldM[i]) a++;
      if (neu[i]) c++;
    }
    return name + " iter03=" + Pct(a, nn) + " ofNeighbor iter04=" + Pct(c, nn) + " ofNeighbor " + Pct(c, Count(neu)) + " ofCandidate";
  }

  static string Stats(string name, bool[] on, bool[] body, int w, int h) {
    int opaque = 0, outside = 0, left = 0, right = 0, minX = w, minY = h, maxX = -1, maxY = -1;
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
      if (!on[y * w + x]) continue;
      opaque++;
      if (x < 512) left++; else right++;
      if (!body[y * w + x]) outside++;
      if (x < minX) minX = x; if (y < minY) minY = y; if (x > maxX) maxX = x; if (y > maxY) maxY = y;
    }
    return name + " opaque=" + opaque + " left=" + left + " right=" + right + " outside=" + outside + " (" + Pct(outside, opaque) + ") bbox=" + minX + "," + minY + "-" + maxX + "," + maxY;
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

  static bool[] LoadMask(string path, int threshold) {
    using (var bmp = new Bitmap(path)) return Alpha(bmp, threshold);
  }

  static bool[] Alpha(Bitmap bmp, int threshold) {
    int w = bmp.Width, h = bmp.Height;
    var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
    byte[] b = new byte[data.Stride * h];
    Marshal.Copy(data.Scan0, b, 0, b.Length);
    bmp.UnlockBits(data);
    var on = new bool[w * h];
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
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
      path.AddBezier(p1, Add(p1, Mul(Sub(p2, p0), 1f / 6f)), Sub(p2, Mul(Sub(p3, p1), 1f / 6f)), p2);
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

  static void SavePaint(byte[] bb, int bs, bool[] body, bool[] on, int w, int h, string path) {
    using (var bmp = PaintBmp(bb, bs, body, on, w, h)) bmp.Save(path, ImageFormat.Png);
  }

  static void SaveEdges(byte[] bb, int bs, bool[] on, int w, int h, string path) {
    using (var bmp = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
      var data = bmp.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
      byte[] o = new byte[data.Stride * h];
      for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
        int si = y * bs + x * 4, di = y * data.Stride + x * 4;
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
      int fake = w * 4;
      Paint(bb, bs, body, a, w, h, left, fake);
      Paint(bb, bs, body, c, w, h, right, fake);
      for (int y = 0; y < h; y++) {
        Buffer.BlockCopy(left, y * fake, o, y * data.Stride, fake);
        Buffer.BlockCopy(right, y * fake, o, y * data.Stride + fake, fake);
      }
      Marshal.Copy(o, 0, data.Scan0, o.Length);
      bmp.UnlockBits(data);
      bmp.Save(path, ImageFormat.Png);
    }
  }

  static void Paint(byte[] bb, int bs, bool[] body, bool[] on, int w, int h, byte[] o, int stride) {
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
      int si = y * bs + x * 4, di = y * stride + x * 4;
      bool m = on[y * w + x], inside = body[y * w + x];
      if (!inside && !m) continue;
      if (m && !inside) { o[di] = 40; o[di + 1] = 40; o[di + 2] = 230; o[di + 3] = 255; continue; }
      if (!inside) continue;
      byte r = bb[si + 2], g = bb[si + 1], b = bb[si];
      if (m) { o[di] = (byte)(b * 58 / 100 + 33); o[di + 1] = (byte)(g * 58 / 100 + 29); o[di + 2] = (byte)(r * 58 / 100 + 101); }
      else { o[di] = b; o[di + 1] = g; o[di + 2] = r; }
      o[di + 3] = 255;
    }
  }
}
