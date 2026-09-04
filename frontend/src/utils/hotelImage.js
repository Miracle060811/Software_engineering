const DEFAULT_HOTEL_IMAGE = "/images/editorial/coffee-card-v82.jpg";

const CITY_HOTEL_IMAGES = Object.freeze({
  北京: ["/images/real/hotels/beijing-manxin.webp", "/images/real/hotels/beijing-hyatt-wangjing.jpg", "/images/real/hotels/qianmen-jianguo.webp"],
  上海: ["/images/real/hotels/shanghai-bund-atour.webp", "/images/real/hotels/shanghai-hengshan-garden.webp", "/images/real/hotels/shanghai-treasury.webp"],
  成都: ["/images/real/hotels/chengdu-orange-crystal.jpg", "/images/real/hotels/chengdu-east-hampton.jpg"],
  重庆: ["/images/real/hotels/chongqing-hongyadong-manxin.jpg", "/images/real/hotels/chongqing-raffles-ascott.jpg"],
  广州: ["/images/real/hotels/guangzhou-elegant.webp", "/images/real/hotels/guangzhou-hampton.webp"],
  杭州: ["/images/real/hotels/hangzhou-atour.webp", "/images/real/hotels/hangzhou-junting-hubin.jpg"],
  南京: ["/images/real/hotels/nanjing-fuzimiao-atour.jpg"],
  青岛: ["/images/real/hotels/qingdao-atour-seaview.jpg", "/images/real/hotels/qingdao-badaguan-seaview.jpg"],
  三亚: ["/images/real/hotels/sanya-haitang-resort.webp"],
  苏州: ["/images/real/hotels/suzhou-pingjiang-mercure.jpg"],
  厦门: ["/images/real/hotels/xiamen-gulangyu-seaview.jpg", "/images/real/hotels/xiamen-mercure-seaview.jpg"],
  西安: ["/images/real/hotels/xian-atour.webp", "/images/real/hotels/xian-mercure.webp"],
});

const stableIndex = (hotel, length) => {
  const source = String(hotel?.id ?? hotel?.name ?? hotel?.city ?? "");
  let hash = 0;
  for (const character of source) hash = (hash * 31 + character.charCodeAt(0)) >>> 0;
  return length > 0 ? hash % length : 0;
};

export const getHotelImageFallback = (hotel) => {
  const candidates = CITY_HOTEL_IMAGES[String(hotel?.city || "").trim()];
  if (!candidates?.length) return DEFAULT_HOTEL_IMAGE;
  return candidates[stableIndex(hotel, candidates.length)];
};

export const resolveHotelCover = (hotel) => {
  const configured = String(hotel?.coverImg || "").trim();
  return configured || getHotelImageFallback(hotel);
};
