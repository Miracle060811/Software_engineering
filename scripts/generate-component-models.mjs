import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const outputDir = path.join(root, "02_docs", "概要设计说明");
const sourceDir = path.join(outputDir, "source");

const models = [
  ["UC01", "用户注册登录与账户安全", "Login.vue / UserProfile.vue", "POST /user/register、/user/login、/user/password", "UserController", "UserService", "UserMapper + JwtUtil", "MySQL: tm_user", "用户名重复、凭证错误或账号禁用时返回失败"],
  ["UC02", "查询并预订航班", "FlightSearch.vue", "GET /api/flight/search；POST /api/order/flight/create", "FlightController + TrafficOrderController", "FlightServiceImpl + TrafficOrderServiceImpl", "FlightMapper + TrafficOrderMapper", "MySQL: tm_flight、tm_traffic_order", "无航班或库存不足时不创建订单"],
  ["UC03", "查询火车票与提交候补", "TrainSearch.vue", "GET /api/train/search；POST /api/train/waitlist、/api/order/train/create", "TrainController + TrafficOrderController", "TrainServiceImpl + TrainWaitlistServiceImpl + TrafficOrderServiceImpl", "TrainMapper + TrainWaitlistMapper + TrafficOrderMapper", "MySQL: tm_train、tm_train_waitlist、tm_traffic_order", "同步失败回退本地数据；旅客或库存无效时拒绝"],
  ["UC04", "交通订单支付取消退款", "MyOrders.vue", "POST /api/order/{orderNo}/pay|cancel|refund", "TrafficOrderController", "TrafficOrderServiceImpl + OrderTimeoutScheduler", "TrafficOrderMapper + NotificationCenterServiceImpl", "MySQL: tm_traffic_order、tm_notification", "订单不属于当前用户或状态跃迁非法时拒绝"],
  ["UC05", "搜索酒店并完成订房", "HotelSearch.vue / HotelDetail.vue", "GET /api/hotel/search、/{id}/rooms；POST /api/hotel/order/create", "HotelController", "HotelServiceImpl + HotelOrderServiceImpl + HotelRoomStockServiceImpl", "HotelMapper + HotelRoomMapper + HotelOrderMapper", "Redis + MySQL: tm_hotel、tm_hotel_room、tm_hotel_order", "Redis 可降级；数据库库存不足时回滚预减并失败"],
  ["UC06", "酒店订单支付取消退款与库存回补", "MyOrders.vue", "POST /api/hotel/order/{orderNo}/pay|cancel|refund", "HotelController", "HotelOrderServiceImpl + HotelRoomStockServiceImpl + OrderTimeoutScheduler", "HotelOrderMapper + HotelRoomMapper", "Redis + MySQL: tm_hotel_order、tm_hotel_room", "越权或非法状态拒绝；重复回补按幂等规则跳过"],
  ["UC07", "景点浏览与购票", "AttractionList.vue", "GET /api/attraction/search；POST /api/attraction/{id}/ticket", "AttractionController", "AttractionServiceImpl", "AttractionMapper + AttractionOrderMapper", "MySQL: tm_attraction、tm_attraction_order", "景点不可用、票数无效或未登录时拒绝购票"],
  ["UC08", "浏览一日游周边游产品", "AttractionList.vue", "GET /api/tour/list?type=", "TourProductController", "TourProductServiceImpl", "TourProductMapper", "MySQL: tm_tour_product", "查询失败返回错误；当前仅覆盖浏览而不含购买闭环"],
  ["UC09", "提交评价回复与举报处理", "HotelDetail.vue / AdminDashboard.vue", "POST /api/review/add、/api/reply/add、/api/review/report；后台处理接口", "ReviewController + ReplyController + ReviewReportController + AdminController", "ReviewServiceImpl", "ReviewMapper + ReplyMapper + ReviewReportMapper", "MySQL: tm_review、tm_reply、tm_review_report", "越权、目标不存在或重复处理时返回失败"],
  ["UC10", "优惠券领取与订单核销", "CouponCenter.vue / 下单页", "GET /api/coupon/list、/my；POST /api/coupon/claim/{id}", "CouponController", "CouponServiceImpl", "CouponMapper + UserCouponMapper", "MySQL: tm_coupon、tm_user_coupon", "库存、有效期或重复领取校验失败时拒绝；核销证据待补"],
  ["UC11", "生成并保存AI行程", "AiPlan.vue", "POST /api/ai/plan/generate", "AiController", "AiServiceImpl", "DeepSeek API + AiPlanMapper", "MySQL: tm_ai_plan", "API Key 缺失、超时或 JSON 非法时使用本地模板并标记降级"],
  ["UC12", "AI客服多轮对话", "AiPlan.vue", "POST /api/ai/chat", "AiController", "AiServiceImpl", "DeepSeek API + AiChatMapper", "MySQL: tm_ai_chat", "外部调用失败时返回可解释错误，不编造订单价格和库存"],
  ["UC13", "通知中心与站内私信", "NotificationCenter.vue / PrivateMessages.vue", "notification 与 /api/private-message/* 接口", "AiController + PrivateMessageController", "NotificationCenterServiceImpl + PrivateMessageServiceImpl", "NotificationMapper + PrivateMessageMapper + PrivateContactMapper", "MySQL: tm_notification、tm_private_message、tm_private_contact", "非本人通知或会话访问被拒绝；通知失败不回滚核心业务"],
  ["UC14", "游记发布编辑删除与审核", "Community.vue / PostCreate.vue / AdminDashboard.vue", "POST /api/post/create；PUT/DELETE /api/post/{id}；后台审核接口", "PostController + AdminController", "PostServiceImpl + SensitiveWordServiceImpl", "PostMapper + SysSensitiveWordMapper", "MySQL: tm_post、sys_sensitive_word", "敏感词、越权编辑或非法审核状态时拒绝"],
  ["UC15", "社区点赞收藏与评论", "PostDetail.vue / MyCollections.vue", "POST /api/like/toggle、/api/comment/add；DELETE /api/comment/{id}", "LikeController + CommentController", "LikeServiceImpl + CommentServiceImpl", "LikeMapper + CommentMapper", "MySQL: tm_like、tm_comment", "重复互动保持幂等；越权删除或父评论不匹配时拒绝"],
  ["UC16", "常用旅客管理与使用", "FlightSearch.vue / TrainSearch.vue", "GET /api/passenger/list；POST /add；DELETE /{id}", "PassengerController", "PassengerServiceImpl", "PassengerMapper", "MySQL: tm_passenger", "证件信息无效、重复或旅客不属于当前用户时拒绝"],
  ["UC17", "用户主页与关注关系", "UserProfile.vue / PostDetail.vue", "GET /api/user/profile/*；POST /api/follow/{userId}", "UserProfileController + FollowController", "FollowServiceImpl", "UserMapper + PostMapper + FollowMapper", "MySQL: tm_user、tm_post、tm_follow", "用户不存在、自关注或非法访问时返回失败"],
  ["UC18", "管理后台资源订单与用户管理", "AdminDashboard.vue", "/api/admin/**", "JwtFilter + SecurityConfig + AdminController", "现有业务 Service（按资源复用）", "各业务 Mapper + SysLogAspect + SysLogMapper", "MySQL: 业务表、sys_log", "未登录或非管理员拒绝；写入冲突返回失败并记录日志"],
  ["UC19", "内容安全举报处理与可观测性", "AdminDashboard.vue", "/api/admin/posts、review-reports、sensitive-words、logs", "AdminController", "SensitiveWordServiceImpl + NotificationCenterServiceImpl", "PostMapper + ReviewReportMapper + SysSensitiveWordMapper + SysLogMapper", "MySQL: tm_post、tm_review_report、sys_sensitive_word、sys_log", "重复处理保持终态；统计或通知故障不回滚审核结果"],
];

