import { expect, test } from "@playwright/test";
import { randomUUID } from "node:crypto";

async function login(request, username, password) {
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

async function registerAndLogin(request) {
  const username = `ci${Date.now()}${randomUUID().replaceAll("-", "").slice(0, 12)}`;
  const password = "Travel123456";
  const registerResponse = await request.post("/user/register", {
    form: { username, password },
  });
  expect(registerResponse.ok()).toBeTruthy();
  expect((await registerResponse.json()).code).toBe(200);
  return login(request, username, password);
}

async function getUserId(request, username) {
  const response = await request.get(`/api/user/profile/${encodeURIComponent(username)}`);
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.code).toBe(200);
  return body.data.userId;
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

test("UC09 submits, lists and reports a review against the real backend", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  const marker = `UC09-${randomUUID()}`;

  try {
    const addResponse = await request.post("/api/review/add", {
      headers,
      data: { targetId: 1, targetType: 0, rating: 5, content: `  ${marker}  ` },
    });
    expect(addResponse.ok()).toBeTruthy();
    expect((await addResponse.json()).code).toBe(200);

    const listResponse = await request.get("/api/review/list?targetId=1&targetType=0");
    const listBody = await listResponse.json();
    expect(listBody.code).toBe(200);
    const review = listBody.data.find((item) => item.content === marker);
    expect(review).toBeTruthy();

    const reportResponse = await request.post("/api/review/report", {
      headers,
      data: { reviewId: review.id, reason: "CI 自动化举报验证" },
    });
    expect(reportResponse.ok()).toBeTruthy();
    expect((await reportResponse.json()).code).toBe(200);

    const duplicateResponse = await request.post("/api/review/report", {
      headers,
      data: { reviewId: review.id, reason: "重复举报" },
    });
    const duplicateBody = await duplicateResponse.json();
    expect(duplicateBody.code).toBe(500);
    expect(duplicateBody.msg).toContain("已举报过");
  } finally {
    await deleteAccount(request, session);
  }
});

test("UC13 sends and reads a private message against the real backend", async ({ request }) => {
  const sender = await registerAndLogin(request);
  const receiver = await registerAndLogin(request);
  const senderId = await getUserId(request, sender.username);
  const receiverId = await getUserId(request, receiver.username);
  const marker = `UC13-${randomUUID()}`;

  try {
    const activeSender = await login(request, sender.username, sender.password);
    const sendResponse = await request.post("/api/private-message/send", {
      headers: authenticatedHeaders(activeSender),
      data: { receiverId, content: `  ${marker}  ` },
    });
    const sendBody = await sendResponse.json();
    expect(sendResponse.ok()).toBeTruthy();
    expect(sendBody.code).toBe(200);
    expect(sendBody.data.content).toBe(marker);

    const activeReceiver = await login(request, receiver.username, receiver.password);
    const unreadResponse = await request.get("/api/private-message/unread-count", {
      headers: authenticatedHeaders(activeReceiver),
    });
    expect((await unreadResponse.json()).data).toBeGreaterThanOrEqual(1);

    const conversationResponse = await request.get(`/api/private-message/conversation/${senderId}`, {
      headers: authenticatedHeaders(activeReceiver),
    });
    const conversationBody = await conversationResponse.json();
    expect(conversationBody.code).toBe(200);
    expect(conversationBody.data.some((message) => message.content === marker)).toBeTruthy();

    const afterReadResponse = await request.get("/api/private-message/unread-count", {
      headers: authenticatedHeaders(activeReceiver),
    });
    expect((await afterReadResponse.json()).data).toBe(0);
  } finally {
    const activeReceiver = await login(request, receiver.username, receiver.password);
    await deleteAccount(request, activeReceiver);
    const activeSender = await login(request, sender.username, sender.password);
    await deleteAccount(request, activeSender);
  }
});

