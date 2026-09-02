# -*- coding: utf-8 -*-
import re
p = r"E:\SoftEngneeringHomework\Software_engineering\defense_ppt\readback_final.xml"
s = open(p, encoding="utf-8").read()

slides = re.findall(r"<slide[ >].*?</slide>", s, re.S)
print("slides:", len(slides))

# 1. no local placeholder / external url in img src
bad_at = re.findall(r'src="@[^"]*"', s)
bad_http = re.findall(r'src="https?://[^"]*"', s)
print("residual @path:", len(bad_at), "external http img:", len(bad_http))

# 2. images per slide + tokens
for i, sl in enumerate(slides, 1):
    imgs = re.findall(r'<img[^>]*src="([^"]+)"', sl)
    if imgs:
        print(f"slide {i}: {len(imgs)} imgs, tokens ok:", all(len(t) > 20 for t in imgs))

# 3. key texts must exist
keys = ["任务完成", "CI/CD", "微服务", "数据", "测试", "扩缩容", "故障", "性能对比",
        "276", "1142", "Run #217", "HPA", "503", "317", "待实测", "207", "演示路线"]
text = re.sub(r"<[^>]+>", "", s)
for k in keys:
    print(("OK  " if k in text else "MISS"), k)

# 4. every slide has a title (22pt bold) and note
for i, sl in enumerate(slides, 1):
    has_title = 'fontSize="22"' in sl
    has_note = "<note>" in sl
    if not (has_title and has_note):
        print("slide", i, "title:", has_title, "note:", has_note)
print("title/note check done")
