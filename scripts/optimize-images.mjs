import { mkdir, readdir, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const sourceRoot = path.join(projectRoot, "frontend", "public", "images", "real");
const outputRoot = path.join(projectRoot, "frontend", "public", "images", "generated");
const widths = [480, 960];
const rasterPattern = /\.(?:jpe?g|png|gif|webp)$/i;

const walk = async (directory) => {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(
    entries.map((entry) => {
      const entryPath = path.join(directory, entry.name);
      return entry.isDirectory() ? walk(entryPath) : [entryPath];
    }),
  );
  return nested.flat();
};

const files = (await walk(sourceRoot)).filter((file) => rasterPattern.test(file));
let sourceBytes = 0;
let generatedBytes = 0;

for (const file of files) {
  const relative = path.relative(sourceRoot, file);
  const extension = path.extname(relative);
  const stem = relative.slice(0, -extension.length);
  sourceBytes += (await stat(file)).size;

  for (const width of widths) {
    const destination = path.join(outputRoot, `${stem}-${width}.webp`);
    await mkdir(path.dirname(destination), { recursive: true });
    await sharp(file, { animated: false })
      .rotate()
      .resize({ width, withoutEnlargement: false, fit: "inside" })
      .webp({ quality: 78, effort: 5, smartSubsample: true })
      .toFile(destination);
    generatedBytes += (await stat(destination)).size;
  }
}

const mb = (bytes) => `${(bytes / 1024 / 1024).toFixed(2)} MB`;
console.log(
  `Generated ${files.length * widths.length} responsive images from ${files.length} sources: ${mb(sourceBytes)} -> ${mb(generatedBytes)}`,
);
