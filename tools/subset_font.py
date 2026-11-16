#!/usr/bin/env python3
"""重新生成品牌字体子集。

用途
----
`res/font/zqknbcgpst_subset.ttf` 是从郑庆科-南北词宫谱宋体裁出来的子集，
只保留项目里实际用到的字符。**一旦新增/修改了使用该字体的文案，
必须重跑本脚本**，否则新字符会回退成系统字体，字形不一致。

用法
----
    python tools/subset_font.py --source <原始字体.ttf>

    # 指定输出与额外字符
    python tools/subset_font.py --source X.ttf --output app/src/main/res/font/x.ttf \\
        --extra "额外字符"

依赖
----
    pip install fonttools

字符是从哪里来的
----------------
1. 所有 `fontFamily = KebenSong` 附近的字符串字面量（text = "..."）
2. `text = label` / `text = kind.label()` 这类间接引用，会去解析对应枚举
3. `res/values/strings.xml` 的全部字符串
4. ASCII 可见字符 + 常用中文标点（兜底，避免标点回退）
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MAIN = ROOT / "app" / "src" / "main"
KT = MAIN / "kotlin" / "com" / "lacrimosa" / "staria"

# 兜底字符：ASCII 可见区 + 常用中文标点
FALLBACK = "".join(chr(c) for c in range(0x20, 0x7F)) + "　：，。、；？！“”‘’（）《》—…·【】"


def literal_chars() -> set[str]:
    """从所有用到 KebenSong 的源码里抽取字符。"""
    chars: set[str] = set()

    for path in KT.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        if "KebenSong" not in text:
            continue
        lines = text.split("\n")
        for i, line in enumerate(lines):
            if "fontFamily = KebenSong" not in line:
                continue
            # 向上找最近的 text = "..." 或 text = 标识符
            for j in range(i, max(-1, i - 12), -1):
                m = re.search(r'text\s*=\s*"([^"]*)"', lines[j])
                if m:
                    chars.update(m.group(1))
                    break
                m = re.search(r"text\s*=\s*([A-Za-z_][\w.]*)\s*,", lines[j])
                if m:
                    chars.update(resolve_identifier(path.parent, m.group(1)))
                    break

    # strings.xml
    sx = (MAIN / "res" / "values" / "strings.xml").read_text(encoding="utf-8")
    for m in re.finditer(r"<string[^>]*>([^<]*)</string>", sx):
        chars.update(m.group(1))

    return chars


def resolve_identifier(scope: Path, ident: str) -> set[str]:
    """把 `label` / `themeColor.label` 这类引用解析成它可能的取值。"""
    name = ident.split(".")[-1]
    found: set[str] = set()

    for path in KT.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        # 形如 enum class Foo(val label: String, ...) { Bar("简约白", ...), ... }
        for m in re.finditer(r"enum class \w+\(val " + re.escape(name) + r": *String[^)]*\)\s*\{(.*?)\n\}", text, re.S):
            body = m.group(1)
            for v in re.finditer(r'\w+\(\s*"([^"]+)"', body):
                found.update(v.group(1))
        # 形如 EditKind.Diary -> "日记"
        for m in re.finditer(r'->\s*"([^"]+)"', text):
            found.update(m.group(1))
    return found


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", required=True, help="原始字体 ttf 路径")
    ap.add_argument(
        "--output",
        default=str(MAIN / "res" / "font" / "zqknbcgpst_subset.ttf"),
        help="输出 ttf 路径",
    )
    ap.add_argument("--extra", default="", help="额外补充的字符")
    ap.add_argument(
        "--no-fallback",
        action="store_true",
        help="不加 ASCII 与中文标点兜底",
    )
    args = ap.parse_args()

    chars = literal_chars()
    if not args.no_fallback:
        chars.update(FALLBACK)
    chars.update(args.extra)

    # 源字体可能不含这些 ASCII 符号，子集时一并剔除并由调用方决定是否保留
    chars = {c for c in chars if c.strip() or c == "　"} | {"　"}
    text = "".join(sorted(chars))

    tmp = Path(sys.argv[0]).resolve().parent / "_charset.txt"
    tmp.write_text(text, encoding="utf-8")

    cmd = [
        sys.executable, "-m", "fontTools.subset", args.source,
        f"--text-file={tmp}",
        f"--output-file={args.output}",
        "--layout-features=*",
        "--glyph-names",
        "--symbol-cmap",
        "--legacy-cmap",
        "--notdef-glyph",
        "--notdef-outline",
        "--recommended-glyphs",
        "--drop-tables+=DSIG",
        "--no-hinting",
    ]
    print("字符数:", len(text))
    print("执行:", " ".join(cmd[:4]), "...")
    subprocess.run(cmd, check=True)

    out = Path(args.output)
    print(f"输出: {out}  {out.stat().st_size / 1024:.1f} KB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
