const seedAsset = (file) => `${import.meta.env.BASE_URL}images/seed/${file}`;
const generatedAsset = (file) => `${import.meta.env.BASE_URL}images/generated/${file}`;
const apiBaseURL = import.meta.env.VITE_API_BASE_URL?.trim().replace(/\/+$/, "") || "";
const LOCAL_RESPONSIVE_WIDTHS = [480, 960];
// Wikimedia rejects hotlinked thumbnails that are not one of its production size steps.
const REMOTE_RESPONSIVE_WIDTHS = [500, 960, 1280];

export const FALLBACK_IMAGE = seedAsset("fallback.svg");

const stripWrappingQuotes = (value) => String(value || "").trim().replace(/^['"]|['"]$/g, "");

const toWikimediaThumbnailUrl = (value, width = 960) => {
  try {
    const parsed = new URL(String(value).startsWith("//") ? `https:${value}` : value);
    if (parsed.hostname === "commons.wikimedia.org") {
      if (parsed.pathname.startsWith("/wiki/File:")) {
        parsed.pathname = parsed.pathname.replace("/wiki/File:", "/wiki/Special:Redirect/file/");
      }
      if (parsed.pathname.startsWith("/wiki/Special:Redirect/file/")) {
        parsed.searchParams.set("width", String(width));
        return parsed.toString();
      }
    }

    if (parsed.hostname !== "upload.wikimedia.org") return "";
    const parts = parsed.pathname.split("/").filter(Boolean);
    if (parts[0] !== "wikipedia" || parts[1] !== "commons") return "";

    if (parts[2] === "thumb" && parts.length >= 7) {
      const fileName = parts[5];
      const renderedName = fileName.toLowerCase().endsWith(".svg")
        ? `${width}px-${fileName}.png`
        : `${width}px-${fileName}`;
      parsed.pathname = `/${parts.slice(0, 6).join("/")}/${renderedName}`;
      parsed.search = "";
      parsed.hash = "";
      return parsed.toString();
    }

    if (parts.length >= 5) {
      const [, , firstHash, secondHash, ...fileParts] = parts;
      const fileName = fileParts.join("/");
      const renderedName = fileName.toLowerCase().endsWith(".svg")
        ? `${width}px-${fileName}.png`
        : `${width}px-${fileName}`;
      parsed.pathname = `/wikipedia/commons/thumb/${firstHash}/${secondHash}/${fileName}/${renderedName}`;
      parsed.search = "";
      parsed.hash = "";
      return parsed.toString();
    }
  } catch (e) {
    return "";
  }
  return "";
};

const toBackendAssetUrl = (path) => {
  if (apiBaseURL && /^https?:\/\//i.test(apiBaseURL)) {
    return `${apiBaseURL}${path}`;
  }
  return path;
};

export const normalizeImageUrl = (url, fallback = FALLBACK_IMAGE) => {
  const raw = stripWrappingQuotes(url).replace(/\\/g, "/");
  if (!raw) return fallback || FALLBACK_IMAGE;
  const wikimediaThumbnail = toWikimediaThumbnailUrl(raw);
  if (wikimediaThumbnail) return wikimediaThumbnail;
  if (/^(https?:)?\/\//i.test(raw) || /^(data|blob):/i.test(raw)) return raw;
  if (raw.startsWith("/uploads/")) return toBackendAssetUrl(raw);
  if (raw.startsWith("uploads/")) return toBackendAssetUrl(`/${raw}`);
  if (raw.startsWith("/images/")) return raw;
  if (raw.startsWith("images/")) return `${import.meta.env.BASE_URL}${raw}`;
  if (raw.startsWith("/")) return raw;
  return raw;
};

export const getResponsiveImageData = (url, fallback = FALLBACK_IMAGE) => {
  const normalized = normalizeImageUrl(url, fallback);
  const localMatch = normalized.match(/(?:^|\/)images\/real\/(.+)\.(?:jpe?g|png|gif|webp)(?:[?#].*)?$/i);

  if (localMatch) {
    const stem = localMatch[1];
    const candidates = LOCAL_RESPONSIVE_WIDTHS.map((width) => ({
      width,
      url: generatedAsset(`${stem}-${width}.webp`),
    }));
    return {
      src: candidates.find((item) => item.width === 960)?.url || candidates[0].url,
      srcset: candidates.map((item) => `${item.url} ${item.width}w`).join(", "),
      original: normalized,
    };
  }

  const wikimediaCandidates = REMOTE_RESPONSIVE_WIDTHS
    .map((width) => ({ width, url: toWikimediaThumbnailUrl(normalized, width) }))
    .filter((item) => item.url);
  if (wikimediaCandidates.length > 0) {
    return {
      src: wikimediaCandidates.find((item) => item.width === 960)?.url || normalized,
      srcset: wikimediaCandidates.map((item) => `${item.url} ${item.width}w`).join(", "),
      original: normalized,
    };
  }

  return { src: normalized, srcset: "", original: normalized };
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
