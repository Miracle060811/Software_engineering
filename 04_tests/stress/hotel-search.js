// 压测场景二：酒店查询接口 GET /api/hotel/search
//
// 执行示例（重复实验 3 次）：
//   k6 run -e BASE_URL=http://127.0.0.1:8080 -e RUN=1 --summary-export=results/hotel-search-run1.json hotel-search.js

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const RUN = __ENV.RUN || '1';

export const options = {
  scenarios: {
    hotel_search: {
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
    http_req_failed: ['rate==0'],
  },
  tags: { scenario: 'hotel-search', run: RUN },
};

export default function () {
  const cities = ['北京', '上海', '成都', '广州', '杭州'];
  const city = cities[Math.floor(Math.random() * cities.length)];

  const res = http.get(
    `${BASE_URL}/api/hotel/search?city=${encodeURIComponent(city)}`,
    { tags: { name: 'hotel-search' } }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body is success': (r) => r.json('code') === 200 || r.json('code') === 0 || r.json('code') === undefined,
  });

  sleep(0.5);
}