test("UC15 likes, collects and comments on a post against the real backend", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  let postId;
  let commentId;
  let liked = false;
  let collected = false;

  try {
    const postsResponse = await request.get("/api/post/list?page=1&size=5");
    const postsBody = await postsResponse.json();
    expect(postsBody.code).toBe(200);
    postId = postsBody.data[0].id;

    const likeResponse = await request.post("/api/like/toggle", {
      headers,
      data: { targetId: postId, targetType: 0 },
    });
    expect((await likeResponse.json()).data.liked).toBeTruthy();
    liked = true;

    const collectResponse = await request.post("/api/like/toggle", {
      headers,
      data: { targetId: postId, targetType: 2 },
    });
    expect((await collectResponse.json()).data.liked).toBeTruthy();
    collected = true;

    const marker = `UC15-${randomUUID()}`;
    const commentResponse = await request.post("/api/comment/add", {
      headers,
      data: { postId, content: marker },
    });
    const commentBody = await commentResponse.json();
    expect(commentBody.code).toBe(200);
    expect(commentBody.data.content).toBe(marker);
    commentId = commentBody.data.id;

    const commentsResponse = await request.get(`/api/comment/list?postId=${postId}`);
    const commentsBody = await commentsResponse.json();
    expect(commentsBody.data.some((comment) => comment.id === commentId)).toBeTruthy();
  } finally {
    if (commentId) {
      const response = await request.delete(`/api/comment/${commentId}`, { headers });
      expect((await response.json()).code).toBe(200);
    }
    if (liked) {
      await request.post("/api/like/toggle", { headers, data: { targetId: postId, targetType: 0 } });
    }
    if (collected) {
      await request.post("/api/like/toggle", { headers, data: { targetId: postId, targetType: 2 } });
    }
    await deleteAccount(request, session);
  }
});

test("UC16 adds, validates and deletes a passenger against the real backend", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  const idCard = `P${randomUUID().replaceAll("-", "").slice(0, 11).toUpperCase()}`;
  let passengerId;

  try {
    const passenger = { name: "  CI 乘客  ", idCard, phone: "13800138000", type: 1 };
    const addResponse = await request.post("/api/passenger/add", { headers, data: passenger });
    expect((await addResponse.json()).code).toBe(200);

    const listResponse = await request.get("/api/passenger/list", { headers });
    const listBody = await listResponse.json();
    const created = listBody.data.find((item) => item.idCard === idCard);
    expect(created.name).toBe("CI 乘客");
    passengerId = created.id;

    const duplicateResponse = await request.post("/api/passenger/add", { headers, data: passenger });
    const duplicateBody = await duplicateResponse.json();
    expect(duplicateBody.code).toBe(500);
    expect(duplicateBody.msg).toContain("已存在");

    const deleteResponse = await request.delete(`/api/passenger/${passengerId}`, { headers });
    expect((await deleteResponse.json()).code).toBe(200);
    passengerId = undefined;
  } finally {
    if (passengerId) {
      await request.delete(`/api/passenger/${passengerId}`, { headers });
    }
    await deleteAccount(request, session);
  }
});

test("UC17 follows a user and exposes only public profile data", async ({ request }) => {
  const follower = await registerAndLogin(request);
  const followee = await registerAndLogin(request);
  const followerId = await getUserId(request, follower.username);
  const followeeId = await getUserId(request, followee.username);
  let followed = false;

  try {
    const activeFollower = await login(request, follower.username, follower.password);
    const headers = authenticatedHeaders(activeFollower);
    const followResponse = await request.post(`/api/follow/${followeeId}`, { headers });
    expect((await followResponse.json()).data.followed).toBeTruthy();
    followed = true;

    const statusResponse = await request.get(`/api/follow/status/${followeeId}`, { headers });
    expect((await statusResponse.json()).data).toBeTruthy();

    const fansResponse = await request.get(`/api/follow/fans/${followeeId}`, { headers });
    const fansBody = await fansResponse.json();
    expect(fansBody.data.some((user) => user.userId === followerId)).toBeTruthy();

    const profileResponse = await request.get(`/api/user/profile/${followee.username}`);
    const profileBody = await profileResponse.json();
    expect(profileBody.data.fansCount).toBeGreaterThanOrEqual(1);
    expect(profileBody.data).not.toHaveProperty("password");
    expect(profileBody.data).not.toHaveProperty("email");
    expect(profileBody.data).not.toHaveProperty("phone");

    const unfollowResponse = await request.post(`/api/follow/${followeeId}`, { headers });
    expect((await unfollowResponse.json()).data.followed).toBeFalsy();
    followed = false;
  } finally {
    if (followed) {
      const activeFollower = await login(request, follower.username, follower.password);
      await request.post(`/api/follow/${followeeId}`, { headers: authenticatedHeaders(activeFollower) });
    }
    const activeFollowee = await login(request, followee.username, followee.password);
    await deleteAccount(request, activeFollowee);
    const activeFollower = await login(request, follower.username, follower.password);
    await deleteAccount(request, activeFollower);
  }
});
