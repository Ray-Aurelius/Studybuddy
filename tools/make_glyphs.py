#!/usr/bin/env python3
"""
Sophinity glyphs: the app's own Greek-themed line-art icon set.

Every glyph is drawn on a 24 x 24 grid with one stroke weight and round caps,
so the set reads as a family (pottery-painting line work, not emoji).
Output:
  app/src/main/res/drawable/gl_<name>.xml   Android vector drawables (tinted in the UI)
  docs/brand/glyphs.svg                      contact sheet for review and for the brand guide

Run:  python3 tools/make_glyphs.py
"""
import math, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SW = 1.7  # stroke width on the 24-unit grid


def f(v):
    s = f"{v:.2f}".rstrip('0').rstrip('.')
    return '0' if s in ('-0', '') else s


def P(*pts):
    return ' '.join(f(p) for p in pts)


def circle(cx, cy, r):
    return f"M{P(cx - r, cy)}a{P(r, r)} 0 1 0 {P(2 * r, 0)}a{P(r, r)} 0 1 0 {P(-2 * r, 0)}z"


def ellipse(cx, cy, rx, ry, rot=0.0):
    a = math.radians(rot)
    x1, y1 = cx + rx * math.cos(a), cy + rx * math.sin(a)
    x2, y2 = cx - rx * math.cos(a), cy - rx * math.sin(a)
    return f"M{P(x1, y1)}A{P(rx, ry)} {f(rot)} 1 0 {P(x2, y2)}A{P(rx, ry)} {f(rot)} 1 0 {P(x1, y1)}z"


def polar(cx, cy, r, deg):
    a = math.radians(deg)
    return cx + r * math.cos(a), cy + r * math.sin(a)


def leaf(bx, by, deg, length, width):
    """Filled vesica-shaped leaf from base (bx, by) pointing at angle deg (screen coords)."""
    tx, ty = polar(bx, by, length, deg)
    mx, my = (bx + tx) / 2, (by + ty) / 2
    nx, ny = polar(0, 0, width, deg + 90)
    return f"M{P(bx, by)}Q{P(mx + nx, my + ny, tx, ty)}Q{P(mx - nx, my - ny, bx, by)}z"


def arc_pts(cx, cy, r, a0, a1, n):
    return [polar(cx, cy, r, a0 + (a1 - a0) * i / (n - 1)) for i in range(n)]


def poly(pts, close=False):
    d = 'M' + P(*pts[0]) + ''.join('L' + P(*p) for p in pts[1:])
    return d + ('z' if close else '')


def spark(cx, cy, r):
    """Four-point star with concave sides."""
    return (f"M{P(cx, cy - r)}Q{P(cx, cy, cx + r, cy)}Q{P(cx, cy, cx, cy + r)}"
            f"Q{P(cx, cy, cx - r, cy)}Q{P(cx, cy, cx, cy - r)}z")


def mirror_x(d):
    """Mirror a path made only of absolute M/L/Q/C commands across x = 12."""
    out, tok, i = [], d.replace(',', ' '), 0
    import re
    parts = re.findall(r'[MLQCZz]|-?\d*\.?\d+', tok)
    cmd = None
    nums = []
    def flush():
        if cmd is None: return
        if cmd in 'Zz': out.append('z'); return
        vals = [float(v) for v in nums]
        res = []
        for j, v in enumerate(vals):
            res.append(24 - v if j % 2 == 0 else v)
        out.append(cmd + P(*res))
    for p in parts:
        if p in 'MLQCZz':
            flush(); cmd = p; nums = []
        else:
            nums.append(p)
    flush()
    return ''.join(out)


# ---- composite generators -------------------------------------------------

def wreath(cx=12, cy=11.4, r=7.2, top_gap=40, nodes=4, leaf_len=4.4, leaf_w=1.6, ribbon=True):
    """Two laurel branches; leaves grow in pairs, pointing up toward the open top."""
    els = []
    a0, a1 = 104, 270 - top_gap
    stem = arc_pts(cx, cy, r, a0, a1, 24)
    els.append(('s', poly(stem)))
    left = []
    for i in range(nodes):
        t = 0.12 + 0.8 * i / (nodes - 1)
        ang = a0 + (a1 - a0) * t
        bx, by = polar(cx, cy, r, ang)
        tangent = ang + 90
        size = leaf_len * (0.88 + 0.12 * t)
        left.append(leaf(bx, by, tangent + 42, size, leaf_w))
        left.append(leaf(bx, by, tangent - 42, size * 0.92, leaf_w * 0.95))
    tipx, tipy = polar(cx, cy, r, a1)
    left.append(leaf(tipx, tipy, a1 + 90, leaf_len * 0.95, leaf_w))
    for d in left:
        els.append(('f', d))
    els.append(('s', poly([(24 - x, y) for x, y in stem])))
    for d in left:
        els.append(('f', mirror_x(d)))
    if ribbon:
        els.append(('s', f"M{P(cx - 2.4, cy + r + 2.6)}L{P(cx, cy + r)}L{P(cx + 2.4, cy + r + 2.6)}"))
    return els


