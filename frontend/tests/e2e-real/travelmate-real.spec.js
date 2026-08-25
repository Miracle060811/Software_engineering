import { expect, test } from "@playwright/test";

async function registerAndLogin(request) {
  const username = `ci${Date.now()}${Math.floor(Math.random() * 1000)}`;
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
  return { username, password, token: loginBody.data };
}

test("UC01 registers and logs in against the real backend", async ({ page, request }) => {
  const { username, password } = await registerAndLogin(request);

  await page.goto("/login");
  await page.getByPlaceholder("请输入用户名").fill(username);
  await page.getByPlaceholder("请输入密码").fill(password);
  await page.getByRole("button", { name: "登 录" }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect.poll(() => page.evaluate(() => localStorage.getItem("token"))).not.toBeNull();
});

test("representative use-case APIs return real database-backed contracts", async ({ request }) => {
  const { token } = await registerAndLogin(request);
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
      headers: { Authorization: `Bearer ${token}` },
    } : undefined);
    expect(response.ok(), `${useCase} ${url}`).toBeTruthy();
    const body = await response.json();
    expect(body.code, useCase).toBe(200);
    expect(body.data, useCase).not.toBeNull();
  }
});

test("UC02 flight search renders results from the real backend", async ({ page }) => {
  await page.goto("/flight-search");
  await page.getByPlaceholder("如：北京").fill("北京");
  await page.getByPlaceholder("如：上海").fill("上海");
  await page.getByRole("button", { name: /搜索航班/ }).click();
  await expect(page.locator(".flight-card").first()).toBeVisible();
});
