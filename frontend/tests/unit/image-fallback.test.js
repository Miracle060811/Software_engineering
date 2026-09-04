import { describe, expect, it } from "vitest";

import { normalizeImageUrl } from "@/utils/image";
import { getHotelImageFallback, resolveHotelCover } from "@/utils/hotelImage";

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
});