fs.mkdirSync(sourceDir, { recursive: true });

const overall = `flowchart LR
    subgraph Browser[浏览器表现层]
      Vue[Vue 3 页面组件]
      Router[Vue Router]
      Axios[Axios request.js]
      Vue --> Router --> Axios
    end
    subgraph Monolith[Spring Boot 模块化单体（当前实现）]
      Security[JwtFilter / SecurityConfig]
      Controller[Controller 接口层]
      Service[Service / ServiceImpl 业务层]
      Mapper[MyBatis-Plus Mapper]
      Scheduler[OrderTimeoutScheduler 等定时任务]
      Log[SysLogAspect / SysLogMapper]
      Security --> Controller --> Service --> Mapper
      Scheduler --> Service
      Controller -.写操作审计.-> Log
    end
    Axios -->|HTTP/JSON| Security
    Mapper --> MySQL[(MySQL 8)]
    Service --> Redis[(Redis 缓存/限流/库存预减)]
    Service --> DeepSeek[DeepSeek OpenAI兼容 API]
    DeepSeek -.失败降级.-> Template[本地模板]
    Log --> MySQL
    subgraph Target[目标设计（尚未部署）]
      Gateway[API Gateway]
      Microservices[按业务域拆分的微服务]
      Containers[Docker / Kubernetes / CD]
      Gateway --> Microservices --> Containers
    end
    Monolith -.后续演进.-> Target
`;
fs.writeFileSync(path.join(sourceDir, "00_系统总体组件图.mmd"), overall, "utf8");

for (const [id, title, page, api, controller, service, dependency, data, failure] of models) {
  const content = `sequenceDiagram
    title COMP-${id} ${title}——组件级顺序图
    actor User as 用户/调用方
    participant Page as ${page}
    participant API as Axios / Vue Router
    participant Controller as ${controller}
    participant Service as ${service}
    participant Dependency as ${dependency}
    participant Data as ${data}
    User->>Page: 发起“${title}”操作
    Page->>API: ${api}
    API->>Controller: HTTP/JSON + JWT（需要时）
    Controller->>Service: 参数、身份与业务请求
    Service->>Dependency: 执行业务规则/持久化或外部调用
    Dependency->>Data: 查询或条件写入
    alt 成功
      Data-->>Dependency: 返回数据或影响行数
      Dependency-->>Service: 返回业务结果
      Service-->>Controller: 成功结果
      Controller-->>API: Result.success(...)
      API-->>Page: 更新页面状态
      Page-->>User: 展示可验证结果
    else 关键失败
      Note over Service,Data: ${failure}
      Service-->>Controller: 抛出/返回业务失败
      Controller-->>API: Result.error(...)
      API-->>Page: 展示错误且不伪造成功状态
    end
`;
  fs.writeFileSync(path.join(sourceDir, `COMP-${id}_${title}.mmd`), content, "utf8");
}

console.log(`Generated ${models.length + 1} Mermaid sources in ${path.relative(root, sourceDir)}`);
