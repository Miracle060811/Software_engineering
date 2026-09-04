import { expect, test } from "@playwright/test";

const result = (data) => ({
  code: 200,
  msg: "成功",
  data,
});

async function mockTravelMateApi(page) {
  // Playwright evaluates matching routes in reverse registration order, so keep the catch-all first.
  await page.route("**/api/**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([])),
    });
  });

  await page.route("**/user/login**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      headers: {
        "Set-Cookie": "TRAVELMATE_REFRESH=mock-refresh; HttpOnly; Path=/user; SameSite=Lax",
      },
      body: JSON.stringify(result("mock-token")),
    });
  });

  await page.route("**/user/refresh**", async (route) => {
    const hasRefreshCookie = route.request().headers().cookie?.includes("TRAVELMATE_REFRESH=mock-refresh");
    await route.fulfill({
      status: hasRefreshCookie ? 200 : 403,
      contentType: "application/json",
      body: JSON.stringify(hasRefreshCookie
        ? result("mock-refreshed-token")
        : { code: 500, msg: "登录状态已失效", data: null }),
    });
  });

  await page.route("**/user/me**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result({
        id: 1,
        username: "testuser",
        nickname: "测试用户",
        role: 0,
      })),
    });
  });

  await page.route("**/api/flight/search**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([
        {
          id: 1,
          flightNo: "CA1234",
          airline: "中国国际航空",
          departureCity: "北京",
          arrivalCity: "上海",
          departureTime: "2026-06-01 08:00:00",
          arrivalTime: "2026-06-01 10:05:00",
          economyPrice: 680,
          businessPrice: 2180,
          availableSeats: 45,
          status: 1,
        },
      ])),
    });
  });

  await page.route("**/api/train/search**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([
        {
          id: 1,
          trainNo: "G101",
          departureStation: "北京南",
          arrivalStation: "上海虹桥",
          departureTime: "2026-06-01 08:00:00",
          arrivalTime: "2026-06-01 13:30:00",
          secondSeatPrice: 553,
          secondSeatCount: 20,
          status: 1,
        },
      ])),
    });
  });

  await page.route("**/api/hotel/search**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([
        {
          id: 1,
          name: "上海外滩亚朵酒店",
          city: "上海",
          address: "上海市黄浦区",
          starRating: 4,
          minPrice: 499,
          coverImg: "/images/seed/hotel.svg",
        },
      ])),
    });
  });

  await page.route("**/api/attraction/search**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([
        {
          id: 1,
          name: "故宫博物院",
          city: "北京",
          price: 60,
          coverImg: "/images/seed/beijing.svg",
          status: 1,
        },
      ])),
    });
  });

  await page.route("**/api/post/list**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([
        {
          id: 1,
          title: "北京三日游攻略",
          content: "故宫、长城、颐和园路线记录",
          destination: "北京",
          images: "/images/real/posts/beijing-forbidden-city.jpg",
          nickname: "测试用户",
          likeCount: 12,
          commentCount: 3,
          createTime: "2026-06-01 12:00:00",
        },
        {
          id: 2,
          title: "三亚海岛路线",
          content: "蜈支洲岛与亚龙湾路线记录",
          destination: "三亚",
          images: "https://upload.wikimedia.org/wikipedia/commons/1/19/Wuzhizhou_Island_-_01.jpg",
          nickname: "测试用户",
          likeCount: 8,
          commentCount: 2,
          createTime: "2026-06-01 13:00:00",
        },
      ])),
    });
  });

  await page.route("**/api/coupon/list**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([])),
    });
  });

  await page.route("**/api/destinations**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(result([])),
    });
  });

}

test.beforeEach(async ({ page }) => {
  await mockTravelMateApi(page);
});

test("public pages render without backend dependency", async ({ page }) => {
  const publicPaths = [
    "/",
    "/login",
    "/flight-search",
    "/train-search",
    "/hotel-search",
    "/attractions",
    "/ai-plan",
    "/community",
    "/coupons",
  ];

  for (const path of publicPaths) {
    await page.goto(path);
    await expect(page.locator("body")).toBeVisible();
    await expect(page.locator("body")).not.toHaveText("");
  }
});

test("community covers use responsive generated images", async ({ page }) => {
  const imageRequests = [];
  page.on("request", (request) => {
    if (request.resourceType() === "image") imageRequests.push(request.url());
  });

  await page.goto("/community");
  const cover = page.locator(".post-cover").first();
  await expect(cover).toHaveAttribute("loading", "eager");
  await expect(cover).toHaveAttribute("fetchpriority", "high");
  await expect(cover).toHaveAttribute("decoding", "async");
  await expect(cover).toHaveAttribute("srcset", /images\/generated\/posts\/beijing-forbidden-city-480\.webp/);
  await expect.poll(() => cover.evaluate((image) => image.naturalWidth)).toBeGreaterThan(0);
  expect(imageRequests.some((url) => url.includes("/images/real/posts/beijing-forbidden-city.jpg"))).toBe(false);
});

test("hotel cards replace legacy seed artwork with a visible local photo", async ({ page }) => {
  await page.goto("/hotel-search");
  const cover = page.locator(".hotel-img").first();
  await expect(cover).toHaveAttribute("src", "/images/editorial/coffee-card-v82.jpg");
  await expect.poll(() => cover.evaluate((image) => image.naturalWidth)).toBeGreaterThan(0);
});

test("Wikimedia covers only request supported thumbnail sizes", async ({ page }) => {
  await page.route("https://upload.wikimedia.org/**", async (route) => {
    await route.fulfill({
      contentType: "image/svg+xml",
      body: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="9"><rect width="16" height="9" fill="#168f7e"/></svg>',
    });
  });

  await page.goto("/community");
  const cover = page.locator(".post-cover").nth(1);
  await expect(cover).toHaveAttribute("srcset", /500px-Wuzhizhou_Island_-_01\.jpg 500w/);
  await expect(cover).toHaveAttribute("srcset", /960px-Wuzhizhou_Island_-_01\.jpg 960w/);
  await expect(cover).toHaveAttribute("srcset", /1280px-Wuzhizhou_Island_-_01\.jpg 1280w/);
  await expect(cover).not.toHaveAttribute("srcset", /(?:480|1440)px-/);
  await expect.poll(() => cover.evaluate((image) => image.naturalWidth)).toBeGreaterThan(0);
});

test("auth-only routes redirect anonymous users to login", async ({ page }) => {
  for (const path of ["/post/create", "/my-orders", "/notifications", "/collections", "/admin"]) {
    await page.goto(path);
    await expect(page).toHaveURL(/\/login(?:\?.*)?$/);
    expect(new URL(page.url()).searchParams.get("redirect")).toBe(path);
  }
});

test("non-admin user is rejected by admin route guard", async ({ page }) => {
  await page.goto("/login");
  await page.getByPlaceholder("请输入用户名").fill("testuser");
  await page.getByPlaceholder("请输入密码").fill("test123");
  await page.getByRole("button", { name: "登 录" }).click();
  await expect(page).toHaveURL(/\/$/);

  await page.goto("/admin");
  await expect(page).toHaveURL(/\/$/);
});

test("login flow keeps access token out of localStorage and returns to home", async ({ page }) => {
  await page.goto("/login");
  await page.getByPlaceholder("请输入用户名").fill("testuser");
  await page.getByPlaceholder("请输入密码").fill("test123");
  await page.getByRole("button", { name: "登 录" }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect.poll(() => page.evaluate(() => localStorage.getItem("token"))).toBeNull();
});
