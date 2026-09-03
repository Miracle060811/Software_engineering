import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";

import SafeImage from "@/components/SafeImage.vue";

describe("SafeImage", () => {
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
});