def olive_branch():
    els = [('s', "M4 20.8Q8.6 14.2 19.8 4.2")]
    # (point on stem, leaf direction)
    for (x, y), a in (((6.6, 17.2), -100), ((8.9, 14.5), 5), ((11.6, 11.8), -100), ((14.5, 9.2), 5), ((17.3, 6.6), -100)):
        els.append(('f', leaf(x, y, a, 5.0, 1.35)))
    els.append(('s', "M10.2 13 11.6 16.4"))
    els.append(('f', ellipse(11.9, 17.6, 1.25, 1.6, -20)))
    els.append(('s', "M15.9 7.9 17.6 10.8"))
    els.append(('f', ellipse(17.9, 12, 1.25, 1.6, -20)))
    return els


def gear(cx=12, cy=12, ro=8.6, ri=6.6, n=9):
    pts = []
    for i in range(n):
        base = 360 * i / n
        w = 360 / n
        for a, r in ((base - w * 0.32, ri), (base - w * 0.18, ro), (base + w * 0.18, ro), (base + w * 0.32, ri)):
            pts.append(polar(cx, cy, r, a))
    return [('s', poly(pts, True)), ('s', circle(cx, cy, 2.6)),
            ('s', poly([polar(cx, cy, 2.6, 90), polar(cx, cy, 6.6, 90)])),
            ('s', poly([polar(cx, cy, 2.6, 210), polar(cx, cy, 6.6, 210)])),
            ('s', poly([polar(cx, cy, 2.6, 330), polar(cx, cy, 6.6, 330)]))]


def sun(cx=12, cy=12, r=4.1, rays=8, r1=6.4, r2=8.9):
    els = [('s', circle(cx, cy, r))]
    for i in range(rays):
        els.append(('s', poly([polar(cx, cy, r1, 360 * i / rays - 90), polar(cx, cy, r2, 360 * i / rays - 90)])))
    return els


def sundial():
    cx, cy, r = 12, 17.5, 8.2
    els = [('s', "M2.5 17.5h19"), ('s', f"M{P(cx - r, cy)}A{P(r, r)} 0 0 1 {P(cx + r, cy)}")]
    for a in (200, 230, 250, 290, 310, 340):
        els.append(('s', poly([polar(cx, cy, r - 2.4, a), polar(cx, cy, r, a)])))
    els.append(('f', "M12 17.5V9.6l3.4 7.9z"))
    els.append(('s', "M7 20.5h10"))
    return els


def armillary():
    return [('s', circle(12, 10.5, 7.2)), ('s', ellipse(12, 10.5, 7.2, 2.4, -24)),
            ('s', ellipse(12, 10.5, 7.2, 2.4, 66)), ('f', circle(12, 10.5, 1.3)),
            ('s', "M12 17.7v2.6M8.5 21h7")]


def theatre():
    return [('s', "M2.8 18.5a9.2 9.2 0 0 1 18.4 0"), ('s', "M6.3 18.5a5.7 5.7 0 0 1 11.4 0"),
            ('s', "M2 21h20"), ('f', "M10.6 13.9v4l3.4-2z")]


def masks():
    left = "M2.5 4.5h8.5v5.2c0 3.5-1.9 5.8-4.25 5.8S2.5 13.2 2.5 9.7z"
    right = "M13 8.5h8.5v5.2c0 3.5-1.9 5.8-4.25 5.8S13 17.2 13 13.7z"
    return [('s', left), ('s', right),
            ('f', ellipse(4.9, 8.2, 1.0, 0.65, 20)), ('f', ellipse(8.6, 8.2, 1.0, 0.65, -20)),
            ('s', "M4.6 12.6q2.15-1.6 4.3 0"),  # tragedy: frown
            ('f', ellipse(15.4, 12.2, 1.0, 0.65, -20)), ('f', ellipse(19.1, 12.2, 1.0, 0.65, 20)),
            ('s', "M15.1 15.2q2.15 1.9 4.3 0")]  # comedy: smile


