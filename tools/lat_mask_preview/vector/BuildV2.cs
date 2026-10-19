using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Globalization;
using System.Text;

public static class LatV2c {
  struct P { public int X, Y; public string Edge; public string Certainty; public string Note; public P(int x, int y, string e, string c, string n) { X = x; Y = y; Edge = e; Certainty = c; Note = n; } }
  static PointF Add(PointF a, PointF b) { return new PointF(a.X + b.X, a.Y + b.Y); }
  static PointF Sub(PointF a, PointF b) { return new PointF(a.X - b.X, a.Y - b.Y); }
  static PointF Mul(PointF a, float s) { return new PointF(a.X * s, a.Y * s); }
  static PointF Pt(P p) { return new PointF(p.X, p.Y); }
  static string F(float v) { return v.ToString("0.##", CultureInfo.InvariantCulture); }

  public static void Run(string src, string dir) {
    P[] v1L = Side(false, false);
    P[] v1R = Side(true, false);
    P[] v2L = Side(false, true);
    P[] v2R = Side(true, true);
    System.IO.File.WriteAllText(dir + "\\latissimus_dorsi_calib_v2.svg", Svg(v2L, v2R), new UTF8Encoding(false));
    using (var body = new Bitmap(src)) {
      using (var compare = new Bitmap(body)) {
        using (var g = Graphics.FromImage(compare)) {
          g.SmoothingMode = SmoothingMode.AntiAlias;
          Stroke(g, v1L, Color.FromArgb(220, 40, 90, 220), 4f, DashStyle.Dash);
          Stroke(g, v1R, Color.FromArgb(220, 40, 90, 220), 4f, DashStyle.Dash);
          Stroke(g, v2L, Color.FromArgb(230, 210, 40, 40), 3f, DashStyle.Solid);
          Stroke(g, v2R, Color.FromArgb(230, 210, 40, 40), 3f, DashStyle.Solid);
        }
        using (var crop = compare.Clone(new Rectangle(300, 390, 430, 300), PixelFormat.Format32bppArgb))
        using (var big = new Bitmap(860, 600))
        using (var g = Graphics.FromImage(big)) {
          g.InterpolationMode = InterpolationMode.NearestNeighbor;
          g.PixelOffsetMode = PixelOffsetMode.Half;
          g.DrawImage(crop, new Rectangle(0, 0, 860, 600));
          big.Save(dir + "\\latissimus_dorsi_v2_compare.png", ImageFormat.Png);
        }
      }
      using (var big = new Bitmap(860, 600))
      using (var g = Graphics.FromImage(big)) {
        g.InterpolationMode = InterpolationMode.NearestNeighbor;
        g.PixelOffsetMode = PixelOffsetMode.Half;
        g.DrawImage(body, new Rectangle(0, 0, 860, 600), new Rectangle(300, 390, 430, 300), GraphicsUnit.Pixel);
        g.SmoothingMode = SmoothingMode.AntiAlias;
        DrawZoom(g, v2L, 1);
        DrawZoom(g, v2R, 14);
        big.Save(dir + "\\latissimus_dorsi_v2_anchors.png", ImageFormat.Png);
      }
    }
  }

