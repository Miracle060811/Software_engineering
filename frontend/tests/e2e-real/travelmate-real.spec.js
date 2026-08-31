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
  const password = `pw${randomUUID().replaceAll("-", "").slice(0, 16)}`;
  const registerResponse = await request.post("/user/register", {
    form: { username, password },
  });
  expect(registerResponse.ok()).toBeTruthy();
  expect((await registerResponse.json()).code).toBe(200);
  return login(request, username, password);
}

async function registerAdminAndLogin(request) {
  const secret = process.env.ADMIN_REGISTER_SECRET;
  expect(secret).toBeTruthy();
  const username = `ciadmin${Date.now()}${randomUUID().replaceAll("-", "").slice(0, 8)}`;
  const password = `pw${randomUUID().replaceAll("-", "").slice(0, 16)}`;
  const registerResponse = await request.post("/user/admin-register", {
    form: { username, password, secret },
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

test("[E2E-TC-101] UC01 registers and logs in against the real backend", async ({ page, request }) => {
  let session = await registerAndLogin(request);

  try {
    await page.goto("/login");
    await page.getByPlaceholder("请输入用户名").fill(session.username);
    await page.getByPlaceholder("请输入密码").fill(session.password);
    await page.getByRole("button", { name: "登 录" }).click();

    await expect(page).toHaveURL(/\/$/);
    await expect.poll(() => page.evaluate(() => localStorage.getItem("token"))).not.toBeNull();

    const meBody = await (await request.get("/user/me", {
      headers: authenticatedHeaders(session),
    })).json();
    expect(meBody.code).toBe(200);
    expect(meBody.data.username).toBe(session.username);
    expect(meBody.data.password).toBeNull();

    const newPassword = `next${randomUUID().replaceAll("-", "").slice(0, 16)}`;
    const changeBody = await (await request.post("/user/password", {
      headers: authenticatedHeaders(session),
      form: { oldPassword: session.password, newPassword },
    })).json();
    expect(changeBody.code).toBe(200);

    const oldLoginBody = await (await request.post("/user/login", {
      form: { username: session.username, password: session.password },
    })).json();
    expect(oldLoginBody.code).not.toBe(200);
    session = await login(request, session.username, newPassword);
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

test("[E2E-TC-104][E2E-TC-110] UC04 and UC10 create, pay/refund and cancel flight orders with a real coupon", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  let pendingOrderNo;

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
    const flight = flightsBody.data[0];

    const couponsResponse = await request.get("/api/coupon/list", { headers });
    const couponsBody = await couponsResponse.json();
    expect(couponsBody.code).toBe(200);
    const coupon = couponsBody.data.find((item) =>
      (item.category === "all" || item.category === "flight") &&
      Number(item.minAmount) <= Number(flight.economyPrice));
    expect(coupon).toBeTruthy();

    const claimResponse = await request.post(`/api/coupon/claim/${coupon.id}`, { headers });
    expect((await claimResponse.json()).code).toBe(200);

    const duplicateClaimResponse = await request.post(`/api/coupon/claim/${coupon.id}`, { headers });
    const duplicateClaimBody = await duplicateClaimResponse.json();
    expect(duplicateClaimBody.code).toBe(500);
    expect(duplicateClaimBody.msg).toContain("已领取过");

    const myCouponsResponse = await request.get("/api/coupon/my", { headers });
    const myCouponsBody = await myCouponsResponse.json();
    expect(myCouponsBody.code).toBe(200);
    const userCoupon = myCouponsBody.data.find((item) => item.couponId === coupon.id);
    expect(userCoupon).toBeTruthy();
    expect(userCoupon.status).toBe(0);

    const createResponse = await request.post("/api/order/flight/create", {
      headers,
      data: {
        flightId: flight.id,
        passengerId: passengersBody.data[0].id,
        seatType: "Economy",
        ticketCount: 1,
        userCouponId: userCoupon.id,
      },
    });
    const createBody = await createResponse.json();
    expect(createResponse.ok()).toBeTruthy();
    expect(createBody.code).toBe(200);
    expect(createBody.data).toMatch(/^T/);
    const refundOrderNo = createBody.data;

    const ordersResponse = await request.get("/api/order/list", { headers });
    const ordersBody = await ordersResponse.json();
    expect(ordersBody.code).toBe(200);
    const discountedOrder = ordersBody.data.find((order) => order.orderNo === refundOrderNo);
    expect(discountedOrder).toBeTruthy();
    const expectedAmount = coupon.discountType === 1
      ? Number(flight.economyPrice) * Number(coupon.discountValue)
      : Number(flight.economyPrice) - Number(coupon.discountValue);
    expect(Number(discountedOrder.amount)).toBeCloseTo(Math.max(0, expectedAmount), 2);

    const couponsAfterUseResponse = await request.get("/api/coupon/my", { headers });
    const couponsAfterUseBody = await couponsAfterUseResponse.json();
    const usedCoupon = couponsAfterUseBody.data.find((item) => item.id === userCoupon.id);
    expect(usedCoupon.status).toBe(1);
    expect(usedCoupon.usedTime).toBeTruthy();

    const payResponse = await request.post(`/api/order/${refundOrderNo}/pay`, { headers });
    expect((await payResponse.json()).code).toBe(200);

    const repeatedPayResponse = await request.post(`/api/order/${refundOrderNo}/pay`, { headers });
    const repeatedPayBody = await repeatedPayResponse.json();
    expect(repeatedPayBody.code).toBe(500);
    expect(repeatedPayBody.msg).toContain("无法再次支付");

    const refundResponse = await request.post(`/api/order/${refundOrderNo}/refund`, { headers });
    expect((await refundResponse.json()).code).toBe(200);

    const receiptResponse = await request.get(`/api/order/${refundOrderNo}/receipt`, { headers });
    const receiptBody = await receiptResponse.json();
    expect(receiptBody.code).toBe(200);
    expect(receiptBody.data.status).toBe(5);

    const pendingCreateResponse = await request.post("/api/order/flight/create", {
      headers,
      data: {
        flightId: flight.id,
        passengerId: passengersBody.data[0].id,
        seatType: "Economy",
        ticketCount: 1,
        userCouponId: null,
      },
    });
    const pendingCreateBody = await pendingCreateResponse.json();
    expect(pendingCreateBody.code).toBe(200);
    pendingOrderNo = pendingCreateBody.data;

    const cancelResponse = await request.post(`/api/order/${pendingOrderNo}/cancel`, { headers });
    expect((await cancelResponse.json()).code).toBe(200);
    const cancelledReceiptResponse = await request.get(`/api/order/${pendingOrderNo}/receipt`, { headers });
    expect((await cancelledReceiptResponse.json()).data.status).toBe(3);
    pendingOrderNo = undefined;
  } finally {
    if (pendingOrderNo) {
      const cancelResponse = await request.post(`/api/order/${pendingOrderNo}/cancel`, { headers });
      expect(cancelResponse.ok()).toBeTruthy();
      expect((await cancelResponse.json()).code).toBe(200);
    }
    await deleteAccount(request, session);
  }
});

test("[E2E-TC-102] UC02 flight search renders results from the real backend", async ({ page }) => {
  await page.goto("/flight-search");
  await page.getByPlaceholder("如：北京").fill("北京");
  await page.getByPlaceholder("如：上海").fill("上海");
  await page.getByRole("button", { name: /搜索航班/ }).click();
  await expect(page.locator(".flight-card").first()).toBeVisible();
});

test("[E2E-TC-108] UC08 exposes day-tour and nearby-tour contracts and rejects invalid type", async ({ request }) => {
  for (const type of [0, 1]) {
    const body = await (await request.get(`/api/tour/list?type=${type}`)).json();
    expect(body.code).toBe(200);
    expect(Array.isArray(body.data)).toBeTruthy();
    expect(body.data.length).toBeGreaterThan(0);
    for (const product of body.data) {
      expect(product.tourType).toBe(type);
      expect(product.name).toBeTruthy();
      expect(Number(product.price)).toBeGreaterThanOrEqual(0);
    }
  }

  const invalidBody = await (await request.get("/api/tour/list?type=9")).json();
  expect(invalidBody.code).not.toBe(200);
  expect(invalidBody.msg).toContain("必须为0或1");
});

test("[E2E-TC-111][E2E-TC-112] UC11 and UC12 generate a saved plan and persist a multi-turn AI chat", async ({ request }) => {
  const owner = await registerAndLogin(request);
  const outsider = await registerAndLogin(request);
  const ownerHeaders = authenticatedHeaders(owner);

  try {
    const startDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    const generateBody = await (await request.post("/api/ai/plan/generate", {
      headers: ownerHeaders,
      data: {
        origin: "上海",
        destination: "杭州",
        days: 2,
        budget: 3000,
        peopleCount: 2,
        preferences: "美食,轻松",
        startDate,
        travelStyle: "轻松",
        transportPreference: "公共交通",
        accommodationPreference: "安静",
      },
    })).json();
    expect(generateBody.code).toBe(200);
    expect(generateBody.data.id).toBeTruthy();
    expect(generateBody.data.userId).toBeTruthy();
    expect(generateBody.data.destination).toMatch(/^杭州(?:市)?$/);
    expect(generateBody.data.days).toBe(2);
    const planContent = JSON.parse(generateBody.data.planContent);
    expect(planContent.origin).toMatch(/^上海(?:市)?$/);
    expect(planContent.destination).toBe(generateBody.data.destination);
    expect(planContent.locationVerified).toBeTruthy();
    expect(planContent.days).toHaveLength(2);

    const listBody = await (await request.get("/api/ai/plan/list", { headers: ownerHeaders })).json();
    expect(listBody.code).toBe(200);
    expect(listBody.data.some((plan) => plan.id === generateBody.data.id)).toBeTruthy();

    const outsiderSession = await login(request, outsider.username, outsider.password);
    const forbiddenBody = await (await request.get(`/api/ai/plan/${generateBody.data.id}`, {
      headers: authenticatedHeaders(outsiderSession),
    })).json();
    expect(forbiddenBody.code).not.toBe(200);
    expect(forbiddenBody.msg).toContain("无权访问");

    const chatSession = `uc12-${randomUUID()}`;
    const firstReply = await (await request.post("/api/ai/chat", {
      headers: ownerHeaders,
      data: { sessionId: chatSession, message: "你好", clientDate: startDate, clientTimeZone: "Asia/Shanghai" },
    })).json();
    expect(firstReply.code).toBe(200);
    expect(firstReply.data.sessionId).toBe(chatSession);
    expect(firstReply.data.role).toBe("assistant");
    expect(firstReply.data.content).toContain("目的地");

    const secondReply = await (await request.post("/api/ai/chat", {
      headers: ownerHeaders,
      data: { sessionId: chatSession, message: "酒店怎么选", clientDate: startDate, clientTimeZone: "Asia/Shanghai" },
    })).json();
    expect(secondReply.code).toBe(200);
    expect(secondReply.data.sessionId).toBe(chatSession);
    expect(secondReply.data.content).toContain("酒店");

    const blankBody = await (await request.post("/api/ai/chat", {
      headers: ownerHeaders,
      data: { sessionId: chatSession, message: "   " },
    })).json();
    expect(blankBody.code).not.toBe(200);
    expect(blankBody.msg).toContain("消息不能为空");
  } finally {
    const activeOutsider = await login(request, outsider.username, outsider.password);
    await deleteAccount(request, activeOutsider);
    const activeOwner = await login(request, owner.username, owner.password);
    await deleteAccount(request, activeOwner);
  }
});

test("[E2E-TC-103] UC03 creates and cancels a train order against real inventory", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  let orderNo;

  try {
    const passengerResponse = await request.post("/api/passenger/add", {
      headers,
      data: {
        name: "CI 火车旅客",
        idCard: `T${randomUUID().replaceAll("-", "").slice(0, 11).toUpperCase()}`,
        phone: "13800138000",
      },
    });
    expect((await passengerResponse.json()).code).toBe(200);
    const passengersBody = await (await request.get("/api/passenger/list", { headers })).json();
    const passenger = passengersBody.data.find((item) => item.name === "CI 火车旅客");
    expect(passenger).toBeTruthy();

    const trainsBody = await (await request.get(
      "/api/train/search?depStation=北京南&arrStation=上海虹桥",
    )).json();
    expect(trainsBody.code).toBe(200);
    const train = trainsBody.data.find((item) => Number(item.secondClassSeats) > 0);
    expect(train).toBeTruthy();

    const createBody = await (await request.post("/api/order/train/create", {
      headers,
      data: {
        trainId: train.id,
        passengerId: passenger.id,
        seatType: "SecondClass",
        ticketCount: 1,
        userCouponId: null,
      },
    })).json();
    expect(createBody.code).toBe(200);
    expect(createBody.data).toMatch(/^TR/);
    orderNo = createBody.data;

    const receiptBody = await (await request.get(`/api/order/${orderNo}/receipt`, { headers })).json();
    expect(receiptBody.code).toBe(200);
    expect(receiptBody.data.ticketNo).toBe(train.trainNo);
    expect(receiptBody.data.seatType).toBe("SecondClass");
    expect(Number(receiptBody.data.amount)).toBeCloseTo(Number(train.secondClassPrice), 2);

    const cancelBody = await (await request.post(`/api/order/${orderNo}/cancel`, { headers })).json();
    expect(cancelBody.code).toBe(200);
    orderNo = undefined;
  } finally {
    if (orderNo) {
      await request.post(`/api/order/${orderNo}/cancel`, { headers });
    }
    await deleteAccount(request, session);
  }
});

test("[MS-E2E-MEMBER1] identity ownership, traffic order and Outbox notification form a real service chain", async ({ request }) => {
  const owner = await registerAndLogin(request);
  const outsider = await registerAndLogin(request);
  const ownerIdCard = `O${randomUUID().replaceAll("-", "").slice(0, 11).toUpperCase()}`;
  const outsiderIdCard = `X${randomUUID().replaceAll("-", "").slice(0, 11).toUpperCase()}`;
  let ownerPassengerId;
  let outsiderPassengerId;

  try {
    const outsiderHeaders = authenticatedHeaders(outsider);
    const outsiderAddBody = await (await request.post("/api/passenger/add", {
      headers: outsiderHeaders,
      data: {
        name: "CI 越权旅客",
        idCard: outsiderIdCard,
        phone: "13900139000",
      },
    })).json();
    expect(outsiderAddBody.code).toBe(200);
    const outsiderPassengers = await (await request.get("/api/passenger/list", {
      headers: outsiderHeaders,
    })).json();
    outsiderPassengerId = outsiderPassengers.data.find(
      (item) => item.idCard === outsiderIdCard,
    )?.id;
    expect(outsiderPassengerId).toBeTruthy();

    const activeOwner = await login(request, owner.username, owner.password);
    const ownerHeaders = authenticatedHeaders(activeOwner);
    const ownerAddBody = await (await request.post("/api/passenger/add", {
      headers: ownerHeaders,
      data: {
        name: "CI 跨服务旅客",
        idCard: ownerIdCard,
        phone: "13800138000",
      },
    })).json();
    expect(ownerAddBody.code).toBe(200);
    const ownerPassengers = await (await request.get("/api/passenger/list", {
      headers: ownerHeaders,
    })).json();
    ownerPassengerId = ownerPassengers.data.find((item) => item.idCard === ownerIdCard)?.id;
    expect(ownerPassengerId).toBeTruthy();

    const trainsBody = await (await request.get(
      "/api/train/search?depStation=北京南&arrStation=上海虹桥",
    )).json();
    expect(trainsBody.code).toBe(200);
    const train = trainsBody.data.find((item) => Number(item.secondClassSeats) > 1);
    expect(train).toBeTruthy();

    const ordersBefore = await (await request.get("/api/order/list", {
      headers: ownerHeaders,
    })).json();
    const inventoryBefore = Number(train.secondClassSeats);
    const forbiddenBody = await (await request.post("/api/order/train/create", {
      headers: ownerHeaders,
      data: {
        trainId: train.id,
        passengerId: outsiderPassengerId,
        seatType: "SecondClass",
        ticketCount: 1,
        userCouponId: null,
      },
    })).json();
    expect(forbiddenBody.code).not.toBe(200);

    const ordersAfterForbidden = await (await request.get("/api/order/list", {
      headers: ownerHeaders,
    })).json();
    const trainAfterForbidden = await (await request.get(`/api/train/${train.id}`)).json();
    expect(ordersAfterForbidden.data).toHaveLength(ordersBefore.data.length);
    expect(Number(trainAfterForbidden.data.secondClassSeats)).toBe(inventoryBefore);

    const createBody = await (await request.post("/api/order/train/create", {
      headers: ownerHeaders,
      data: {
        trainId: train.id,
        passengerId: ownerPassengerId,
        seatType: "SecondClass",
        ticketCount: 1,
        userCouponId: null,
      },
    })).json();
    expect(createBody.code).toBe(200);
    expect(createBody.data).toMatch(/^TR/);
    const orderNo = createBody.data;

    const payBody = await (await request.post(`/api/order/${orderNo}/pay`, {
      headers: ownerHeaders,
    })).json();
    expect(payBody.code).toBe(200);
    const receiptBody = await (await request.get(`/api/order/${orderNo}/receipt`, {
      headers: ownerHeaders,
    })).json();
    expect(receiptBody.code).toBe(200);
    expect(receiptBody.data.status).toBe(1);

    await expect.poll(async () => {
      const notificationBody = await (await request.get("/api/notification/list", {
        headers: ownerHeaders,
      })).json();
      return notificationBody.data?.some(
        (item) => item.type === "traffic_order" && item.content?.includes(orderNo),
      );
    }, { timeout: 20000, intervals: [500, 1000, 2000] }).toBeTruthy();
  } finally {
    const activeOutsider = await login(request, outsider.username, outsider.password);
    if (outsiderPassengerId) {
      await request.delete(`/api/passenger/${outsiderPassengerId}`, {
        headers: authenticatedHeaders(activeOutsider),
      });
    }
    await deleteAccount(request, activeOutsider);

    const activeOwner = await login(request, owner.username, owner.password);
    if (ownerPassengerId) {
      await request.delete(`/api/passenger/${ownerPassengerId}`, {
        headers: authenticatedHeaders(activeOwner),
      });
    }
    await deleteAccount(request, activeOwner);
  }
});

test("[E2E-TC-105][E2E-TC-106] UC05 and UC06 create, pay, refund and cancel hotel orders", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  const checkInDate = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  const checkOutDate = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  const pendingOrders = new Set();

  try {
    const hotelsBody = await (await request.get("/api/hotel/search?city=上海")).json();
    expect(hotelsBody.code).toBe(200);
    expect(hotelsBody.data.length).toBeGreaterThan(0);
    const hotel = hotelsBody.data[0];
    const roomsBody = await (await request.get(`/api/hotel/${hotel.id}/rooms`)).json();
    expect(roomsBody.code).toBe(200);
    const room = roomsBody.data.find((item) => Number(item.availableRooms) > 1);
    expect(room).toBeTruthy();

    const createHotelOrder = async () => {
      const body = await (await request.post("/api/hotel/order/create", {
        headers,
        data: {
          hotelId: hotel.id,
          roomId: room.id,
          roomCount: 1,
          checkInDate,
          checkOutDate,
          guestName: "CI 酒店住客",
          guestPhone: "13800138000",
          userCouponId: null,
        },
      })).json();
      expect(body.code).toBe(200);
      expect(body.data).toMatch(/^HT/);
      pendingOrders.add(body.data);
      return body.data;
    };

    const refundOrderNo = await createHotelOrder();
    const receiptBody = await (await request.get(
      `/api/hotel/order/${refundOrderNo}/receipt`, { headers },
    )).json();
    expect(receiptBody.data.roomId).toBe(room.id);
    expect(receiptBody.data.nights).toBe(2);
    expect(Number(receiptBody.data.amount)).toBeCloseTo(Number(room.price) * 2, 2);

    expect((await (await request.post(
      `/api/hotel/order/${refundOrderNo}/pay`, { headers },
    )).json()).code).toBe(200);
    expect((await (await request.post(
      `/api/hotel/order/${refundOrderNo}/refund`, { headers },
    )).json()).code).toBe(200);
    const refundedBody = await (await request.get(
      `/api/hotel/order/${refundOrderNo}/receipt`, { headers },
    )).json();
    expect(refundedBody.data.status).toBe(5);
    pendingOrders.delete(refundOrderNo);

    const cancelOrderNo = await createHotelOrder();
    expect((await (await request.post(
      `/api/hotel/order/${cancelOrderNo}/cancel`, { headers },
    )).json()).code).toBe(200);
    const cancelledBody = await (await request.get(
      `/api/hotel/order/${cancelOrderNo}/receipt`, { headers },
    )).json();
    expect(cancelledBody.data.status).toBe(4);
    pendingOrders.delete(cancelOrderNo);
  } finally {
    for (const orderNo of pendingOrders) {
      await request.post(`/api/hotel/order/${orderNo}/cancel`, { headers });
    }
    await deleteAccount(request, session);
  }
});

test("[E2E-TC-107] UC07 buys attraction tickets and isolates receipt ownership", async ({ request }) => {
  const buyer = await registerAndLogin(request);
  const outsider = await registerAndLogin(request);
  let activeBuyer;

  try {
    activeBuyer = await login(request, buyer.username, buyer.password);
    const buyerHeaders = authenticatedHeaders(activeBuyer);
    const attractionsBody = await (await request.get("/api/attraction/search?city=北京")).json();
    expect(attractionsBody.code).toBe(200);
    const attraction = attractionsBody.data.find((item) => Number(item.availableTickets) >= 2);
    expect(attraction).toBeTruthy();

    const buyBody = await (await request.post(`/api/attraction/${attraction.id}/ticket`, {
      headers: buyerHeaders,
      data: {
        adultCount: 1,
        childCount: 1,
        guestName: "CI 景点游客",
        guestPhone: "13800138000",
      },
    })).json();
    expect(buyBody.code).toBe(200);
    expect(buyBody.data).toMatch(/^AT/);

    const receiptBody = await (await request.get(
      `/api/attraction/order/${buyBody.data}/receipt`, { headers: buyerHeaders },
    )).json();
    expect(receiptBody.code).toBe(200);
    expect(receiptBody.data.ticketCount).toBe(2);
    const expectedAmount = Number(attraction.adultPrice) + Number(attraction.childPrice || 0);
    expect(Number(receiptBody.data.amount)).toBeCloseTo(expectedAmount, 2);

    const activeOutsider = await login(request, outsider.username, outsider.password);
    const outsiderBody = await (await request.get(
      `/api/attraction/order/${buyBody.data}/receipt`,
      { headers: authenticatedHeaders(activeOutsider) },
    )).json();
    expect(outsiderBody.code).not.toBe(200);
    expect(outsiderBody.msg).toContain("无权查看");
  } finally {
    const activeOutsider = await login(request, outsider.username, outsider.password);
    await deleteAccount(request, activeOutsider);
    activeBuyer = await login(request, buyer.username, buyer.password);
    await deleteAccount(request, activeBuyer);
  }
});

test("[E2E-TC-109] UC09 submits, lists and reports a review against the real backend", async ({ request }) => {
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

test("[E2E-TC-113] UC13 sends and reads a private message against the real backend", async ({ request }) => {
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

test("[E2E-TC-115] UC15 likes, collects and comments on a post against the real backend", async ({ request }) => {
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

test("[E2E-TC-116] UC16 adds, validates and deletes a passenger against the real backend", async ({ request }) => {
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

test("[E2E-TC-117] UC17 follows a user and exposes only public profile data", async ({ request }) => {
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

test("[E2E-TC-118][E2E-TC-119] UC18 and UC19 enforce admin RBAC and complete audit workflows", async ({ request }) => {
  const ordinary = await registerAndLogin(request);
  const ordinaryHeaders = authenticatedHeaders(ordinary);
  let admin;
  let sensitiveWordId;

  try {
    const forbiddenResponse = await request.get("/api/admin/stats", { headers: ordinaryHeaders });
    expect(forbiddenResponse.status()).toBe(403);

    const marker = `UC19-${randomUUID()}`;
    const reviewBody = await (await request.post("/api/review/add", {
      headers: ordinaryHeaders,
      data: { targetId: 1, targetType: 0, rating: 3, content: `待审核评价 ${marker}` },
    })).json();
    expect(reviewBody.code).toBe(200);
    const reviewsBody = await (await request.get("/api/review/list?targetId=1&targetType=0")).json();
    const review = reviewsBody.data.find((item) => item.content.includes(marker));
    expect(review).toBeTruthy();
    const reportBody = await (await request.post("/api/review/report", {
      headers: ordinaryHeaders,
      data: { reviewId: review.id, reason: marker },
    })).json();
    expect(reportBody.code).toBe(200);

    admin = await registerAdminAndLogin(request);
    const adminHeaders = authenticatedHeaders(admin);
    const statsBody = await (await request.get("/api/admin/stats", { headers: adminHeaders })).json();
    expect(statsBody.code).toBe(200);
    expect(Number(statsBody.data.totalUsers)).toBeGreaterThanOrEqual(2);
    expect(statsBody.data).toHaveProperty("totalOrders");
    expect(statsBody.data).toHaveProperty("pendingPosts");

    const word = `ci-${randomUUID()}`;
    const createWordBody = await (await request.post("/api/admin/sensitive-words", {
      headers: adminHeaders,
      data: { word: `  ${word}  `, level: 2 },
    })).json();
    expect(createWordBody.code).toBe(200);
    expect(createWordBody.data.word).toBe(word);
    sensitiveWordId = createWordBody.data.id;

    const duplicateWordBody = await (await request.post("/api/admin/sensitive-words", {
      headers: adminHeaders,
      data: { word, level: 2 },
    })).json();
    expect(duplicateWordBody.code).not.toBe(200);
    expect(duplicateWordBody.msg).toContain("已存在");

    const pendingReports = await (await request.get(
      "/api/admin/review-reports?status=0", { headers: adminHeaders },
    )).json();
    const report = pendingReports.data.find((item) => item.reason === marker);
    expect(report).toBeTruthy();
    const resolveBody = await (await request.post(`/api/admin/review-reports/${report.id}/resolve`, {
      headers: adminHeaders,
      data: { remark: "CI 人工复核完成" },
    })).json();
    expect(resolveBody.code).toBe(200);

    const handledReports = await (await request.get(
      "/api/admin/review-reports?status=1", { headers: adminHeaders },
    )).json();
    const handled = handledReports.data.find((item) => item.id === report.id);
    expect(handled.handleRemark).toBe("CI 人工复核完成");

    const logsBody = await (await request.get("/api/admin/logs?page=1&size=20", {
      headers: adminHeaders,
    })).json();
    expect(logsBody.code).toBe(200);
    expect(Array.isArray(logsBody.data.records)).toBeTruthy();
    expect(logsBody.data.size).toBe(20);
  } finally {
    if (sensitiveWordId && admin) {
      await request.delete(`/api/admin/sensitive-words/${sensitiveWordId}`, {
        headers: authenticatedHeaders(admin),
      });
    }
    const activeOrdinary = await login(request, ordinary.username, ordinary.password);
    await deleteAccount(request, activeOrdinary);
  }
});

test("[E2E-TC-114] UC14 creates, edits and deletes a post against the real backend", async ({ request }) => {
  const session = await registerAndLogin(request);
  const headers = authenticatedHeaders(session);
  let postId;

  try {
    const marker = `UC14-${randomUUID()}`;
    const createResponse = await request.post("/api/post/create", {
      headers,
      data: {
        title: `  ${marker}  `,
        content: "原始内容",
        destination: "丽江",
        tags: "自由行",
        visibility: "0",
      },
    });
    const createBody = await createResponse.json();
    expect(createResponse.ok()).toBeTruthy();
    expect(createBody.code).toBe(200);
    expect(createBody.data.title).toBe(marker);
    expect(createBody.data.status).toBe(0);
    postId = createBody.data.id;

    await new Promise((resolve) => setTimeout(resolve, 1100));

    const myPostsResponse = await request.get("/api/post/my", { headers });
    const myPostsBody = await myPostsResponse.json();
    expect(myPostsBody.code).toBe(200);
    expect(myPostsBody.data.some((post) => post.id === postId)).toBeTruthy();

    const updateResponse = await request.put(`/api/post/${postId}`, {
      headers,
      data: {
        title: "更新后的标题",
        content: "更新后的内容",
        destination: "大理",
        tags: "自驾",
        visibility: "1",
      },
    });
    const updateBody = await updateResponse.json();
    expect(updateResponse.ok()).toBeTruthy();
    expect(updateBody.code).toBe(200);
    expect(updateBody.data.title).toBe("更新后的标题");
    expect(updateBody.data.visibility).toBe(1);

    const deleteResponse = await request.delete(`/api/post/${postId}`, { headers });
    expect((await deleteResponse.json()).code).toBe(200);
    postId = undefined;
  } finally {
    if (postId) {
      await request.delete(`/api/post/${postId}`, { headers });
    }
    await deleteAccount(request, session);
  }
});
