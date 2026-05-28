# 修改日志 - 2026-05-28

| 字段 | 内容 |
| --- | --- |
| 分支 | `main` |
| 提交者 | 今日暂无新提交，基于当前工作区未提交改动生成 |
| 提交 Hash | 无 |

## 今日概述

今天主要围绕 TravelMate 前端视觉体验做了一轮系统优化，重点是首页、AI 行程规划页、全局导航、公共页面标题和票务/酒店/社区列表的视觉统一。整体风格从偏后台卡片式界面，调整为更浅、更安静、更自然的品牌官网与旅行产品结合风格，并参考 Kosbiotic 的浅色背景、分屏 Hero 和大面板节奏继续打磨首页。

## 变更内容

### feat · New Feature

- **首页路线内容链路**: 新增路线预览模块，展示示例行程、预算、路线标签和跳转入口，让首页从“搜索入口”升级为更完整的旅行规划引导页（`frontend/src/views/Home.vue`）。
- **首页规划步骤视觉模块**: 将“把复杂行程，拆成三段安静的准备”改为与上方路线预览一致的大面板，并加入旅行图片、步骤浮层和标签点缀，提升内容丰满度（`frontend/src/views/Home.vue`）。
- **AI 行程规划工作台**: 将 AI 规划页重构为工作台布局，新增顶部说明、推荐起点、路线空状态预览、结果摘要和更清晰的每日时间线展示（`frontend/src/views/ai/AiPlan.vue`）。
- **首页搜索交互**: 为机票和火车票搜索增加出发/到达交换按钮，默认填充出发日期；酒店默认填充入住和退房日期（`frontend/src/views/Home.vue`）。
- **热门城市展示**: 首页热门城市数量从 5 个调整为 6 个，并改成更稳定的 3 x 2 桌面网格（`frontend/src/views/Home.vue`）。

### style · UI / Visual Polish

- **全局自然主题**: 新增浅色自然疗愈风格主题 token，统一 OKLCH 色彩、按钮、输入框、卡片、阴影和 Element Plus 基础控件视觉（`frontend/src/styles/theme.css`）。
- **首页 Hero 重排**: 参考 Kosbiotic 的浅底分屏节奏，调整 Hero 为浅米绿色背景、大字号标题、右侧目的地图像组和更克制的搜索面板（`frontend/src/views/Home.vue`）。
- **导航与页脚**: 优化顶部导航、Logo、用户入口、移动端按钮、面包屑和页脚的颜色、阴影、间距和圆角，使其与自然主题一致（`frontend/src/App.vue`）。
- **公共页面头部**: 将 `PageHeader` 改为浅色面板样式，标题层级和图标容器更统一（`frontend/src/components/PageHeader.vue`）。
- **社区列表**: 优化社区卡片圆角、边框、hover、图片遮罩、文案颜色和拒审提示样式（`frontend/src/views/community/Community.vue`）。
- **票务与酒店列表**: 统一机票、火车票、酒店搜索页的搜索框、结果卡片、价格、路线信息和移动端布局风格（`frontend/src/views/flight/FlightSearch.vue`、`frontend/src/views/train/TrainSearch.vue`、`frontend/src/views/hotel/HotelSearch.vue`）。

### fix · Bug Fix

- **图片视觉错位**: 修复首页“三段安静准备”模块右侧图片与步骤浮层边界不一致的问题，图片改为铺满视觉容器，步骤卡片对齐在图片内部右下角（`frontend/src/views/Home.vue`）。
- **交互可访问性**: 将首页热门城市卡片、功能入口卡片、AI 历史行程、客服浮窗等可点击 `div` 改为真实 `button`，改善键盘访问和语义（`frontend/src/views/Home.vue`、`frontend/src/views/ai/AiPlan.vue`）。

## 文件更改

| File | Changes |
| --- | --- |
| `frontend/src/views/Home.vue` | +1363 -308 |
| `frontend/src/views/ai/AiPlan.vue` | +735 -133 |
| `frontend/src/styles/theme.css` | +250 -0 |
| `frontend/src/App.vue` | +77 -53 |
| `frontend/src/views/train/TrainSearch.vue` | +30 -26 |
| `frontend/src/views/flight/FlightSearch.vue` | +29 -25 |
| `frontend/src/views/hotel/HotelSearch.vue` | +35 -14 |
| `frontend/src/views/community/Community.vue` | +19 -15 |
| `frontend/src/components/PageHeader.vue` | +20 -8 |

## 验证情况

- 已多次运行 `npm run build`，构建通过。
- 已用 `Invoke-WebRequest http://localhost:3000/` 检查首页，本地服务返回 `200`。
- 构建提示仍包含既有的大 chunk 警告，暂未处理代码分包。

## 未完成事项

- `backend/target/classes/com/travelmate/service/impl/TrainServiceImpl.class` 当前显示为已修改的编译产物，建议提交前确认是否需要恢复或加入忽略范围。
- 首页和 AI 规划页已重点优化，后续还可以继续细化目的地详情、酒店详情、订单中心和管理后台的视觉一致性。
- `theme.css` 中仍存在多段历史主题 token 叠加，后续可以进一步整理为单一主题层，降低维护成本。

## 明日计划

- 继续优化机票、火车票、酒店搜索结果的细节状态，包括空状态、加载状态和筛选区层级。
- 审查目的地详情、景点列表和酒店详情页，补齐图片、留白和信息层级。
- 清理生成产物和非源码改动，准备一次干净的前端视觉优化提交。
- 评估前端大 chunk 警告，考虑对 ECharts、管理后台或大页面做懒加载拆分。

## 备注

- 本日志由 `D:\Skill\daily-changelog` skill 生成。
- 今天暂无 git commit，日志内容来自当前工作区 diff 和构建验证结果。


## 追加记录：主页文字间距微调

| 字段 | 内容 |
| --- | --- |
| 提交者 | Sylphira-ovo |
| 提交 Hash | `3c4da3bf7354bb0ccf0e62c8ba74313701f27090` |
| 提交信息 | `update home page` |

### fix - Bug Fix

- **首页目的地文字重叠修复**: 修复首页 Hero 右侧主图卡片中“山水画卷”和“桂林”在部分视图下距离过近的问题，避免文字互相挤压或重叠。
- **图片卡片文字布局统一**: 将目的地标签和城市名从两个独立绝对定位元素调整为同一个 `.hero-image-caption` 文本组，用 `flex-direction: column` 和统一 `gap` 控制两行文字间距。
- **多卡片显示一致性**: 让桂林主卡片、杭州卡片、成都卡片使用同一套文字组结构，减少不同字号、不同卡片高度和浏览器缩放导致的视觉偏差。

### 验证情况

- 已在本地刷新首页，确认“山水画卷 / 桂林”不再重叠。
- 本次改动仅涉及 `frontend/src/views/Home.vue` 的模板与 scoped CSS，未改动接口、路由、数据源和后端逻辑。