def lyre():
    return [('s', "M7.3 15.5C4.2 13 3.6 8.2 5.6 4.2"), ('s', "M16.7 15.5c3.1-2.5 3.7-7.3 1.7-11.3"),
            ('f', circle(5.6, 3.6, 1.05)), ('f', circle(18.4, 3.6, 1.05)),
            ('s', "M4.6 7h14.8"),
            ('s', "M6.3 15h11.4v2.4a3.1 3.1 0 0 1-3.1 3.1H9.4a3.1 3.1 0 0 1-3.1-3.1z"),
            ('s', "M9.7 7v8M12 7v8M14.3 7v8")]


def scroll():
    return [('s', "M3.5 5.6a1.6 1.6 0 0 1 3.2 0v12.8a1.6 1.6 0 0 1-3.2 0z"),
            ('s', "M17.3 5.6a1.6 1.6 0 0 1 3.2 0v12.8a1.6 1.6 0 0 1-3.2 0z"),
            ('s', "M6.7 6.6h10.6M6.7 17.4h10.6"),
            ('s', "M9.3 10.3h5.4M9.3 13.7h3.8")]


def scroll_stack():
    els = []
    for x, y in ((6.6, 17.6), (12, 17.6), (17.4, 17.6), (9.3, 12.5), (14.7, 12.5), (12, 7.4)):
        els.append(('s', circle(x, y, 2.55)))
        els.append(('f', circle(x, y, 0.75)))
    return els


def stoa():
    return [('s', "M3 8.6 12 3.8l9 4.8z"), ('s', "M5.6 10.8v6.6M9.9 10.8v6.6M14.1 10.8v6.6M18.4 10.8v6.6"),
            ('s', "M3.8 19.3h16.4M2.6 21.2h18.8")]


def tablet():
    return [('s', "M3.5 5.5h10.6a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H3.5z"),
            ('s', "M6 9.3h6.2M6 12.5h6.2M6 15.7h4"),
            ('s', "M20.6 3.4 16.3 15.6"), ('f', "M16.3 15.6l-.55 2.9 2-2.15z")]


def papyrus():
    return [('s', "M5.5 3h8.8l4.2 4.3V21H5.5z"), ('s', "M14.3 3v4.3h4.2"),
            ('s', "M8.3 11.2h7.4M8.3 14.4h7.4M8.3 17.6h4.6")]


def meander():
    return [('s', "M4.5 19.5v-15h15v15h-10v-10h5v5")]


def hourglass():
    return [('s', "M5.5 3h13M5.5 21h13"),
            ('s', "M7.3 3c0 5.2 8.4 5.8 8.4 9s-8.4 3.8-8.4 9"),
            ('s', "M16.7 3c0 5.2-8.4 5.8-8.4 9s8.4 3.8 8.4 9"),
            ('f', "M9.4 19.6h5.2L12 16.9z"), ('f', "M10.3 7.2h3.4L12 9z")]


def torch():
    return [('s', "M10.4 21.2h3.2l1.1-8.6H9.3z"), ('s', "M8.2 12.6h7.6"),
            ('s', "M12 2.8c2.6 2.6 3.7 4.3 3.7 6a3.7 3.7 0 0 1-7.4 0c0-1.7 1.3-3.4 3.7-6z"),
            ('f', "M12 6.8c1.1 1.1 1.5 1.8 1.5 2.6a1.5 1.5 0 0 1-3 0c0-.8.5-1.5 1.5-2.6z")]


def flame():
    return [('s', "M12 2.5c3.6 3.3 5.6 6.2 5.6 9.7a5.6 5.6 0 0 1-11.2 0c0-2.4 1-4.2 2.6-5.8.3 1.8 1 2.9 2.1 3.4C10.8 7.2 11 4.9 12 2.5z"),
            ('f', "M12 12.4c1.5 1.4 2.2 2.4 2.2 3.6a2.2 2.2 0 0 1-4.4 0c0-1.2.7-2.2 2.2-3.6z")]


def columns():
    els = []
    for x0, top in ((4.2, 13.6), (10, 9.4), (15.8, 5.2)):
        els.append(('s', f"M{P(x0 - 0.7, top)}h{f(5.4)}"))
        els.append(('s', f"M{P(x0 + 0.5, top + 1.6)}V19.5M{P(x0 + 3.5, top + 1.6)}V19.5"))
    els.append(('s', "M2.5 21h19"))
    return els