  static P[] Side(bool right, bool v2) {
    if (!right && !v2) return new P[] {
      new P(370,422,"superior","visible",""), new P(400,426,"superior","visible",""), new P(438,430,"superior","visible",""), new P(462,436,"superior","visible",""),
      new P(480,490,"medial","reference",""), new P(488,540,"medial","reference",""), new P(470,600,"medial","reference",""),
      new P(448,646,"inferior","uncertain",""), new P(428,652,"inferior","uncertain",""), new P(418,632,"inferior","uncertain",""),
      new P(426,590,"lateral","silhouette",""), new P(400,530,"lateral","silhouette",""), new P(364,472,"lateral","silhouette","")
    };
    if (right && !v2) return new P[] {
      new P(654,422,"superior","visible",""), new P(624,426,"superior","visible",""), new P(586,430,"superior","visible",""), new P(556,436,"superior","visible",""),
      new P(544,490,"medial","reference",""), new P(534,540,"medial","reference",""), new P(552,600,"medial","reference",""),
      new P(574,646,"inferior","uncertain",""), new P(596,652,"inferior","uncertain",""), new P(606,628,"inferior","uncertain",""),
      new P(588,590,"lateral","silhouette",""), new P(616,532,"lateral","silhouette",""), new P(660,472,"lateral","silhouette","")
    };
    if (!right) return new P[] {
      new P(370,422,"superior","visible","上缘。肩胛下白线，本版未移动。"),
      new P(400,426,"superior","visible","上缘。肩胛下白线，本版未移动。"),
      new P(438,430,"superior","visible","上缘。肩胛下白线，本版未移动。"),
      new P(462,436,"superior","visible","上缘内侧。白线变淡处，本版未移动。"),
      new P(480,490,"medial","reference","内缘参考，本版按要求保留。"),
      new P(488,540,"medial","reference","内缘参考，本版按要求保留。"),
      new P(470,600,"medial","reference","内缘参考，本版按要求保留。"),
      new P(460,628,"inferior","pending","待人工确认。内缘下延，停在髂嵴白带上方，不沿白带收成圆角。"),
      new P(452,644,"inferior","pending","待人工确认。斜向浅沟与白带上方纤维区相遇，不在躯干外轮廓上。纤维终点不锐利。"),
      new P(442,624,"lateral","pending","待人工确认。腰部斜向浅沟，在身体剪影内侧。不能确定它是肌界还是纤维沟。"),
      new P(432,582,"lateral","pending","待人工确认。同一条斜向浅沟的上端。"),
      new P(400,530,"lateral","pending","待人工确认。此段底图没有独立肌界，锚点仍靠近躯干外轮廓，未把它当成背阔肌边界。"),
      new P(364,472,"lateral","pending","待人工确认。腋窝旁纤维一直画到体侧，未见更靠内的肌界。")
    };
    return new P[] {
      new P(654,422,"superior","visible","上缘。肩胛下白线，本版未移动。"),
      new P(624,426,"superior","visible","上缘。肩胛下白线，本版未移动。"),
      new P(586,430,"superior","visible","上缘。肩胛下白线，本版未移动。"),
      new P(556,436,"superior","visible","上缘内侧。白线变淡处，本版未移动。"),
      new P(544,490,"medial","reference","内缘参考，本版按要求保留。"),
      new P(534,540,"medial","reference","内缘参考，本版按要求保留。"),
      new P(552,600,"medial","reference","内缘参考，本版按要求保留。"),
      new P(562,628,"inferior","pending","待人工确认。内缘下延，停在髂嵴白带上方，不沿白带收成圆角。"),
      new P(570,644,"inferior","pending","待人工确认。斜向浅沟与白带上方纤维区相遇，不在躯干外轮廓上。纤维终点不锐利。"),
      new P(580,624,"lateral","pending","待人工确认。腰部斜向浅沟，在身体剪影内侧。不能确定它是肌界还是纤维沟。"),
      new P(590,582,"lateral","pending","待人工确认。同一条斜向浅沟的上端。"),
      new P(616,532,"lateral","pending","待人工确认。此段底图没有独立肌界，锚点仍靠近躯干外轮廓，未把它当成背阔肌边界。"),
      new P(660,472,"lateral","pending","待人工确认。腋窝旁纤维一直画到体侧，未见更靠内的肌界。")
    };
  }

