# 修改日志 - 2026-05-28

| 字段      | 内容                               |
| --------- | ---------------------------------- |
| 分支      | `main`                             |
| 提交者    | Yinghao Xiang、Sylphira-ovo        |
| 提交 Hash | `d50499f2`、`3c4da3bf`、`8b950abe` |

## 今日概述

今天主要围绕 TravelMate 前端视觉体验做系统级优化，重点覆盖首页、AI 行程规划页、全局导航、公共页面标题，以及机票、火车票、酒店和社区列表页。整体界面从偏后台卡片式风格调整为更浅、更自然的旅行产品视觉，并在后续提交中修复了首页 Hero 图片卡片文字间距问题，同时补充了对应日志记录。

## 变更内容

### feat · New Feature

- **首页路线内容链路**: 新增路线预览、预算、标签和跳转入口，让首页不仅是搜索入口，也能承接旅行规划引导（`frontend/src/views/Home.vue`）。
- **首页规划步骤模块**: 将“三段安静的准备”改为图文大面板，加入旅行图片、步骤浮层和标签点缀，提升首页内容层次（`frontend/src/views/Home.vue`）。
- **AI 行程规划工作台**: 重构 AI 规划页布局，新增推荐起点、结果摘要、空状态预览和更清晰的每日时间线展示（`frontend/src/views/ai/AiPlan.vue`）。
- **首页搜索交互**: 为机票和火车票搜索增加出发/到达交换按钮，并补齐机票、火车票、酒店的默认日期逻辑（`frontend/src/views/Home.vue`）。
- **热门城市展示**: 首页热门城市从 5 个扩展为 6 个，并调整为更稳定的桌面网格展示（`frontend/src/views/Home.vue`）。

### style · UI / Visual Polish

- **全局自然主题**: 新增浅色自然风格主题 token，统一 OKLCH 色彩、按钮、输入框、卡片、阴影和 Element Plus 控件视觉（`frontend/src/styles/theme.css`）。
- **首页 Hero 重排**: 调整 Hero 为浅底分屏结构，强化右侧目的地图像组、搜索面板和主标题的旅行产品气质（`frontend/src/views/Home.vue`）。
- **导航与页脚**: 优化顶部导航、Logo、用户入口、移动端按钮、面包屑和页脚的颜色、间距、圆角与阴影，使整体风格更统一（`frontend/src/App.vue`）。
- **公共页面头部**: 将 `PageHeader` 调整为浅色面板样式，统一标题层级、图标容器和页面节奏（`frontend/src/components/PageHeader.vue`）。
- **社区与搜索列表**: 统一社区卡片、机票搜索、火车票搜索、酒店搜索的搜索框、结果卡片、价格、路线信息和移动端布局（`frontend/src/views/community/Community.vue`、`frontend/src/views/flight/FlightSearch.vue`）。

### fix · Bug Fix

- **首页图片文字重叠**: 将 Hero 目的地卡片中的标签和城市名合并到 `.hero-image-caption` 文本组，用统一布局控制间距，避免“山水画卷 / 桂林”等文字在部分视图下挤压重叠（`frontend/src/views/Home.vue`）。
- **首页图片视觉错位**: 修复“三段安静准备”模块右侧图片和步骤浮层边界不一致的问题，让图片铺满容器并让步骤卡片稳定对齐（`frontend/src/views/Home.vue`）。
- **交互语义与可访问性**: 将首页热门城市卡片、功能入口卡片、AI 历史行程、客服浮窗等可点击 `div` 改为真实 `button`，改善键盘访问和语义表达（`frontend/src/views/Home.vue`、`frontend/src/views/ai/AiPlan.vue`）。

### docs · Documentation

- **日志记录整理**: 新增并补充 2026-05-28 修改日志，记录前端视觉优化、首页文字间距修复、验证情况和后续计划（`Changelog-2026-05-28.md`）。
- **README 说明补充**: 更新项目说明中与前端视觉和页面结构相关的内容，便于后续维护者理解当前页面状态（`README.md`）。

## 文件更改

| File                                         | Changes    |
| -------------------------------------------- | ---------- |
| `frontend/src/views/Home.vue`                | +1378 -316 |
| `frontend/src/views/ai/AiPlan.vue`           | +735 -133  |
| `frontend/src/styles/theme.css`              | +250 -0    |
| `frontend/src/App.vue`                       | +77 -53    |
| `frontend/src/views/train/TrainSearch.vue`   | +30 -26    |
| `frontend/src/views/flight/FlightSearch.vue` | +29 -25    |
| `frontend/src/views/hotel/HotelSearch.vue`   | +35 -14    |
| `frontend/src/views/community/Community.vue` | +19 -15    |
| `frontend/src/components/PageHeader.vue`     | +20 -8     |
| `README.md`                                  | +4 -0      |
| `Changelog-2026-05-28.md`                    | +93 -0     |

## 验证情况

- 当日记录显示已多次运行 `npm run build`，前端生产构建通过。
- 当日记录显示已用 `Invoke-WebRequest http://localhost:3000/` 检查首页，本地服务返回 `200`。
- 首页 Hero 右侧目的地卡片文字间距已在本地刷新确认，不再出现“山水画卷 / 桂林”挤压重叠。
- 构建仍存在既有的大 chunk 警告，暂未在当天处理代码分包。

## 未完成事项

- 首页和 AI 规划页已经完成重点优化，后续仍可继续细化目的地详情、酒店详情、订单中心和管理后台的视觉一致性。
- `theme.css` 中存在多段历史主题 token 叠加，后续可以整理为更清晰的单一主题层，降低维护成本。
- 前端大 chunk 警告仍未处理，后续可评估对 ECharts、管理后台或大页面做懒加载拆分。

## 明日计划

- 继续优化机票、火车票、酒店搜索结果页的空状态、加载状态和筛选区层级。
- 审查目的地详情、景点列表和酒店详情页，补齐图片、留白和信息层级。
- 清理生成产物和非源码改动，准备一次干净的前端视觉优化提交。
- 评估前端大 chunk 警告，优先拆分体积较大的页面和图表依赖。

## 备注

- 本日志已按 2026-05-28 当天实际提交重写，移除了旧版“今日暂无新提交”的错误说明。
- 提交 `8b950abe` 仅补充日志内容；本次重写已将追加记录合并进统一结构。