def ionic_column():
    return [('s', "M5.6 6.2h12.8"), ('s', circle(6.9, 8.4, 1.5)), ('s', circle(17.1, 8.4, 1.5)),
            ('s', "M8.4 8.4h7.2"), ('s', "M9 10.3v8M15 10.3v8M11 10.3v8M13 10.3v8"),
            ('s', "M7.6 18.6h8.8M6.2 21h11.6")]


def broken_column():
    return [('s', "M8 21V10.2l1.6-1.4 1.3 1.2 1.6-2.2 1.4 1.6 2.1-1.3V21"),
            ('s', "M10.7 12.4V21M13.3 11.3V21"), ('s', "M5.5 21h13"),
            ('s', "M17.8 17.6l2.6-1.4 1 1.9-2.6 1.4z")]


def bow_arrow():
    return [('s', "M8.4 2.8C13.9 6.6 13.9 17.4 8.4 21.2"), ('s', "M8.4 2.8 4.8 12l3.6 9.2"),
            ('s', "M4.8 12h15.6"), ('f', "M21.3 12l-3.3-2.3v4.6z"),
            ('s', "M4.8 12 3 10.2M4.8 12 3 13.8M6.6 12 4.8 10.2M6.6 12 4.8 13.8")]


def target():
    return [('s', circle(10.5, 13.5, 7.6)), ('s', circle(10.5, 13.5, 4.4)), ('f', circle(10.5, 13.5, 1.5)),
            ('s', "M10.5 13.5 19.8 4.2"), ('s', "M19.8 4.2h-2.6M19.8 4.2v2.6M18.2 3v2.6M21 5.8h-2.6")]


def owl():
    return [('s', "M12 21c-4.1 0-6.2-3-6.2-7.2V7.6l2.6 2.2c1-.9 2.3-1.4 3.6-1.4s2.6.5 3.6 1.4l2.6-2.2v6.2C18.2 18 16.1 21 12 21z"),
            ('s', circle(9.5, 13.1, 2.15)), ('s', circle(14.5, 13.1, 2.15)),
            ('f', circle(9.5, 13.1, 0.85)), ('f', circle(14.5, 13.1, 0.85)),
            ('f', "M12 14.5l-.85 1.5h1.7z"), ('s', "M10 18.2l2 .9 2-.9")]


def coin():
    return [('s', circle(12, 12, 8.6)), ('s', circle(12, 12, 6.4)), ('s', "M12 8.2 15.6 15H8.4z")]


def shield():
    return [('s', circle(12, 12, 8.6)), ('s', circle(12, 12, 6.2)), ('s', "M9.2 15.6 12 8.6l2.8 7")]


def scales():
    return [('f', circle(12, 3.8, 1.1)), ('s', "M12 4.9v14.6M8.3 20.3h7.4"), ('s', "M4.3 7.2h15.4"),
            ('s', "M4.3 7.2 2 13M4.3 7.2 6.6 13M19.7 7.2 17.4 13M19.7 7.2 22 13"),
            ('s', "M1.8 13a2.5 2.5 0 0 0 5 0zM17.2 13a2.5 2.5 0 0 0 5 0z")]


def butterfly():
    return [('s', "M12 8.5v10"), ('s', "M12 8.5l-2-3.4M12 8.5l2-3.4"),
            ('s', "M12 10.5C10.2 6 4 4.6 3.6 8.4 3.3 11.4 7.3 12.6 12 12z"),
            ('s', "M12 12.6c-3.4.6-6.2 2.8-5.2 5.4.9 2.3 4.3.6 5.2-3"),
            ('s', "M12 10.5c1.8-4.5 8-5.9 8.4-2.1.3 3-3.7 4.2-8.4 3.6z"),
            ('s', "M12 12.6c3.4.6 6.2 2.8 5.2 5.4-.9 2.3-4.3.6-5.2-3")]


def amphora():
    """Two-handled storage jar: narrow neck, broad shoulders, body tapering to a small foot."""
    return [('s', "M9.4 2.9h5.2"),
            ('s', "M10.4 2.9v3.3C7.7 7 6.3 8.7 6.3 11c0 3.5 2.2 6.3 4.3 8.3h2.8c2.1-2 4.3-4.8 4.3-8.3 0-2.3-1.4-4-4.1-4.8V2.9"),
            ('s', "M10.4 4.4C8 4.1 6.9 5.4 7.5 8M13.6 4.4C16 4.1 17.1 5.4 16.5 8"),
            ('s', "M7 12.3h10"), ('s', "M10.9 19.3l-.6 1.9h3.4l-.6-1.9")]


