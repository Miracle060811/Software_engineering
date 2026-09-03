import { access, readdir, readFile, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const publicRoot = path.join(projectRoot, "frontend", "public");
const sourceRoot = path.join(publicRoot, "images", "real");
const generatedRoot = path.join(publicRoot, "images", "generated");
const scanRoots = [
  path.join(projectRoot, "frontend", "src"),
  path.join(projectRoot, "docs", "sql"),
];
const widths = [480, 960];
const generatedBudget = 350 * 1024;
const rasterPattern = /\.(?:jpe?g|png|gif|webp)$/i;
const localReferencePattern = /\/images\/real\/([^'"\s,)]+\.(?:jpe?g|png|gif|webp))/gi;
const remoteImagePattern = /https?:\/\/[^'"\s,)]+(?:\.(?:jpe?g|png|gif|webp|svg)(?:\?[^'"\s,)]*)?|images\.unsplash\.com\/[^'"\s,)]+)/gi;
const checkRemote = process.argv.includes("--remote");
const errors = [];
const warnings = [];

const walk = async (directory) => {
  try {
    const entries = await readdir(directory, { withFileTypes: true });
    const nested = await Promise.all(
      entries.map((entry) => {
        const entryPath = path.join(directory, entry.name);
        return entry.isDirectory() ? walk(entryPath) : [entryPath];
      }),
    );
    return nested.flat();
  } catch (error) {
    if (error.code === "ENOENT") return [];
    throw error;
  }
};

const sourceFiles = (await walk(sourceRoot)).filter((file) => rasterPattern.test(file));
for (let id = 1; id <= 48; id += 1) {
  const fileName = `attraction-${String(id).padStart(2, "0")}.webp`;
  try {
    await access(path.join(sourceRoot, "attractions", fileName));
  } catch {
    errors.push(`Missing packaged attraction image: ${fileName}`);
  }
}

for (const sourceFile of sourceFiles) {
  const relative = path.relative(sourceRoot, sourceFile);
  const extension = path.extname(relative);
  const stem = relative.slice(0, -extension.length);
  for (const width of widths) {
    const generated = path.join(generatedRoot, `${stem}-${width}.webp`);
    try {
      await access(generated);
      const info = await stat(generated);
      const metadata = await sharp(generated).metadata();
      if (metadata.format !== "webp") errors.push(`${generated}: expected WebP output`);
      if (metadata.width !== width) errors.push(`${generated}: expected width ${width}, got ${metadata.width}`);
      if (info.size > generatedBudget) {
        errors.push(`${generated}: ${(info.size / 1024).toFixed(0)} KB exceeds 350 KB budget`);
      }
    } catch (error) {
      errors.push(`${generated}: missing responsive variant (${error.message})`);
    }
  }
}

const scannedFiles = (await Promise.all(scanRoots.map(walk))).flat();
const remoteUrls = new Set();
for (const file of scannedFiles) {
  if (!/\.(?:vue|js|sql|md|html|css)$/i.test(file)) continue;
  const content = await readFile(file, "utf8");
  for (const match of content.matchAll(localReferencePattern)) {
    const localFile = path.join(publicRoot, "images", "real", match[1]);
    try {
      await access(localFile);
    } catch {
      errors.push(`${file}: missing local image /images/real/${match[1]}`);
    }
  }
  for (const match of content.matchAll(remoteImagePattern)) remoteUrls.add(match[0]);
}

if (remoteUrls.size > 0) {
  warnings.push(`${remoteUrls.size} remote image URLs remain; Wikimedia originals are resized at runtime`);
}

if (checkRemote) {
  const queue = [...remoteUrls];
  const workers = Array.from({ length: 4 }, async () => {
    while (queue.length > 0) {
      const url = queue.shift();
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 8000);
      try {
        const response = await fetch(url, { method: "HEAD", redirect: "follow", signal: controller.signal });
        const type = response.headers.get("content-type") || "";
        if (!response.ok || (type && !type.startsWith("image/"))) {
          errors.push(`${url}: ${response.status} ${type || "unknown content type"}`);
        }
      } catch (error) {
        errors.push(`${url}: ${error.message}`);
      } finally {
        clearTimeout(timeout);
      }
    }
  });
  await Promise.all(workers);
}

for (const warning of warnings) console.warn(`WARN ${warning}`);
if (errors.length > 0) {
  for (const error of errors) console.error(`ERROR ${error}`);
  process.exitCode = 1;
} else {
  console.log(
    `Image check passed: ${sourceFiles.length} sources, ${sourceFiles.length * widths.length} generated variants, ${remoteUrls.size} remote references`,
  );
}
