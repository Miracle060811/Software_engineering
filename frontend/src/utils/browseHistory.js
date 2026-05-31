const HISTORY_KEY = "travelmate_browse_history";
const MAX_HISTORY = 8;

export const addBrowseHistory = (item) => {
  if (!item?.type || !item?.id || !item?.title || !item?.path) return;
  const current = getBrowseHistory().filter(
    (record) => !(record.type === item.type && String(record.id) === String(item.id)),
  );
  const next = [{ ...item, visitedAt: Date.now() }, ...current].slice(0, MAX_HISTORY);
  localStorage.setItem(HISTORY_KEY, JSON.stringify(next));
};

export const getBrowseHistory = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    return [];
  }
};