  static string Svg(P[] L, P[] R) {
    var sb = new StringBuilder();
    sb.AppendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    sb.AppendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1024\" height=\"1536\" viewBox=\"0 0 1024 1536\">");
    sb.AppendLine("  <!-- 背阔肌校准第二版。第一版保留为 latissimus_dorsi_calib.svg 与 latissimus_dorsi_calib_v1.svg。 -->");
    sb.AppendLine("  <!-- 上缘 1-4、14-17 未移动。内缘 5-7、18-20 保留为参考。 -->");
    sb.AppendLine("  <!-- 下缘 8-9、21-22 与外缘下段 10-11、23-24 改到腰部可见斜向浅沟内侧，离开躯干剪影。这些点均为待人工确认。 -->");
    sb.AppendLine("  <!-- 外缘上段 12-13、25-26 未移动：底图没有独立肌界，不能把身体剪影写成背阔肌边界。 -->");
    sb.AppendLine("  <g id=\"lat-fill\" fill=\"#ffffff\">");
    sb.Append("    <path id=\"lat-left\" d=\"").Append(D(L)).AppendLine("\"/>");
    sb.Append("    <path id=\"lat-right\" d=\"").Append(D(R)).AppendLine("\"/>");
    sb.AppendLine("  </g>");
    sb.AppendLine("  <g id=\"borders\" fill=\"none\" stroke-width=\"3\" stroke-linejoin=\"round\">");
    Border(sb, "left-superior", L, "superior", "#E6B400");
    Border(sb, "left-medial", L, "medial", "#285AE6");
    Border(sb, "left-inferior", L, "inferior", "#D22858");
    Border(sb, "left-lateral", L, "lateral", "#1E9A46");
    Border(sb, "right-superior", R, "superior", "#E6B400");
    Border(sb, "right-medial", R, "medial", "#285AE6");
    Border(sb, "right-inferior", R, "inferior", "#D22858");
    Border(sb, "right-lateral", R, "lateral", "#1E9A46");
    sb.AppendLine("  </g>");
    sb.AppendLine("  <g id=\"anchors\">");
    Anchors(sb, L, 1);
    Anchors(sb, R, 14);
    sb.AppendLine("  </g>");
    sb.AppendLine("</svg>");
    return sb.ToString();
  }

  static void Anchors(StringBuilder sb, P[] pts, int n0) {
    for (int i = 0; i < pts.Length; i++) {
      int n = n0 + i;
      sb.Append("    <!-- ").Append(n).Append(" ").Append(pts[i].Edge).Append(" ").Append(pts[i].Certainty).Append(" ").Append(pts[i].Note).AppendLine(" -->");
      sb.Append("    <circle id=\"p").Append(n).Append("\" cx=\"").Append(pts[i].X).Append("\" cy=\"").Append(pts[i].Y);
      sb.Append("\" r=\"5\" data-edge=\"").Append(pts[i].Edge).Append("\" data-certainty=\"").Append(pts[i].Certainty);
      sb.Append("\" data-note=\"").Append(pts[i].Note).AppendLine("\" fill=\"#ffe14a\" stroke=\"#111\" stroke-width=\"1.5\"/>");
    }
  }

  static void Border(StringBuilder sb, string id, P[] pts, string edge, string color) {
    string certainty = "visible";
    for (int i = 0; i < pts.Length; i++) if (SegEdge(pts, i) == edge) { certainty = pts[(i + 1) % pts.Length].Certainty; break; }
    if (edge == "lateral" || edge == "inferior") certainty = "pending";
    sb.Append("    <path id=\"").Append(id).Append("\" data-edge=\"").Append(edge).Append("\" data-certainty=\"").Append(certainty).Append("\" stroke=\"").Append(color).Append("\"");
    if (certainty == "pending") sb.Append(" stroke-dasharray=\"8 6\"");
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

  static string SegEdge(P[] pts, int i) {
    int n = pts.Length;
    string dest = pts[(i + 1) % n].Edge;
    if (pts[i].Edge == "lateral" && dest == "superior") return "lateral";
    return dest;
  }

  static PointF[] Segment(P[] pts, int i) {
    int n = pts.Length;
    PointF p0 = Pt(pts[(i - 1 + n) % n]), p1 = Pt(pts[i]), p2 = Pt(pts[(i + 1) % n]), p3 = Pt(pts[(i + 2) % n]);
    return new PointF[] { p1, Add(p1, Mul(Sub(p2, p0), 1f / 6f)), Sub(p2, Mul(Sub(p3, p1), 1f / 6f)), p2 };
  }

  static void Stroke(Graphics g, P[] pts, Color color, float width, DashStyle dash) {
    using (var pen = new Pen(color, width)) {
      pen.DashStyle = dash;
      var path = new GraphicsPath();
      for (int i = 0; i < pts.Length; i++) {
        PointF[] s = Segment(pts, i);
        path.AddBezier(s[0], s[1], s[2], s[3]);
      }
      path.CloseFigure();
      g.DrawPath(pen, path);
    }
  }

  static void DrawZoom(Graphics g, P[] pts, int n0) {
    int x0 = 300, y0 = 390; float sc = 2f;
    using (var pen = new Pen(Color.FromArgb(210, 40, 40), 2f)) {
      pen.DashStyle = DashStyle.Dash;
      var path = new GraphicsPath();
      for (int i = 0; i < pts.Length; i++) {
        PointF[] s = Segment(pts, i);
        path.AddBezier(Z(s[0], x0, y0, sc), Z(s[1], x0, y0, sc), Z(s[2], x0, y0, sc), Z(s[3], x0, y0, sc));
      }
      g.DrawPath(pen, path);
    }
    using (var font = new Font("Consolas", 13f, FontStyle.Bold)) {
      for (int i = 0; i < pts.Length; i++) {
        float x = (pts[i].X - x0) * sc, y = (pts[i].Y - y0) * sc;
        Color fill = pts[i].Certainty == "pending" ? Color.OrangeRed : pts[i].Certainty == "reference" ? Color.LightSkyBlue : Color.White;
        using (var b = new SolidBrush(fill)) g.FillEllipse(b, x - 8, y - 8, 16, 16);
        g.DrawEllipse(Pens.Black, x - 8, y - 8, 16, 16);
        float tx = pts[i].X < 450 ? x - 26 : x + 10;
        using (var halo = new SolidBrush(Color.FromArgb(220, 255, 255, 255))) g.FillRectangle(halo, tx - 1, y - 18, 22, 16);
        g.DrawString((n0 + i).ToString(), font, Brushes.Black, tx, y - 18);
      }
    }
  }

  static PointF Z(PointF p, int x0, int y0, float s) { return new PointF((p.X - x0) * s, (p.Y - y0) * s); }
}
