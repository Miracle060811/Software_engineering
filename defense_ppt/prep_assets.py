# -*- coding: utf-8 -*-
import shutil, os, struct

ROOT = r"E:\SoftEngneeringHomework\Software_engineering"
SRC_DIR = os.path.join(ROOT, "05_management", "CI-CD验收截图")
ASSETS = os.path.join(ROOT, "defense_ppt", "assets")
os.makedirs(ASSETS, exist_ok=True)

print("== source dir listing ==")
for f in sorted(os.listdir(SRC_DIR)):
    print(" ", f, os.path.getsize(os.path.join(SRC_DIR, f)))

def dims(path):
    with open(path, "rb") as f:
        head = f.read(32)
    if head[:8] == b"\x89PNG\r\n\x1a\n":
        w, h = struct.unpack(">II", head[16:24])
        return "png", w, h
    if head[:2] == b"\xff\xd8":  # JPEG: scan SOF marker
        with open(path, "rb") as f:
            data = f.read()
        i = 2
        while i < len(data) - 9:
            if data[i] != 0xFF:
                i += 1; continue
            marker = data[i+1]
            if marker in (0xC0, 0xC1, 0xC2, 0xC3):
                h, w = struct.unpack(">HH", data[i+5:i+9])
                return "jpg", w, h
            seg = struct.unpack(">H", data[i+2:i+4])[0]
            i += 2 + seg
        return "jpg", 0, 0
    return "unknown", 0, 0

sources = {
    "arch_overall": r"document\详细设计说明\01_系统总体架构图.png",
    "svc_split":    r"document\详细设计说明\26_TravelMate目标服务划分图.png",
    "svc_compensate": r"document\详细设计说明\27_跨服务调用与失败补偿图.png",
    "cicd_topology": r"document\详细设计说明\28_容器化与CI-CD流水线拓扑图.png",
    "ci_overview":  r"05_management\CI-CD验收截图\01-流水线总览成功.png",
    "ci_build":     r"05_management\CI-CD验收截图\02-Docker镜像构建与Trivy扫描成功.png",
    "ci_k8s":       r"05_management\CI-CD验收截图\03-Kubernetes部署与健康检查成功.png",
    "ci_artifact":  r"05_management\CI-CD验收截图\04-流水线Artifacts验收证据.png",
    "ci_pr":        r"05_management\CI-CD验收截图\05-PR216已合并.png",
    "ci_registry":  r"05_management\CI-CD验收截图\06-GHCR前后端镜像包.png",
    "ci_workload":  r"05_management\CI-CD验收截图\07-Kubernetes工作负载与Pod状态.png",
    "ci_health":    r"05_management\CI-CD验收截图\08-前后端健康检查.png",
}
print("== copied assets ==")
for name, rel in sources.items():
    src = os.path.join(ROOT, rel)
    fmt, w, h = dims(src)
    dst = os.path.join(ASSETS, name + "." + fmt)
    shutil.copy2(src, dst)
    print(f"{name+'.'+fmt:20s} {w}x{h}  ratio={w/h:.3f}" if h else f"{name}: dims fail")
