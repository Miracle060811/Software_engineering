import { describe, expect, it } from "vitest";

import { normalizeImageUrl } from "@/utils/image";
import { getHotelImageFallback, resolveHotelCover } from "@/utils/hotelImage";
import { getAttractionImageFallback, resolveAttractionCover } from "@/utils/attractionImage";

describe("local image fallbacks", () => {
  it("replaces legacy community seed artwork with bundled photos", () => {
    expect(normalizeImageUrl("/images/seed/mountain.svg"))
      .toBe("/images/real/attractions/taishan.jpg");
    expect(normalizeImageUrl("/images/seed/beijing.svg"))
      .toBe("/images/real/posts/beijing-forbidden-city.jpg");
    expect(normalizeImageUrl("/images/seed/hotel.svg"))
      .toBe("/images/editorial/coffee-card-v82.jpg");
  });

  it("uses a deterministic bundled hotel photo when coverImg is missing", () => {
    const hotel = { id: 1, city: "北京", coverImg: null };
    expect(resolveHotelCover(hotel)).toBe(getHotelImageFallback(hotel));
    expect(resolveHotelCover(hotel)).toMatch(/^\/images\/real\/hotels\//);
  });

  it("keeps a configured hotel cover", () => {
    expect(resolveHotelCover({ city: "上海", coverImg: "/uploads/hotel.jpg" }))
      .toBe("/uploads/hotel.jpg");
  });

  it("uses the bundled attraction photo matching the attraction id", () => {
    const attraction = { id: 27, name: "天门山国家森林公园", coverImg: null };
    expect(resolveAttractionCover(attraction)).toBe("/images/real/attractions/attraction-27.webp");
    expect(getAttractionImageFallback(attraction)).toBe(resolveAttractionCover(attraction));
  });

  it("keeps a configured attraction cover and supplies a local fallback", () => {
    const attraction = { id: 48, coverImg: "https://example.com/attraction.jpg" };
    expect(resolveAttractionCover(attraction)).toBe(attraction.coverImg);
    expect(getAttractionImageFallback(attraction)).toBe("/images/real/attractions/attraction-48.webp");
  });
});
