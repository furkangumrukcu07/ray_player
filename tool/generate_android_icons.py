#!/usr/bin/env python3
"""Ray IPTV: TV banner + launcher rasters (Mina generate_android_icons.py ile aynı tablolar)."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
UMBRELLA = RES / "drawable-xxxhdpi" / "ic_launcher_foreground.png"
FONT = Path("/System/Library/Fonts/Supplemental/Arial Bold.ttf")

TV_BG = (0x0D, 0x11, 0x17, 255)
MIPMAP_BANNER = {
    "mdpi": (160, 90),
    "hdpi": (240, 135),
    "xhdpi": (320, 180),
    "xxhdpi": (480, 270),
    "xxxhdpi": (640, 360),
}
DRAWABLE_BANNER = {
    "xhdpi": (320, 180),
    "xxhdpi": (480, 270),
    "xxxhdpi": (640, 360),
}


def compose_banner(w: int, h: int, umbrella: Image.Image) -> Image.Image:
    canvas = Image.new("RGBA", (w, h), TV_BG)
    pad = int(w * 0.07)
    # Sol: şemsiye — TV overscan payı; metin sağda sığmalı.
    max_side = int(h * 0.58)
    uw, uh = umbrella.size
    scale = min(max_side / uw, max_side / uh)
    nw, nh = max(1, int(uw * scale)), max(1, int(uh * scale))
    icon = umbrella.resize((nw, nh), Image.Resampling.LANCZOS)
    ox = pad
    oy = (h - nh) // 2
    canvas.paste(icon, (ox, oy), icon)

    draw = ImageDraw.Draw(canvas)
    text_x = ox + nw + int(w * 0.045)
    max_text_w = w - text_x - pad
    lines = ["RAY", "PLAYER"]
    font_h = int(h * 0.24)
    font = None
    while font_h >= 10:
        font = ImageFont.truetype(str(FONT), font_h)
        widths = [draw.textbbox((0, 0), line, font=font)[2] for line in lines]
        if max(widths) <= max_text_w:
            break
        font_h -= 1
    assert font is not None
    gap = max(2, int(h * 0.035))
    total_h = font_h * 2 + gap
    ty = (h - total_h) // 2
    for i, line in enumerate(lines):
        draw.text((text_x, ty + i * (font_h + gap)), line, font=font, fill=(255, 255, 255, 255))
    return canvas


def main() -> None:
    umbrella = Image.open(UMBRELLA).convert("RGBA")
    master = compose_banner(1280, 720, umbrella)

    for name, (bw, bh) in MIPMAP_BANNER.items():
        d = RES / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        master.resize((bw, bh), Image.Resampling.LANCZOS).save(d / "tv_banner.png", optimize=True)

    for name, (bw, bh) in DRAWABLE_BANNER.items():
        d = RES / f"drawable-{name}"
        d.mkdir(parents=True, exist_ok=True)
        master.resize((bw, bh), Image.Resampling.LANCZOS).save(d / "tv_banner.png", optimize=True)

    tv_icon = RES / "mipmap-television-xhdpi" / "ic_launcher.png"
    if tv_icon.is_file():
        src = Image.open(tv_icon)
        for dens in ("mdpi", "hdpi", "tvdpi"):
            d = RES / f"mipmap-television-{dens}"
            d.mkdir(parents=True, exist_ok=True)
            src.save(d / "ic_launcher.png", optimize=True)

    print("OK banners + television mdpi/hdpi/tvdpi icons")


if __name__ == "__main__":
    main()
