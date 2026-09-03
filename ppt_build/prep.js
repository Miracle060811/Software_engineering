const sharp = require('C:/Users/28603/AppData/Roaming/npm/node_modules/sharp');
const fs = require('fs');
const path = require('path');
const root = 'E:/SoftEngneeringHomework/Software_engineering';
const out = path.join(root, 'ppt_build/assets');
fs.mkdirSync(out, { recursive: true });

const jobs = [
  ['document/详细设计说明/01_系统总体架构图.png'],
  ['document/详细设计说明/05_AI行程生成流程图.png'],
  ['document/详细设计说明/06_库存防超卖流程图.png'],
  ['document/详细设计说明/09_社区内容审核流程图.png'],
  ['document/详细设计说明/28_容器化与CI-CD流水线拓扑图.png'],
  ['document/中期验收/TravelMate服务划分图.svg', 'svc.png'],
  ['05_management/CI-CD验收截图/01-流水线总览成功.png'],
  ['05_management/CI-CD验收截图/03-Kubernetes部署与健康检查成功.png'],
  ['05_management/CI-CD验收截图/02-Docker镜像构建与Trivy扫描成功.png'],
];

(async () => {
  for (const [rel, name] of jobs) {
    const src = path.join(root, rel);
    const base = path.basename(rel).replace(/\.(png|svg)$/i, '') ;
    const dest = path.join(out, name || base + '.png');
    let img = sharp(src, { density: 150 });
    if (rel.endsWith('.svg')) img = img.resize({ width: 2000 });
    const info = await img.png().toFile(dest);
    console.log(path.basename(dest), info.width, 'x', info.height);
  }
})();
