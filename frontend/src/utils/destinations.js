import request from "@/utils/request";
import { destinations as staticDestinations } from "@/data/destinations";

const splitList = (value) => {
  if (Array.isArray(value)) {
    return value;
  }
  return String(value || "")
    .split(/\r?\n|\||,|，|、/)
    .map((item) => item.trim())
    .filter(Boolean);
};

export const normalizeDestination = (item = {}) => ({
  ...item,
  country: item.country || "中国",
  keywords: splitList(item.keywords),
  highlights: splitList(item.highlights),
});

export const fallbackDestinations = staticDestinations.map(normalizeDestination);

export const fetchDestinationList = async () => {
  try {
    const data = await request.get("/api/destinations", { skipErrorMessage: true });
    const list = Array.isArray(data) ? data.map(normalizeDestination) : [];
    return list.length ? list : fallbackDestinations;
  } catch (error) {
    return fallbackDestinations;
  }
};

export const fetchDestinationBySlug = async (slug) => {
  try {
    const data = await request.get(`/api/destinations/${slug}`, { skipErrorMessage: true });
    return data ? normalizeDestination(data) : null;
  } catch (error) {
    return fallbackDestinations.find((item) => item.slug === slug) || null;
  }
};
