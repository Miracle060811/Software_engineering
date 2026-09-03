import { mkdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const migrationPath = path.join(
  projectRoot,
  "backend",
  "src",
  "main",
  "resources",
  "db",
  "migration",
  "V2__image_data_update.sql",
);
const attractionRoot = path.join(projectRoot, "frontend", "public", "images", "real", "attractions");
const packagedSources = new Map([
  [20, path.join(projectRoot, "frontend", "public", "images", "real", "hotels", "sanya-haitang-resort.webp")],
  [30, path.join(attractionRoot, "potala.jpg")],
  [31, path.join(attractionRoot, "tianshan-tianchi.jpg")],
  [35, path.join(attractionRoot, "taishan.jpg")],
  [36, path.join(attractionRoot, "tianshan-tianchi.jpg")],
  [41, path.join(attractionRoot, "wuyi-mountain.jpg")],
  [42, path.join(attractionRoot, "zhouzhuang.jpg")],
]);

const migration = await readFile(migrationPath, "utf8");
const remoteById = new Map(
  [...migration.matchAll(/SET cover_img = '([^']+)' WHERE id = (\d+);/g)].map((match) => [
    Number(match[2]),
    match[1],
  ]),
);

if (remoteById.size !== 48) {
  throw new Error(`Expected 48 attraction sources in V2, found ${remoteById.size}`);
}

await mkdir(attractionRoot, { recursive: true });

for (const id of [...remoteById.keys()].sort((left, right) => left - right)) {
  const localSource = packagedSources.get(id);
  let input;
  if (localSource) {
    input = localSource;
  } else {
    const response = await fetch(remoteById.get(id), {
      headers: { "User-Agent": "TravelMate image localization" },
      redirect: "follow",
    });
    if (!response.ok) throw new Error(`Attraction ${id}: ${response.status} ${response.statusText}`);
    input = Buffer.from(await response.arrayBuffer());
  }

  const fileName = `attraction-${String(id).padStart(2, "0")}.webp`;
  await sharp(input, { animated: false })
    .rotate()
    .resize({ width: 1600, withoutEnlargement: true, fit: "inside" })
    .webp({ quality: 84, effort: 5, smartSubsample: true })
    .toFile(path.join(attractionRoot, fileName));
  console.log(`Localized attraction ${id}: ${fileName}`);
}
