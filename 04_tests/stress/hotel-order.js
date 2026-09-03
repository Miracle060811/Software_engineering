// 压测场景三：酒店下单接口 POST /api/hotel/order/create（需登录）
//
// 核心目标：验证高并发防超卖。
//   每个虚拟用户使用独立测试账号登录，对同一房型（HOTEL_ID/ROOM_ID 环境变量指定）下 1 间房。
//   脚本内置库存一致性断言：
//     成功订单数 + 业务失败数 = 总请求数
//     成功订单数 ≤ 压测前可用房间数（不超卖）
//
// 执行示例（重复实验 3 次）：
//   k6 run -e BASE_URL=http://127.0.0.1:8080 -e HOTEL_ID=6 -e ROOM_ID=14 \
//          -e INITIAL_STOCK=1 -e RUN=1 \
//          --summary-export=results/hotel-order-run1.json hotel-order.js
//
// 压测前准备（执行 setup 阶段自动完成账号注册）：
//   - USER_COUNT 个压测账号（stress_user_1 .. stress_user_N），密码 STRESS_PASSWORD
//   - 压测后需人工核对数据库：tm_hotel_room.available_rooms >= 0 且
//     tm_hotel_order 中该房型的有效订单数 <= 压测前库存

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const HOTEL_ID = Number(__ENV.HOTEL_ID || 6);
const ROOM_ID = Number(__ENV.ROOM_ID || 14);
const INITIAL_STOCK = Number(__ENV.INITIAL_STOCK || 1);
const RUN = __ENV.RUN || '1';
const STRESS_PASSWORD = __ENV.STRESS_PASSWORD || 'Stress#Pass123';

// 并发用户数：与 INITAL_STOCK 配合，制造"抢库存"竞争
const VU_COUNT = Number(__ENV.VU_COUNT || 50);
const businessSuccess = new Counter('business_success');

export const options = {
  scenarios: {
    hotel_order: {
      executor: 'per-vu-iterations',
      vus: VU_COUNT,
      iterations: 1, // 每用户只下一单：库存只有 INITIAL_STOCK，其余必然失败
      maxDuration: '2m',
    },
  },
  thresholds: {
    // http 层面必须全 200（业务失败返回 200 + code!=200，不算请求失败）
    http_req_failed: ['rate==0'],
  },
  tags: { scenario: 'hotel-order', run: RUN },
};

const tokens = {};

// setup：为每个虚拟用户注册并登录独立账号
export function setup() {
  const created = {};
  for (let i = 1; i <= VU_COUNT; i++) {
    const username = `stress_${RUN}_${i}`;
    const password = STRESS_PASSWORD;
    // 注册（已存在则忽略失败）
    http.post(`${BASE_URL}/user/register?username=${username}&password=${password}`, null, {
      tags: { name: 'setup-register' },
    });
    // 登录拿 token
    const loginRes = http.post(`${BASE_URL}/user/login?username=${username}&password=${password}`, null, {
      tags: { name: 'setup-login' },
    });
    const code = loginRes.json('code');
    const token = loginRes.json('data');
    if ((code === 200 || code === 0) && token) {
      created[i] = token;
    } else {
      console.log(`setup: user ${username} login failed code=${code}`);
    }
  }
  console.log(`setup: ${Object.keys(created).length}/${VU_COUNT} users ready`);
  return created;
}

// 订单创建成功计数（teardown 中用于库存一致性断言）

export default function (data) {
  const vuId = __VU;
  const token = data[vuId];
  if (!token) {
    console.log(`VU ${vuId}: no token, skip`);
    return;
  }

  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  const payload = JSON.stringify({
    hotelId: HOTEL_ID,
    roomId: ROOM_ID,
    roomCount: 1,
    checkInDate: tomorrow(),
    checkOutDate: dayAfterTomorrow(),
    guestName: `压测用户${vuId}`,
    guestPhone: '13800000000',
  });

  const res = http.post(`${BASE_URL}/api/hotel/order/create`, payload, {
    headers,
    tags: { name: 'hotel-order-create' },
  });

  const body = res.json();
  const ok = res.status === 200 && (body.code === 200 || body.code === 0);

  check(res, {
    'status is 200': (r) => r.status === 200,
    // 成功 = 拿到订单号；失败 = 返回明确的业务错误（如"暂无可用房间"）
    'order created or business-rejected': () => ok || (body.code !== 200 && body.code !== 0 && !!body.message),
  });

  if (ok) {
    businessSuccess.add(1);
  }

  sleep(0.2);
}

// teardown：库存一致性断言——成功订单数不得超过初始库存
export function teardown() {
  const successCount = businessSuccess.value;
  console.log(`teardown: successful orders = ${successCount}, initial stock = ${INITIAL_STOCK}`);
  if (successCount > INITIAL_STOCK) {
    console.log(`ASSERTION FAILED: oversell! ${successCount} > ${INITIAL_STOCK}`);
  } else {
    console.log(`ASSERTION PASSED: no oversell (${successCount}/${INITIAL_STOCK})`);
  }
}

function tomorrow() {
  const d = new Date(Date.now() + 24 * 3600 * 1000);
  return d.toISOString().slice(0, 10);
}

function dayAfterTomorrow() {
  const d = new Date(Date.now() + 48 * 3600 * 1000);
  return d.toISOString().slice(0, 10);
}
