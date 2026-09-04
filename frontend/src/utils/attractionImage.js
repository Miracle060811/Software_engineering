const ATTRACTION_IMAGE_COUNT = 48;
const DEFAULT_ATTRACTION_IMAGE = "/images/editorial/guilin-cinematic-v82.jpg";

const bundledAttractionImage = (id) => {
  const numericId = Number(id);
  if (!Number.isInteger(numericId) || numericId < 1 || numericId > ATTRACTION_IMAGE_COUNT) {
    return DEFAULT_ATTRACTION_IMAGE;
  }
  return `/images/real/attractions/attraction-${String(numericId).padStart(2, "0")}.webp`;
};

export const getAttractionImageFallback = (attraction) =>
  bundledAttractionImage(attraction?.id);

export const resolveAttractionCover = (attraction) => {
  const configured = String(attraction?.coverImg || "").trim();
  return configured || getAttractionImageFallback(attraction);
};