def asclepius():
    return [('s', "M12 3.2v17.6"), ('f', circle(12, 3.2, 1.1)),
            ('s', "M14.6 5.6c-3.4-.4-5.6 1.2-2.6 2.8 3.4 1.8 3.6 3.6.2 4.9-3 1.2-3 2.8 0 3.9 1.6.6 2 1.4 1.3 2.2"),
            ('f', ellipse(15.2, 5.7, 1.1, 0.75, 10))]


def kerykeion():
    return [('s', "M12 5.2V21.2"), ('f', circle(12, 4.2, 1.1)),
            ('s', "M12 6c-1.8-2.1-5-2.8-7.6-1.6 2.1.5 3.5 1.5 4.4 3M12 6c1.8-2.1 5-2.8 7.6-1.6-2.1.5-3.5 1.5-4.4 3"),
            ('s', "M9.6 8.4c4.8 1.4 4.8 3.8 0 5.2s-4.8 3.8 0 5.2"),
            ('s', "M14.4 8.4c-4.8 1.4-4.8 3.8 0 5.2s4.8 3.8 0 5.2")]


def quill():
    return [('s', "M20.2 3.2C13 4.4 7.6 9.6 5.4 17.8"),
            ('s', "M20.2 3.2c-.6 6.4-4.8 11-11.8 13.6"),
            ('s', "M12.6 9.6l-3.2-.6M15.4 7.4l-2.6-.9M10.4 12.6l-2.6-.3"),
            ('s', "M5.4 17.8 3.6 21")]


def alpha_omega():
    return [('s', "M2.6 19.2 6.9 5l4.3 14.2M4.2 14.3h5.4"),
            ('s', "M13 19.2h3v-1.6a4.7 4.7 0 1 1 3.4 0v1.6h3")]


def ship():
    return [('s', "M2.2 14.2h19.6c-1.1 3.1-3.2 4.6-6.4 4.6H8.4c-3 0-4.9-1.6-6.2-4.6z"),
            ('s', "M12 14.2V3.2"), ('s', "M6.8 4.8h10.4v7.2H6.8z"),
            ('s', "M7 18.8 5.6 21.4M10 18.8l-.8 2.6M14 18.8l.8 2.6M17 18.8l1.4 2.6"),
            ('f', circle(19.4, 15.8, 0.75))]


def mountain():
    return [('s', "M2 19.8 8.4 8.6l3.3 5.1 3.2-6.8L22 19.8z"), ('s', "M6.6 11.8l1.8 1 1.7-1M12.8 11.3l2.1 1.2 1.9-1.6"),
            ('s', "M3.6 5.2h4.6M15.6 3.6h5"), ('f', spark(12, 3.6, 1.6))]


def club():
    return [('s', "M3.2 19.6 11.2 11.6Q10.2 9.6 12 8.4Q12.4 5.2 15.6 4.6Q17.2 2.6 19.2 4Q21.8 4.6 20.6 7.4Q21.8 10 19.2 11.2Q18.2 13.8 15.4 12.8L5 21.4z"),
            ('f', circle(15, 7.6, 0.85)), ('f', circle(17.8, 8.4, 0.85)), ('f', circle(16.2, 10.4, 0.85))]


def stars():
    return [('f', spark(8.4, 9.6, 5.6)), ('f', spark(17.6, 16.4, 3.8)), ('f', spark(17.8, 5.2, 2.3))]


def tripod():
    return [('s', "M4.6 8.4h14.8c0 3-3.3 5.2-7.4 5.2s-7.4-2.2-7.4-5.2z"),
            ('s', circle(7, 5.6, 1.6)), ('s', circle(17, 5.6, 1.6)),
            ('s', "M8.2 12.4 5.6 21M12 13.6V21M15.8 12.4l2.6 8.6")]


def moon_full():
    return [('s', circle(12, 12, 8.6)), ('s', circle(9.2, 9.6, 1.6)), ('s', circle(14.4, 14.6, 2.2)),
            ('s', "M14.6 6.6a3.6 3.6 0 0 1 2.8 2.8")]


def moon():
    return [('s', "M15.8 3.5A8.8 8.8 0 1 0 20.6 16a7 7 0 0 1-4.8-12.5z"), ('f', spark(18.5, 6.2, 1.7))]


