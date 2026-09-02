# -*- coding: utf-8 -*-
import fitz  # PyMuPDF
import os, json

PDF = r"E:\微信\xwechat_files\wxid_ovgqxdoua57c22_e0a1\msg\file\2026-08\软件工程基础实践-2026夏.pdf"
OUT = r"E:\SoftEngneeringHomework\Software_engineering\defense_ppt"
IMGDIR = os.path.join(OUT, "pdf_images")
os.makedirs(IMGDIR, exist_ok=True)

doc = fitz.open(PDF)
print("PAGES:", doc.page_count)
summary = []
for i, page in enumerate(doc):
    text = page.get_text()
    imgs = page.get_images(full=True)
    tabs = page.find_tables()
    print(f"\n===== PAGE {i+1} | imgs={len(imgs)} tables={len(tabs.tables)} =====")
    print(text)
    for t in tabs.tables:
        print("  [TABLE]")
        for row in t.extract():
            print("   |", " | ".join("" if c is None else str(c).replace("\n"," ") for c in row))
    info = {"page": i+1, "n_img": len(imgs), "n_tab": len(tabs.tables)}
    summary.append(info)
    # render full page as image (to capture diagrams/screenshots)
    pix = page.get_pixmap(dpi=140)
    pix.save(os.path.join(IMGDIR, f"page_{i+1:02d}.png"))
    # extract embedded images
    for j, im in enumerate(imgs):
        xref = im[0]
        try:
            base = doc.extract_image(xref)
            fn = os.path.join(IMGDIR, f"p{i+1:02d}_img{j+1}.{base['ext']}")
            with open(fn, "wb") as f:
                f.write(base["image"])
        except Exception as e:
            print("img extract fail", xref, e)
print("\nSUMMARY:", json.dumps(summary, ensure_ascii=False))
