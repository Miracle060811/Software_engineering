const seedAsset = (file) => `${import.meta.env.BASE_URL}images/seed/${file}`;
const apiBaseURL = import.meta.env.VITE_API_BASE_URL?.trim().replace(/\/+$/, "") || "";

export const FALLBACK_IMAGE = seedAsset("fallback.svg");

const stripWrappingQuotes = (value) => String(value || "").trim().replace(/^['"]|['"]$/g, "");

const toBackendAssetUrl = (path) => {
  if (apiBaseURL && /^https?:\/\//i.test(apiBaseURL)) {
    return `${apiBaseURL}${path}`;
  }
  return path;
};

const isInvalidSeedImage = (url) => {
  const value = String(url || "").replace(/\\/g, "/");
  return value.includes("images/seed/") && !value.endsWith("images/seed/fallback.svg");
};

export const normalizeImageUrl = (url, fallback = FALLBACK_IMAGE) => {
  const raw = stripWrappingQuotes(url).replace(/\\/g, "/");
  if (!raw || isInvalidSeedImage(raw)) return fallback || FALLBACK_IMAGE;
  if (raw.startsWith("https://commons.wikimedia.org/wiki/File:")) {
    return raw.replace(
      "https://commons.wikimedia.org/wiki/File:",
      "https://commons.wikimedia.org/wiki/Special:Redirect/file/",
    );
  }
  if (/^(https?:)?\/\//i.test(raw) || /^(data|blob):/i.test(raw)) return raw;
  if (raw.startsWith("/uploads/")) return toBackendAssetUrl(raw);
  if (raw.startsWith("uploads/")) return toBackendAssetUrl(`/${raw}`);
  if (raw.startsWith("/images/")) return raw;
  if (raw.startsWith("images/")) return `${import.meta.env.BASE_URL}${raw}`;
  if (raw.startsWith("/")) return raw;
  return raw;
};

export const parseImageList = (images, fallback = "") => {
  if (!images) return fallback ? [normalizeImageUrl(fallback)] : [];
  if (Array.isArray(images)) {
    return images.map((item) => normalizeImageUrl(item, fallback)).filter(Boolean);
  }

  const text = String(images).trim();
  if (!text) return fallback ? [normalizeImageUrl(fallback)] : [];

  if (text.startsWith("[") || text.startsWith("{")) {
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => normalizeImageUrl(item, fallback)).filter(Boolean);
      }
      if (parsed && typeof parsed === "object") {
        return Object.values(parsed).map((item) => normalizeImageUrl(item, fallback)).filter(Boolean);
      }
    } catch (e) {
      // Legacy rows can contain malformed JSON-like image strings; split them below.
    }
  }

  return text
    .replace(/^\[|\]$/g, "")
    .split(/[,;\n]+/)
    .map((item) => normalizeImageUrl(item, fallback))
    .filter(Boolean);
};