def anvil():
    return [('s', "M3 11.2h12.4c0 2.1-1.4 3.4-3.6 3.8v2.6h2.6v2.6H6.6v-2.6h2.6V15C6.4 14.8 4 13.6 3 11.2z"),
            ('s', "M15.4 11.2h4.6c-.8 1.6-2.4 2.4-4.6 2.4"), ('s', "M14.2 3.2 19 8M16.6 5.6 12 10.2")]


def stadium():
    """The stadion: the Greek running track, for long efforts."""
    return [('s', "M7.4 5h9.2a7 7 0 0 1 0 14H7.4a7 7 0 0 1 0-14z"),
            ('s', "M7.8 8.6h8.4a3.4 3.4 0 0 1 0 6.8H7.8a3.4 3.4 0 0 1 0-6.8z"),
            ('s', "M12 5v3.6")]


def wheel():
    els = [('s', circle(12, 12, 8.6)), ('s', circle(12, 12, 2))]
    for a in range(0, 360, 60):
        els.append(('s', poly([polar(12, 12, 2, a), polar(12, 12, 8.6, a)])))
    return els


def helios():
    els = [('s', circle(12, 12, 5.2))]
    for a in range(0, 360, 90):
        els.append(('s', poly([polar(12, 12, 1.2, a + 45), polar(12, 12, 5.2, a + 45)])))
    for i in range(12):
        els.append(('s', poly([polar(12, 12, 7.2, i * 30), polar(12, 12, 9.2 if i % 2 == 0 else 8.4, i * 30)])))
    return els


def kylix():
    return [('s', "M3.4 9.2h17.2c0 3.6-3.8 6-8.6 6s-8.6-2.4-8.6-6z"), ('s', "M12 15.2v3.6M8.6 20.2h6.8"),
            ('s', "M3.8 11.2c-2.2.2-2.6-2.4-.4-2.2M20.2 11.2c2.2.2 2.6-2.4.4-2.2")]


def olive_tree():
    return [('s', "M12 21v-6.4M12 17c-1.2-.8-2.6-1-3.6-.6M12 15.6c1.3-.9 2.6-1.1 3.8-.6"),
            ('s', "M7.4 14.6C4.4 14.6 3 12.6 3.6 10.4c.4-1.6 1.8-2.4 3.2-2.4.4-2.6 2.4-4.4 5.2-4.4s4.8 1.8 5.2 4.4c1.4 0 2.8.8 3.2 2.4.6 2.2-.8 4.2-3.8 4.2z"),
            ('f', ellipse(9, 9.6, .8, 1.05, -20)), ('f', ellipse(14.6, 8.6, .8, 1.05, 20)), ('f', ellipse(16, 12.2, .8, 1.05, 20)),
            ('s', "M7.6 21h8.8")]


def lighthouse():
    return [('s', "M8.6 21l1.3-7.2h4.2l1.3 7.2"), ('s', "M10 13.8l.6-4.8h2.8l.6 4.8"),
            ('s', "M10.9 9V6.8h2.2V9"), ('f', "M12 2.2c1 1 1.5 1.8 1.5 2.6a1.5 1.5 0 0 1-3 0c0-.8.5-1.6 1.5-2.6z"),
            ('s', "M5.6 21h12.8M6.4 6.4l-2.6-1M17.6 6.4l2.6-1M6.4 9.2H3.6M17.6 9.2h2.8")]


def capsa():
    return [('s', "M6 10.2h12V20a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1z"), ('s', "M5 10.2h14"),
            ('s', circle(8.6, 7.6, 1.7)), ('s', circle(12, 6.4, 1.7)), ('s', circle(15.4, 7.6, 1.7)),
            ('s', "M9 14h6")]


def key():
    return [('s', circle(7, 7, 3.6)), ('s', "M9.6 9.6 19.6 19.6"), ('s', "M16.4 16.4l-2.2 2.2M19 19l-2 2")]


def trident():
    return [('s', "M12 21.2V3.4"), ('s', "M6.4 4.2v4.2c0 2.2 2.4 3.6 5.6 3.6s5.6-1.4 5.6-3.6V4.2"),
            ('f', "M12 2.2l1.6 2.6h-3.2zM6.4 3l1.6 2.6H4.8zM17.6 3l1.6 2.6h-3.2z")]


