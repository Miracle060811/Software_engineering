import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const files = [
  "docs/sql/init.sql",
  "frontend/src/data/destinations.js",
  "frontend/src/views/Home.vue",
  "frontend/src/views/destination/DestinationList.vue",
  "frontend/src/views/hotel/AttractionList.vue",
  "frontend/src/views/hotel/HotelSearch.vue",
  "frontend/src/views/hotel/HotelDetail.vue",
];

const externalImagePattern =
  /https?:\/\/(?:[^'"\s,)]+\.)?(?:picsum\.photos|randomuser\.me|upload\.wikimedia\.org|commons\.wikimedia\.org\/wiki\/Special:FilePath)[^'"\s,)]+/g;
const localImagePattern = /\/images\/seed\/[A-Za-z0-9._-]+\.(?:svg|png|jpg|jpeg|webp)/g;

let hasError = false;

for (const file of files) {
  const fullPath = path.join(root, file);
  if (!fs.existsSync(fullPath)) continue;

  const text = fs.readFileSync(fullPath, "utf8");
  const external = [...text.matchAll(externalImagePattern)].map((match) => match[0]);
  if (external.length > 0) {
    hasError = true;
    console.error(`[external] ${file}`);
    for (const url of external) console.error(`  ${url}`);
  }

  const localImages = new Set([...text.matchAll(localImagePattern)].map((match) => match[0]));
  for (const image of localImages) {
    const assetPath = path.join(root, "frontend", "public", image);
    if (!fs.existsSync(assetPath)) {
      hasError = true;
      console.error(`[missing] ${file}: ${image}`);
    }
  }
}

if (hasError) {
  process.exitCode = 1;
} else {
  console.log("图片链接检查通过：未发现高风险外链，seed 图片文件均存在。");
}
