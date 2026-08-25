const DEFAULT_MAX_BYTES = 10 * 1024 * 1024;
const DEFAULT_MAX_DIMENSION = 1600;
const DEFAULT_QUALITY = 0.82;
const SUPPORTED_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif"]);

const loadBitmap = async (file) => {
  if ("createImageBitmap" in window) return createImageBitmap(file);
  const objectUrl = URL.createObjectURL(file);
  try {
    const image = new Image();
    image.decoding = "async";
    image.src = objectUrl;
    await image.decode();
    return image;
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
};

const canvasToBlob = (canvas, type, quality) =>
  new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error("图片压缩失败"))),
      type,
      quality,
    );
  });

export const optimizeImageForUpload = async (
  file,
  {
    maxBytes = DEFAULT_MAX_BYTES,
    maxDimension = DEFAULT_MAX_DIMENSION,
    quality = DEFAULT_QUALITY,
  } = {},
) => {
  if (!file?.type?.startsWith("image/") || !SUPPORTED_TYPES.has(file.type)) {
    throw new Error("仅支持 JPG、PNG、WebP 或 GIF 图片");
  }
  if (file.size > maxBytes) throw new Error("图片大小不能超过 10MB");

  // 保留 GIF 动画；超过 2MB 时拒绝，避免把大动画原文件直接送到列表页。
  if (file.type === "image/gif") {
    if (file.size > 2 * 1024 * 1024) throw new Error("GIF 图片不能超过 2MB");
    return file;
  }

  let bitmap;
  try {
    bitmap = await loadBitmap(file);
    const sourceWidth = bitmap.width || bitmap.naturalWidth;
    const sourceHeight = bitmap.height || bitmap.naturalHeight;
    if (!sourceWidth || !sourceHeight) throw new Error("无法读取图片尺寸");

    const scale = Math.min(1, maxDimension / Math.max(sourceWidth, sourceHeight));
    const width = Math.max(1, Math.round(sourceWidth * scale));
    const height = Math.max(1, Math.round(sourceHeight * scale));
    if (scale === 1 && file.type === "image/webp" && file.size <= 500 * 1024) return file;

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext("2d", { alpha: true });
    context.imageSmoothingEnabled = true;
    context.imageSmoothingQuality = "high";
    context.drawImage(bitmap, 0, 0, width, height);

    const blob = await canvasToBlob(canvas, "image/webp", quality);
    if (scale === 1 && blob.size >= file.size) return file;
    const baseName = (file.name || "image").replace(/\.[^.]+$/, "");
    const outputType = blob.type || "image/png";
    const outputExtension = {
      "image/webp": "webp",
      "image/jpeg": "jpg",
      "image/png": "png",
    }[outputType] || "png";
    return new File([blob], `${baseName}.${outputExtension}`, {
      type: outputType,
      lastModified: Date.now(),
    });
  } catch (error) {
    if (error instanceof Error) throw error;
    throw new Error("图片解码或压缩失败");
  } finally {
    bitmap?.close?.();
  }
};