def lamp():
    return [('s', "M3.6 15c0-2.2 3.4-3.6 7.6-3.6 2.8 0 4.8.7 6.4 1.7l3-1.3c.5 1.1-.4 3-2.8 3.6-1.4 1.5-3.8 2.7-7.4 2.7-4 0-6.8-1.1-6.8-3.1z"),
            ('s', "M4 14.3c-2-.4-2.2-3.2.2-3"), ('f', circle(10.2, 13.6, 0.9)),
            ('f', "M20.4 6.4c1.1 1.1 1.6 2 1.6 2.8a1.6 1.6 0 0 1-3.2 0c0-.8.5-1.7 1.6-2.8z"), ('s', "M7 21h8")]


def eye():
    return [('s', "M2.4 12c3-4.6 6-6.6 9.6-6.6s6.6 2 9.6 6.6c-3 4.6-6 6.6-9.6 6.6S5.4 16.6 2.4 12z"),
            ('s', circle(12, 12, 3.2)), ('f', circle(12, 12, 1.3))]


def herm():
    """A herm: a stone pillar topped with a bearded head, the classic portrait of a philosopher."""
    return [('s', circle(12, 5.4, 2.7)), ('f', "M9.3 6.4c0 3.6 1.2 5.6 2.7 5.6s2.7-2 2.7-5.6c-.8.9-1.7 1.3-2.7 1.3s-1.9-.4-2.7-1.3z"),
            ('s', "M8.2 13h7.6l-.9 8H9.1z"), ('s', "M6.6 21h10.8")]


def delphi_e():
    return [('s', circle(12, 12, 8.6)), ('s', "M14.6 7.6H9.4v8.8h5.2M9.4 12h4.2")]


def dyad():
    return [('s', circle(9, 12, 5.8)), ('s', circle(15, 12, 5.8))]


def mosaic():
    els = []
    fills = {(0, 0), (1, 1), (2, 2), (2, 0)}
    for r in range(3):
        for c in range(3):
            x, y = 3.4 + c * 6, 3.4 + r * 6
            d = f"M{P(x, y)}h5v5h-5z"
            els.append(('f' if (r, c) in fills else 's', d if (r, c) in fills else f"M{P(x + .4, y + .4)}h4.2v4.2h-4.2z"))
    return els


def bricks():
    return [('s', "M3 20.5h18M3 15.5h18M3 10.5h18M3 5.5h18"),
            ('s', "M3 5.5v15M21 5.5v15M9 15.5v5M15 15.5v5M6 10.5v5M12 10.5v5M18 10.5v5M9 5.5v5M15 5.5v5")]


def sunrise():
    cx, cy = 12, 16.6
    els = [('s', "M2.2 16.6h19.6"), ('s', f"M{P(cx - 4.6, cy)}a4.6 4.6 0 0 1 9.2 0")]
    for a in (180, 210, 240, 270, 300, 330, 360):
        els.append(('s', poly([polar(cx, cy, 6.6, a), polar(cx, cy, 8.8, a)])))
    els.append(('s', "M6.4 19.6h11.2M9.4 22h5.2"))
    return els


def sprout():
    return [('s', "M12 21v-8.6"), ('s', "M12 13.4C7.8 13.6 5.6 11.4 5.6 8c3.6 0 6.4 2 6.4 5.4z"),
            ('s', "M12 11.4c0-3.6 2.4-6.2 6.2-6.2 0 3.6-2.6 6.2-6.2 6.2z"), ('s', "M7 21h10")]


def steps():
    return [('s', "M2.5 20.5h19M4.5 20.5v-3.4h15v3.4M7 17.1v-3.4h10v3.4M9.5 13.7v-3.4h5v3.4"),
            ('s', "M12 7.6V2.8M9.8 5 12 2.8 14.2 5")]


def waves():
    return [('f', circle(5, 12, 1.7)), ('s', "M9 8.2a5.4 5.4 0 0 1 0 7.6"), ('s', "M12.6 5.4a9.4 9.4 0 0 1 0 13.2"),
            ('s', "M16.2 2.8a13.2 13.2 0 0 1 0 18.4")]


def check():
    return [('s', "M5 12.6l4.4 4.4L19 7.4")]


