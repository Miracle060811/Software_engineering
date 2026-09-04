import { mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";

import SafeImage from "@/components/SafeImage.vue";

describe("SafeImage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("does not send the current page as the referrer by default", () => {
    const wrapper = mount(SafeImage, {
      props: {
        src: "https://bkimg.cdn.bcebos.com/pic/example",
        loading: "eager",
      },
    });

    expect(wrapper.get("img").attributes("referrerpolicy")).toBe("no-referrer");
  });

  it("keeps an explicitly configured referrer policy", () => {
    const wrapper = mount(SafeImage, {
      props: {
        src: "https://example.com/image.jpg",
        loading: "eager",
        referrerpolicy: "origin",
      },
    });

    expect(wrapper.get("img").attributes("referrerpolicy")).toBe("origin");
  });

  it("shows its fallback instead of a transparent pixel before lazy activation", () => {
    vi.stubGlobal("IntersectionObserver", class {
      observe() {}
      disconnect() {}
    });
    const wrapper = mount(SafeImage, {
      props: {
        src: "https://example.com/hotel.jpg",
        fallback: "/images/editorial/coffee-card-v82.jpg",
        loading: "lazy",
      },
    });

    expect(wrapper.get("img").attributes("src")).toBe("/images/editorial/coffee-card-v82.jpg");
  });
});
