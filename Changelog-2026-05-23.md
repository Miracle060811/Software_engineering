# 修改日志 — 2026-05-23

| 字段      | 内容       |
| --------- | ---------- |
| 提交者    | yfan945    |
| 提交 Hash | `edcc49be` |

## 今日概述

> 今日完成了一轮大规模重构，覆盖前后端多个核心模块。后端重点优化了社区（游记/评论/点赞）的数据填充逻辑和权限控制，并完善了优惠券核销流程；前端完成了登录页密码重置功能、个人主页游记状态筛选/账户注销、社区搜索增强，以及首页视觉升级（团队照片 + 功能图标）。整体工作量约 2168 行新增、399 行删除，共 121 个文件变更。

---

## 变更内容

### feat · 新功能

- **认证 · 忘记密码**: 登录页新增"忘记密码"弹窗，支持通过用户名重置密码；后端 `UserController` + `UserService` 新增重置接口（`Login.vue`, `UserController.java`, `UserService.java`）

- **社区 · 游记权限墙**: 游记详情增加三重可见性检查——审核状态、私密设置、关注墙；未关注作者无法查看仅粉丝可见的游记（`PostServiceImpl.java`）

- **社区 · 关键词搜索**: `listPosts` 接口新增 `keyword` 参数支持游记标题/内容搜索（`PostServiceImpl.java`, `PostMapper.java`, `Community.vue`）

- **用户中心 · 游记状态筛选**: 个人主页自己可见时，游记列表新增草稿 / 待审核 / 已发布 / 已拒绝四状态筛选标签；同时展示审核说明提示（`UserProfile.vue`）

- **用户中心 · 注销账户**: 个人主页新增"注销账户"按钮（`UserProfile.vue`, `UserController.java`）

- **用户中心 · 关注/粉丝弹窗**: 主页关注数、粉丝数变为可点击，弹出对应列表（`UserProfile.vue`）

- **优惠券 · 核销逻辑完善**: `CouponServiceImpl` 大量新增核销判断、库存扣减、用户绑定逻辑；新增 `UserCouponMapper` 批量查询能力（`CouponServiceImpl.java`, `UserCouponMapper.java`）

- **Redis 常量集中管理**: 新增 `RedisKeyConstants` 统一维护 Redis key 前缀，避免魔法字符串散落各处（`RedisKeyConstants.java`）

- **首页视觉升级**: 新增团队成员头像资源（5 张）和功能特性图标（AI 规划、景点门票、旅游社区），首页 + App.vue 对应展示区大幅改版（`App.vue`, `Home.vue`）

### fix · Bug 修复

- **评论 · N+1 查询消除**: 评论树构建改为先批量加载所有相关用户，再填充 `authorUsername/Nickname/Avatar` 和 `replyUsername/Nickname`，消除逐条查库的 N+1 问题（`CommentServiceImpl.java`）

- **游记 · 作者信息缺失**: 游记详情/列表现在通过 `fillAuthor` 方法统一回填作者用户名，前端不再出现作者为空的情况（`PostServiceImpl.java`, `Post.java`）

- **限流器 · 逻辑优化**: `RateLimiterInterceptor` 重构计数判断逻辑，解决并发边界问题（`RateLimiterInterceptor.java`）

- **订单 · 防超卖结果对象化**: 新增 `StockPreDeductResult` 值对象，统一表达库存预扣减结果，避免方法返回歧义布尔值；顺带修正酒店和交通订单超卖边界（`StockPreDeductResult.java`, `HotelOrderServiceImpl.java`, `TrafficOrderServiceImpl.java`）

### refactor · 重构

- **点赞模块**: `LikeServiceImpl` 重构（+81 行），状态查询和 Redis 缓存策略调整（`LikeServiceImpl.java`）

- **关注模块**: `FollowServiceImpl` 少量重构，补充接口签名一致性（`FollowServiceImpl.java`）

- **DTO 统一字段**: `FlightOrderCreateDTO`、`HotelOrderCreateDTO`、`TrainOrderCreateDTO` 各新增 couponId 等字段，支持下单时传入优惠券（对应 DTO 文件）

- **Axios 拦截器**: `request.js` 调整响应错误处理逻辑，特殊状态码分支更清晰（`request.js`）

### docs · 文档

- **README**: 新增团队成员信息和功能模块说明（`README.md`）

---

## 文件更改（仅关键源码，忽略 dist/target）

| 文件                                               | 变更           |
| -------------------------------------------------- | -------------- |
| `backend/.../service/impl/PostServiceImpl.java`    | +146 -27       |
| `backend/.../service/impl/CouponServiceImpl.java`  | +131 -8        |
| `frontend/src/views/user/UserProfile.vue`          | +320 -40       |
| `frontend/src/App.vue`                             | +239 -28       |
| `frontend/src/views/community/PostDetail.vue`      | +148 -30       |
| `frontend/src/views/order/CouponCenter.vue`        | +179 -45       |
| `frontend/src/views/community/Community.vue`       | +178 -12       |
| `backend/.../service/impl/LikeServiceImpl.java`    | +81 -20        |
| `backend/.../service/impl/CommentServiceImpl.java` | +83 -10        |
| `frontend/src/views/Login.vue`                     | +133 -0        |
| `frontend/src/views/flight/FlightSearch.vue`       | +90 -10        |
| `frontend/src/views/train/TrainSearch.vue`         | +79 -5         |
| `frontend/src/views/hotel/HotelDetail.vue`         | +78 -8         |
| `frontend/src/views/community/PostCreate.vue`      | +68 -10        |
| `backend/.../common/RedisKeyConstants.java`        | +11 -0（新建） |
| `backend/.../service/StockPreDeductResult.java`    | +7 -0（新建）  |

---

## 未完成事项

- `FlightSearch.vue` 和 `TrainSearch.vue` 未提交的修改（在工作区已改动但未入库），需确认是否可稳定后提交
- `docs/sql/init.sql` 有 +19 行未提交的改动，需验证新增建表/初始化语句正确后一并提交
- 优惠券在订单下单时的完整链路（DTO 字段已加，但 Service 层核销调用路径需端到端联调验证）
- 游记可见性-关注墙功能前端提示文案待细化（目前只抛异常，前端展示仍为通用报错）

## 明日计划

1. **提交未暂存改动**：将 `FlightSearch.vue`、`TrainSearch.vue`、`init.sql` 的本地修改验证无误后提交
2. **优惠券下单联调**：完成 Flight/Train/Hotel 下单时优惠券核销的端到端测试
3. **游记权限前端适配**：对"关注后可见"等权限拒绝做友好提示或引导关注弹窗
4. **测试覆盖**：为今日新增的 `PostServiceImpl` 权限检查和 `CommentServiceImpl` 批量用户加载补充单测

## 备注

- 今日提交时间凌晨 01:44，建议代码 review 重点关注 `PostServiceImpl.getPostDetail` 的权限判断分支（尤其是 `currentUserId == null` 时的兜底行为）。
- 团队成员头像已并入仓库（5 张 jpg，总计 ~392 KB），后续若有版权顾虑可改用 CDN 外链。
- `target/` 目录的 `.class` 文件被意外提交（`edcc49be`），建议在 `.gitignore` 中补充 `backend/target/` 并清理历史。
