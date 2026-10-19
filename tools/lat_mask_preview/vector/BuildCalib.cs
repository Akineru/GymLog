using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Globalization;
using System.Runtime.InteropServices;
using System.Text;

public static class LatSvg3 {
  struct P { public int X, Y; public string Edge; public string Note; public P(int x, int y, string e, string n) { X = x; Y = y; Edge = e; Note = n; } }
  static PointF A(PointF a, PointF b) { return new PointF(a.X + b.X, a.Y + b.Y); }
  static PointF S(PointF a, PointF b) { return new PointF(a.X - b.X, a.Y - b.Y); }
  static PointF M(PointF a, float s) { return new PointF(a.X * s, a.Y * s); }

  public static string Run(string src, string dir) {
    P[] L = {
      new P(370,422,"superior","上缘与外缘的交角。白线在腋窝处变圆，交点可再移。"),
      new P(400,426,"superior","上缘。肩胛下方可看见的白线。"),
      new P(438,430,"superior","上缘。白线仍清楚。"),
      new P(462,436,"superior","上缘内侧终点。白线在这里变淡，并转入内缘。"),
      new P(480,490,"medial","内缘上段。竖脊肌外侧的浅沟。"),
      new P(488,540,"medial","内缘中段。仍贴着竖脊肌外侧。"),
      new P(470,600,"medial","内缘下段。腰筋膜开始向外斜。"),
      new P(448,646,"inferior","内缘与下缘的交角。髂嵴白带上方，纤维终点不锐利。"),
      new P(428,652,"inferior","下缘中点。沿髂嵴白带上沿，精确终点无法确认。"),
      new P(418,632,"inferior","下缘与外缘的交角。腰部转折，白带与体侧轮廓叠在一起。"),
      new P(426,590,"lateral","外缘下段。这里没有单独的肌内膜，边缘与躯干外轮廓重合。"),
      new P(400,530,"lateral","外缘。腋窝凹陷内侧，贴躯干外轮廓。"),
      new P(364,472,"lateral","外缘上段。贴躯干外轮廓，没有更靠内的可见肌界。")
    };
    P[] R = {
      new P(654,422,"superior","上缘与外缘的交角。白线在腋窝处变圆，交点可再移。"),
      new P(624,426,"superior","上缘。肩胛下方可看见的白线。"),
      new P(586,430,"superior","上缘。白线仍清楚。"),
      new P(556,436,"superior","上缘内侧终点。白线在这里变淡，并转入内缘。"),
      new P(544,490,"medial","内缘上段。竖脊肌外侧的浅沟。"),
      new P(534,540,"medial","内缘中段。仍贴着竖脊肌外侧。"),
      new P(552,600,"medial","内缘下段。腰筋膜开始向外斜。"),
      new P(574,646,"inferior","内缘与下缘的交角。髂嵴白带上方，纤维终点不锐利。"),
      new P(596,652,"inferior","下缘中点。沿髂嵴白带上沿，精确终点无法确认。"),
      new P(606,628,"inferior","下缘与外缘的交角。腰部转折，白带与体侧轮廓叠在一起。"),
      new P(588,590,"lateral","外缘下段。这里没有单独的肌内膜，边缘与躯干外轮廓重合。"),
      new P(616,532,"lateral","外缘。腋窝凹陷内侧，贴躯干外轮廓。"),
      new P(660,472,"lateral","外缘上段。贴躯干外轮廓，没有更靠内的可见肌界。")
    };
    System.IO.File.WriteAllText(dir + "\\latissimus_dorsi_calib.svg", BuildSvg(L, R), new UTF8Encoding(false));
    using (var body = new Bitmap(src)) {
      int w = body.Width, h = body.Height;
      var bd = body.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
      byte[] bb = new byte[bd.Stride * h];
      Marshal.Copy(bd.Scan0, bb, 0, bb.Length);
      int bs = bd.Stride;
      body.UnlockBits(bd);
      using (var mask = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
        using (var g = Graphics.FromImage(mask)) {
          g.SmoothingMode = SmoothingMode.AntiAlias;
          g.PixelOffsetMode = PixelOffsetMode.Half;
          g.FillPath(Brushes.White, PathOf(L));
          g.FillPath(Brushes.White, PathOf(R));
        }
        var md = mask.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        byte[] mb = new byte[md.Stride * h];
        Marshal.Copy(md.Scan0, mb, 0, mb.Length);
        int opaque = 0, outside = 0, left = 0, right = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
          int mi = y * md.Stride + x * 4;
          if (mb[mi + 3] < 128) { mb[mi] = mb[mi + 1] = mb[mi + 2] = mb[mi + 3] = 0; continue; }
          if (bb[y * bs + x * 4 + 3] < 16) { mb[mi] = mb[mi + 1] = mb[mi + 2] = mb[mi + 3] = 0; outside++; continue; }
          mb[mi] = mb[mi + 1] = mb[mi + 2] = mb[mi + 3] = 255;
          opaque++; if (x < 512) left++; else right++;
        }
        Marshal.Copy(mb, 0, md.Scan0, mb.Length);
        mask.UnlockBits(md);
        mask.Save(dir + "\\latissimus_dorsi_calib_mask.png", ImageFormat.Png);
        using (var preview = new Bitmap(w, h, PixelFormat.Format32bppArgb))
        using (var contour = new Bitmap(w, h, PixelFormat.Format32bppArgb)) {
          PaintBase(preview, bb, bs, mb, md.Stride, w, h, true);
          PaintBase(contour, bb, bs, mb, md.Stride, w, h, false);
          using (var g = Graphics.FromImage(contour)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            DrawEdge(g, L);
            DrawEdge(g, R);
          }
          preview.Save(dir + "\\latissimus_dorsi_calib_preview.png", ImageFormat.Png);
          contour.Save(dir + "\\latissimus_dorsi_calib_contour.png", ImageFormat.Png);
        }
        using (var zoomSrc = new Bitmap(src))
        using (var big = new Bitmap(860, 600))
        using (var g = Graphics.FromImage(big)) {
          g.InterpolationMode = InterpolationMode.NearestNeighbor;
          g.PixelOffsetMode = PixelOffsetMode.Half;
          g.DrawImage(zoomSrc, new Rectangle(0, 0, 860, 600), new Rectangle(300, 390, 430, 300), GraphicsUnit.Pixel);
          g.SmoothingMode = SmoothingMode.AntiAlias;
          DrawZoom(g, L, 1, 300, 390);
          DrawZoom(g, R, 14, 300, 390);
          big.Save(dir + "\\latissimus_dorsi_calib_anchors.png", ImageFormat.Png);
        }
        return "opaque=" + opaque + " clippedOutside=" + outside + " left=" + left + " right=" + right;
      }
    }
  }

  static void PaintBase(Bitmap dst, byte[] bb, int bs, byte[] mb, int ms, int w, int h, bool tint) {
    var d = dst.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
    byte[] o = new byte[d.Stride * h];
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
      int si = y * bs + x * 4;
      if (bb[si + 3] < 16) continue;
      int di = y * d.Stride + x * 4;
      byte r = bb[si + 2], g = bb[si + 1], b = bb[si];
      bool m = mb[y * ms + x * 4 + 3] == 255;
      if (tint && m) { o[di] = (byte)(b * 58 / 100); o[di + 1] = (byte)(g * 58 / 100 + 28); o[di + 2] = (byte)(r * 58 / 100 + 101); }
      else { o[di] = b; o[di + 1] = g; o[di + 2] = r; }
      o[di + 3] = 255;
    }
    Marshal.Copy(o, 0, d.Scan0, o.Length);
    dst.UnlockBits(d);
  }

  static void DrawEdge(Graphics g, P[] pts) {
    for (int i = 0; i < pts.Length; i++) {
      string edge = SegEdge(pts, i);
      Color c = edge == "superior" ? Color.FromArgb(230, 180, 0) : edge == "medial" ? Color.FromArgb(40, 90, 230) : edge == "lateral" ? Color.FromArgb(30, 160, 70) : Color.FromArgb(210, 40, 90);
      using (var pen = new Pen(c, 3f)) {
        if (edge == "inferior") pen.DashStyle = DashStyle.Dash;
        var seg = new GraphicsPath();
        PointF[] c4 = Segment(pts, i);
        seg.AddBezier(c4[0], c4[1], c4[2], c4[3]);
        g.DrawPath(pen, seg);
      }
    }
  }

  static void DrawZoom(Graphics g, P[] pts, int n0, int x0, int y0) {
    float sc = 2f;
    using (var pen = new Pen(Color.FromArgb(200, 40, 40), 2f)) {
      var path = new GraphicsPath();
      for (int i = 0; i < pts.Length; i++) {
        PointF[] c4 = Segment(pts, i);
        path.AddBezier(Z(c4[0], x0, y0, sc), Z(c4[1], x0, y0, sc), Z(c4[2], x0, y0, sc), Z(c4[3], x0, y0, sc));
      }
      g.DrawPath(pen, path);
    }
    using (var font = new Font("Consolas", 13f, FontStyle.Bold)) {
      for (int i = 0; i < pts.Length; i++) {
        float x = (pts[i].X - x0) * sc, y = (pts[i].Y - y0) * sc;
        Color fill = pts[i].Edge == "inferior" ? Color.OrangeRed : pts[i].Edge == "lateral" ? Color.Yellow : Color.White;
        using (var b = new SolidBrush(fill)) g.FillEllipse(b, x - 8, y - 8, 16, 16);
        g.DrawEllipse(Pens.Black, x - 8, y - 8, 16, 16);
        string label = (n0 + i).ToString();
        float tx = pts[i].X < 450 ? x - 26 : x + 10;
        float ty = y - 18;
        using (var halo = new SolidBrush(Color.FromArgb(220, 255, 255, 255))) g.FillRectangle(halo, tx - 1, ty, 20, 16);
        g.DrawString(label, font, Brushes.Black, tx, ty);
      }
    }
  }

  static PointF Z(PointF p, int x0, int y0, float s) { return new PointF((p.X - x0) * s, (p.Y - y0) * s); }

  static GraphicsPath PathOf(P[] pts) {
    var path = new GraphicsPath();
    for (int i = 0; i < pts.Length; i++) {
      PointF[] c4 = Segment(pts, i);
      path.AddBezier(c4[0], c4[1], c4[2], c4[3]);
    }
    path.CloseFigure();
    return path;
  }

  static PointF[] Segment(P[] pts, int i) {
    int n = pts.Length;
    PointF p0 = Pt(pts[(i - 1 + n) % n]), p1 = Pt(pts[i]), p2 = Pt(pts[(i + 1) % n]), p3 = Pt(pts[(i + 2) % n]);
    return new PointF[] { p1, A(p1, M(S(p2, p0), 1f / 6f)), S(p2, M(S(p3, p1), 1f / 6f)), p2 };
  }

  static string SegEdge(P[] pts, int i) {
    int n = pts.Length;
    string dest = pts[(i + 1) % n].Edge;
    if (pts[i].Edge == "lateral" && dest == "superior") return "lateral";
    return dest;
  }
  static PointF Pt(P p) { return new PointF(p.X, p.Y); }
  static string F(float v) { return v.ToString("0.##", CultureInfo.InvariantCulture); }

  static string BuildSvg(P[] L, P[] R) {
    var sb = new StringBuilder();
    sb.AppendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    sb.AppendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1024\" height=\"1536\" viewBox=\"0 0 1024 1536\">");
    sb.AppendLine("  <!-- 背阔肌人工校准稿。坐标系与 muscle_back_base.png 相同：1024 x 1536。 -->");
    sb.AppendLine("  <!-- 曲线穿过全部编号锚点。改圆点坐标时，请同时改对应 path 的端点。 -->");
    sb.AppendLine("  <!-- 下缘 inferior 整段不确定：髂嵴白带清楚，肌纤维在白带上方何处结束无法确认。 -->");
    sb.AppendLine("  <!-- 外缘 lateral 上中段与躯干外轮廓重合，插画里没有更靠内的独立肌界。 -->");
    sb.AppendLine("  <!-- 锚点 4 和 17：上缘白线变淡，与内缘的交角不确定。 -->");
    sb.AppendLine("  <g id=\"lat-fill\" fill=\"#ffffff\">");
    sb.Append("    <path id=\"lat-left\" d=\"").Append(D(L)).AppendLine("\"/>");
    sb.Append("    <path id=\"lat-right\" d=\"").Append(D(R)).AppendLine("\"/>");
    sb.AppendLine("  </g>");
    sb.AppendLine("  <g id=\"borders\" fill=\"none\" stroke-width=\"3\" stroke-linejoin=\"round\">");
    Border(sb, "left-superior", L, "superior", "#E6B400", false);
    Border(sb, "left-medial", L, "medial", "#285AE6", false);
    Border(sb, "left-inferior", L, "inferior", "#D22858", true);
    Border(sb, "left-lateral", L, "lateral", "#1E9A46", false);
    Border(sb, "right-superior", R, "superior", "#E6B400", false);
    Border(sb, "right-medial", R, "medial", "#285AE6", false);
    Border(sb, "right-inferior", R, "inferior", "#D22858", true);
    Border(sb, "right-lateral", R, "lateral", "#1E9A46", false);
    sb.AppendLine("  </g>");
    sb.AppendLine("  <g id=\"anchors\" font-family=\"Consolas, monospace\" font-size=\"14\">");
    Anchors(sb, L, 1);
    Anchors(sb, R, 14);
    sb.AppendLine("  </g>");
    sb.AppendLine("</svg>");
    return sb.ToString();
  }

  static void Border(StringBuilder sb, string id, P[] pts, string edge, string color, bool dash) {
    sb.Append("    <path id=\"").Append(id).Append("\" data-edge=\"").Append(edge).Append("\" stroke=\"").Append(color).Append("\"");
    if (dash) sb.Append(" stroke-dasharray=\"8 6\" data-certainty=\"uncertain\"");
    else if (edge == "lateral") sb.Append(" data-certainty=\"silhouette\"");
    else sb.Append(" data-certainty=\"visible\"");
    sb.Append(" d=\"");
    bool first = true;
    for (int i = 0; i < pts.Length; i++) {
      if (SegEdge(pts, i) != edge) continue;
      PointF[] s = Segment(pts, i);
      if (first) { sb.Append("M ").Append(F(s[0].X)).Append(' ').Append(F(s[0].Y)); first = false; }
      sb.Append(" C ").Append(F(s[1].X)).Append(' ').Append(F(s[1].Y)).Append(' ').Append(F(s[2].X)).Append(' ').Append(F(s[2].Y)).Append(' ').Append(F(s[3].X)).Append(' ').Append(F(s[3].Y));
    }
    sb.AppendLine("\"/>");
  }

  static string D(P[] pts) {
    var sb = new StringBuilder();
    for (int i = 0; i < pts.Length; i++) {
      PointF[] s = Segment(pts, i);
      if (i == 0) sb.Append("M ").Append(F(s[0].X)).Append(' ').Append(F(s[0].Y));
      sb.Append(" C ").Append(F(s[1].X)).Append(' ').Append(F(s[1].Y)).Append(' ').Append(F(s[2].X)).Append(' ').Append(F(s[2].Y)).Append(' ').Append(F(s[3].X)).Append(' ').Append(F(s[3].Y));
    }
    sb.Append(" Z");
    return sb.ToString();
  }

  static void Anchors(StringBuilder sb, P[] pts, int n0) {
    for (int i = 0; i < pts.Length; i++) {
      int n = n0 + i;
      sb.Append("    <!-- ").Append(n).Append(" ").Append(pts[i].Edge).Append(" ").Append(pts[i].Note).AppendLine(" -->");
      sb.Append("    <circle id=\"p").Append(n).Append("\" cx=\"").Append(pts[i].X).Append("\" cy=\"").Append(pts[i].Y);
      sb.Append("\" r=\"5\" data-edge=\"").Append(pts[i].Edge).Append("\" data-note=\"").Append(pts[i].Note).AppendLine("\" fill=\"#ffe14a\" stroke=\"#111\" stroke-width=\"1.5\"/>");
    }
  }
}
