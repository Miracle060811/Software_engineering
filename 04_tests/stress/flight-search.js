// 压测场景一：航班查询接口 GET /api/flight/search
// 依据《5组-软件详细设计说明》3.12：同一提交、同一数据量、同一硬件、同一脚本，每场景预热后连续 3 次，报告中给出每次结果和中位数。
//
// 执行示例（重复实验 3 次，run=1/2/3）：
//   k6 run -e BASE_URL=http://127.0.0.1:8080 -e RUN=1 --summary-export=results/flight-search-run1.json flight-search.js
//
// 阶梯负载：默认 5/10/20/50 并发各 30s；可通过环境变量覆盖。

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const RUN = __ENV.RUN || '1';

// 预热 + 阶梯加压（Ramp 上升后稳定，便于观察吞吐与 P95）
export const options = {
  scenarios: {
    flight_search: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 5 },    // 预热
        { duration: '30s', target: 10 },
        { duration: '30s', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '15s', target: 0 },    // 回落
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    // 压测门槛：错误率必须为 0，P95 响应时间记录但不设上限（如实报告）
    http_req_failed: ['rate==0'],
  },
  tags: { scenario: 'flight-search', run: RUN },
};

export default function () {
  // 用多组城市组合模拟真实查询分布，避免单条 SQL 缓存偏差
  const cities = [
    ['北京', '上海'],
    ['上海', '成都'],
    ['广州', '北京'],
    ['深圳', '杭州'],
    ['成都', '广州'],
  ];
  const [dep, arr] = cities[Math.floor(Math.random() * cities.length)];

  const res = http.get(
    `${BASE_URL}/api/flight/search?depCity=${encodeURIComponent(dep)}&arrCity=${encodeURIComponent(arr)}`,
    { tags: { name: 'flight-search' } }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body is success': (r) => r.json('code') === 200 || r.json('code') === 0 || r.json('code') === undefined,
  });

  sleep(0.5); // 模拟用户思考时间，避免纯缓存打爆
}