GLYPHS = {
    # library types
    'scroll': scroll, 'lyre': lyre, 'masks': masks, 'stoa': stoa, 'tablet': tablet, 'theatre': theatre,
    'papyrus': papyrus, 'meander': meander,
    # stats and navigation
    'sun': sun, 'sundial': sundial, 'hourglass': hourglass, 'torch': torch, 'columns': columns,
    'bow': bow_arrow, 'wreath': wreath, 'scroll_stack': scroll_stack, 'target': target,
    # subjects and pickers
    'alpha_omega': alpha_omega, 'gear': gear, 'coin': coin, 'scales': scales, 'armillary': armillary,
    'broken_column': broken_column, 'butterfly': butterfly, 'amphora': amphora, 'asclepius': asclepius,
    'quill': quill, 'owl': owl, 'ship': ship, 'olive': olive_branch, 'shield': shield, 'anvil': anvil,
    'kylix': kylix, 'stars': stars, 'eye': eye, 'lamp': lamp, 'sprout': sprout, 'wheel': wheel,
    'waves': waves, 'trident': trident, 'key': key,
    # laurels and titles
    'flame': flame, 'ionic': ionic_column, 'steps': steps, 'mountain': mountain, 'club': club,
    'tripod': tripod, 'moon_full': moon_full, 'moon': moon, 'stadium': stadium, 'helios': helios,
    'capsa': capsa, 'olive_tree': olive_tree, 'lighthouse': lighthouse, 'herm': herm,
    'delphi_e': delphi_e, 'dyad': dyad, 'mosaic': mosaic, 'bricks': bricks, 'sunrise': sunrise,
    'kerykeion': kerykeion, 'check': check,
}


def android_xml(els):
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<!-- Generated by tools/make_glyphs.py. Edit that script, not this file. -->',
             '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
             '    android:width="24dp" android:height="24dp"',
             '    android:viewportWidth="24" android:viewportHeight="24">']
    for kind, d in els:
        if kind == 'f':
            lines.append(f'    <path android:fillColor="#FF000000" android:pathData="{d}"/>')
        else:
            lines.append(f'    <path android:strokeColor="#FF000000" android:strokeWidth="{SW}" '
                         f'android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{d}"/>')
    lines.append('</vector>\n')
    return '\n'.join(lines)


def svg_group(els, color='#123A5E'):
    out = []
    for kind, d in els:
        if kind == 'f':
            out.append(f'<path fill="{color}" d="{d}"/>')
        else:
            out.append(f'<path fill="none" stroke="{color}" stroke-width="{SW}" stroke-linecap="round" stroke-linejoin="round" d="{d}"/>')
    return ''.join(out)


def main():
    draw = os.path.join(ROOT, 'app/src/main/res/drawable')
    os.makedirs(draw, exist_ok=True)
    for old in os.listdir(draw):
        if old.startswith('gl_'):
            os.remove(os.path.join(draw, old))
    names = sorted(GLYPHS)
    for n in names:
        open(os.path.join(draw, f'gl_{n}.xml'), 'w').write(android_xml(GLYPHS[n]()))
    kt = os.path.join(ROOT, 'app/src/main/java/com/rayaurelius/sophinity/ui/glyphs/GlyphRegistry.kt')
    os.makedirs(os.path.dirname(kt), exist_ok=True)
    body = ',\n'.join(f'    "{n}" to R.drawable.gl_{n}' for n in names)
    open(kt, 'w').write('// Generated by tools/make_glyphs.py. Edit that script, not this file.\n'
                        'package com.rayaurelius.sophinity.ui.glyphs\n\n'
                        'import com.rayaurelius.sophinity.R\n\n'
                        '/** Every Sophinity glyph by its stable name (names are stored in the database). */\n'
                        f'internal val glyphDrawables: Map<String, Int> = mapOf(\n{body},\n)\n')
    cols, cell, scale = 8, 96, 3
    rows = (len(names) + cols - 1) // cols
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{cols * cell}" height="{rows * (cell + 18)}" '
             f'font-family="sans-serif" font-size="11"><rect width="100%" height="100%" fill="#F7F3EA"/>']
    for i, n in enumerate(names):
        x, y = (i % cols) * cell, (i // cols) * (cell + 18)
        parts.append(f'<g transform="translate({x + 12},{y + 8}) scale({scale})">{svg_group(GLYPHS[n]())}</g>')
        parts.append(f'<text x="{x + cell / 2}" y="{y + cell + 10}" text-anchor="middle" fill="#555">{n}</text>')
    parts.append('</svg>')
    os.makedirs(os.path.join(ROOT, 'docs/brand'), exist_ok=True)
    open(os.path.join(ROOT, 'docs/brand/glyphs.svg'), 'w').write('\n'.join(parts))
    print(f'{len(names)} glyphs')


if __name__ == '__main__':
    main()
