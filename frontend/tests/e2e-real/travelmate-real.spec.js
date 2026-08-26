import { expect, test } from "@playwright/test";
import { randomUUID } from "node:crypto";

async function registerAndLogin(request) {
  const username = `ci${Date.now()}${randomUUID().replaceAll("-", "").slice(0, 12)}`;
  const password = "Travel123456";
  const registerResponse = await request.post("/user/register", {
    form: { username, password },
  });
  expect(registerResponse.ok()).toBeTruthy();
  expect((await registerResponse.json()).code).toBe(200);

  const loginResponse = await request.post("/user/login", {
    form: { username, password },
  });
  expect(loginResponse.ok()).toBeTruthy();
  const loginBody = await loginResponse.json();
  expect(loginBody.code).toBe(200);
  const storage = await request.storageState();
  const csrfToken = storage.cookies.find((cookie) => cookie.name === "XSRF-TOKEN")?.value;
  expect(csrfToken).toBeTruthy();
  return { username, password, token: loginBody.data, csrfToken };
}

const authenticatedHeaders = ({ token, csrfToken }) => ({
  Authorization: `Bearer ${token}`,
  "X-XSRF-TOKEN": csrfToken,
});

async function deleteAccount(request, session) {
  const response = await request.delete(`/user/account?password=${encodeURIComponent(session.password)}`, {
    headers: authenticatedHeaders(session),
  });
  expect(response.ok()).toBeTruthy();
  expect((await response.json()).code).toBe(200);
}

test("UC01 registers and logs in against the real backend", async ({ page, request }) => {
  const session = await registerAndLogin(request);

  try {
    await page.goto("/login");
    await page.getByPlaceholder("请输入用户名").fill(session.username);
    await page.getByPlaceholder("请输入密码").fill(session.password);
    await page.getByRole("button", { name: "登 录" }).click();

    await expect(page).toHaveURL(/\/$/);
    await expect.poll(() => page.evaluate(() => localStorage.getItem("token"))).not.toBeNull();
  } finally {
    await deleteAccount(request, session);
  }
});

test("representative use-case APIs return real database-backed contracts", async ({ request }) => {
  const session = await registerAndLogin(request);
  try {
    const cases = [
      ["UC02", "/api/flight/search?depCity=北京&arrCity=上海", false],
      ["UC03", "/api/train/search?depStation=北京南&arrStation=上海虹桥", false],
      ["UC05", "/api/hotel/search?city=上海", false],
      ["UC07", "/api/attraction/search?city=北京", false],
      ["UC08", "/api/tour/list", true],
      ["UC10", "/api/coupon/list", false],
      ["UC14", "/api/post/list?page=1&size=5", false],
    ];

    for (const [useCase, url, authenticated] of cases) {
      const response = await request.get(url, authenticated ? {
        headers: authenticatedHeaders(session),
      } : undefined);
      expect(response.ok(), `${useCase} ${url}`).toBeTruthy();
      const body = await response.json();
      expect(body.code, useCase).toBe(200);
      expect(Array.isArray(body.data), useCase).toBeTruthy();
      expect(body.data.length, useCase).toBeGreaterThan(0);
    }
  } finally {
    await deleteAccount(request, session);
  }
});

test("UC04 creates and cancels a flight order against real services", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  let orderNo;

  try {
    const passengerResponse = await request.post("/api/passenger/add", {
      headers,
      data: {
        name: "CI 乘客",
        idCard: "110101199001011234",
        phone: "13800138000",
      },
    });
    expect(passengerResponse.ok()).toBeTruthy();
    expect((await passengerResponse.json()).code).toBe(200);

    const passengersResponse = await request.get("/api/passenger/list", { headers });
    const passengersBody = await passengersResponse.json();
    expect(passengersBody.code).toBe(200);
    expect(passengersBody.data.length).toBeGreaterThan(0);

    const flightsResponse = await request.get("/api/flight/search?depCity=北京&arrCity=上海");
    const flightsBody = await flightsResponse.json();
    expect(flightsBody.code).toBe(200);
    expect(flightsBody.data.length).toBeGreaterThan(0);

    const createResponse = await request.post("/api/order/flight/create", {
      headers,
      data: {
        flightId: flightsBody.data[0].id,
        passengerId: passengersBody.data[0].id,
        seatType: "Economy",
        ticketCount: 1,
        userCouponId: null,
      },
    });
    const createBody = await createResponse.json();
    expect(createResponse.ok()).toBeTruthy();
    expect(createBody.code).toBe(200);
    expect(createBody.data).toMatch(/^T/);
    orderNo = createBody.data;

    const ordersResponse = await request.get("/api/order/list", { headers });
    const ordersBody = await ordersResponse.json();
    expect(ordersBody.code).toBe(200);
    expect(ordersBody.data.some((order) => order.orderNo === orderNo)).toBeTruthy();
  } finally {
    if (orderNo) {
      const cancelResponse = await request.post(`/api/order/${orderNo}/cancel`, { headers });
      expect(cancelResponse.ok()).toBeTruthy();
      expect((await cancelResponse.json()).code).toBe(200);
    }
    await deleteAccount(request, session);
  }
});

test("UC02 flight search renders results from the real backend", async ({ page }) => {
  await page.goto("/flight-search");
  await page.getByPlaceholder("如：北京").fill("北京");
  await page.getByPlaceholder("如：上海").fill("上海");
  await page.getByRole("button", { name: /搜索航班/ }).click();
  await expect(page.locator(".flight-card").first()).toBeVisible();
});
