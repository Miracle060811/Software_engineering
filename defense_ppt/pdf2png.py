# -*- coding: utf-8 -*-
import fitz, sys
sys.stdout.reconfigure(encoding='utf-8')
pdf = r"D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\.preview\render\modified.pdf"
outdir = r"D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\.preview\render\pages"
import pathlib
pathlib.Path(outdir).mkdir(parents=True, exist_ok=True)
doc = fitz.open(pdf)
print("pages:", doc.page_count)
for i, page in enumerate(doc, 1):
    pix = page.get_pixmap(dpi=110)
    p = f"{outdir}/p{i:02d}.png"
    pix.save(p)
    print("saved", p, pix.width, "x", pix.height)
