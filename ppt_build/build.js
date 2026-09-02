const pptxgen = require('C:/Users/28603/AppData/Roaming/npm/node_modules/pptxgenjs');
const path = require('path');

const A = f => path.join('E:/SoftEngneeringHomework/Software_engineering/ppt_build/assets', f);
const OUT = 'E:/SoftEngneeringHomework/Software_engineering/document/TravelMate项目展示.pptx';

// palette: ocean-teal travel theme
const BG_DARK = '0B2B2A';   // deep teal-black (cover/closing)
const BG = 'FFFFFF';
const TINT = 'EFF6F5';      // light teal tint for cards
const TINT2 = 'E2EFED';     // slightly deeper tint
const PRIMARY = '0F766E';   // teal
const PRIMARY_DK = '0A4F4A';
const ACCENT = 'E8590C';    // sunset orange
const TEXT = '1E293B';
const MUTED = '64748B';
const ON_DARK = 'EAF4F2';
const ON_DARK_MUTED = '9DC3BE';

const FONT = '微软雅黑';
const W = 13.33, H = 7.5, M = 0.5;

const pres = new pptxgen();
pres.layout = 'LAYOUT_WIDE';
pres.author = 'Miracle Team';
pres.title = 'TravelMate 项目展示';

const shadow = () => ({ type: 'outer', color: '0B2B2A', blur: 8, offset: 2, angle: 60, opacity: 0.18 });
const bu = () => ({ code: '25B8', indent: 12 });

function pageChrome(s, n) {
  s.background = { color: BG };
  s.addText(String(n).padStart(2, '0'), { x: W - 1.0, y: H - 0.45, w: 0.5, h: 0.3, fontSize: 11, fontFace: FONT, color: MUTED, align: 'right', margin: 0 });
  s.addText('TravelMate', { x: M, y: H - 0.45, w: 2.5, h: 0.3, fontSize: 11, fontFace: FONT, color: MUTED, margin: 0 });
}

function header(s, kicker, title, dark = false) {
  s.addText(kicker, { x: M, y: 0.42, w: 9, h: 0.32, fontSize: 13, fontFace: FONT, color: ACCENT, bold: true, charSpacing: 3, margin: 0 });
  s.addText(title, { x: M, y: 0.74, w: W - 2 * M, h: 0.75, fontSize: 32, fontFace: FONT, bold: true, color: dark ? ON_DARK : TEXT, margin: 0 });
}

/* ---------------- S1 封面 ---------------- */
{
  const s = pres.addSlide();
  s.background = { color: BG_DARK };
  s.addImage({ path: A('01_系统总体架构图.png'), x: 7.0, y: 1.72, w: 5.83, h: 3.89, shadow: shadow() });
  s.addText('SOFTWARE ENGINEERING PROJECT SHOWCASE', { x: 0.75, y: 1.55, w: 6.2, h: 0.35, fontSize: 11.5, fontFace: FONT, color: ACCENT, bold: true, charSpacing: 2, margin: 0 });
  s.addText('TravelMate 伴游', { x: 0.75, y: 2.0, w: 6.25, h: 1.0, fontSize: 50, fontFace: FONT, bold: true, color: ON_DARK, margin: 0 });
  s.addText('一站式智慧旅行平台', { x: 0.75, y: 3.15, w: 6, h: 0.55, fontSize: 25, fontFace: FONT, bold: true, color: '5EEAD4', margin: 0 });
  s.addText('覆盖"行前规划 → 资源预订 → 行中服务 → 行后分享"全链路，\n19 个业务场景全部具备自动化验收证据的完整工程实践。',
    { x: 0.75, y: 4.05, w: 5.9, h: 1.0, fontSize: 14, fontFace: FONT, color: ON_DARK_MUTED, lineSpacing: 22, margin: 0 });
  s.addShape(pres.shapes.LINE, { x: 0.78, y: 5.45, w: 1.2, h: 0, line: { color: ACCENT, width: 2.5 } });
  s.addText('Miracle 团队 · 2026', { x: 0.75, y: 5.62, w: 5, h: 0.35, fontSize: 14, fontFace: FONT, color: ON_DARK, bold: true, margin: 0 });
}

/* ---------------- S2 项目定位 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 2);
  header(s, 'PROJECT OVERVIEW', '一个真实的旅行平台，一次完整的工程实践');
  s.addText([
    { text: 'TravelMate 不是玩具 Demo —— 它是一个主链路可运行、可部署、可追溯的旅行服务平台：', options: { breakLine: true } },
  ], { x: M, y: 1.75, w: 11.5, h: 0.5, fontSize: 17, fontFace: FONT, color: TEXT, margin: 0 });
  const steps = [
    ['行前规划', 'AI 生成个性化行程，机票火车酒店一键比价'],
    ['资源预订', '航班、火车票候补、订房、景点购票与一日游'],
    ['行中服务', '订单支付退款、优惠券核销、通知与 AI 客服'],
    ['行后分享', '游记发布审核、评价互动、点赞收藏与关注'],
  ];
  const cw = 2.75, gap = 0.22, x0 = M, y0 = 2.6;
  steps.forEach(([t, d], i) => {
    const x = x0 + i * (cw + gap);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: y0, w: cw, h: 2.5, fill: { color: TINT }, rectRadius: 0.08 });
    s.addText(String(i + 1).padStart(2, '0'), { x: x + 0.22, y: y0 + 0.22, w: 1.2, h: 0.55, fontSize: 30, fontFace: FONT, bold: true, color: ACCENT, margin: 0 });
    s.addText(t, { x: x + 0.22, y: y0 + 0.88, w: cw - 0.44, h: 0.42, fontSize: 19, fontFace: FONT, bold: true, color: PRIMARY_DK, margin: 0 });
    s.addText(d, { x: x + 0.22, y: y0 + 1.36, w: cw - 0.4, h: 1.0, fontSize: 12.5, fontFace: FONT, color: MUTED, lineSpacing: 19, margin: 0 });
    if (i < 3) s.addShape(pres.shapes.RIGHT_ARROW, { x: x + cw + 0.005, y: y0 + 1.05, w: 0.21, h: 0.4, fill: { color: PRIMARY } });
  });
  s.addText([
    { text: '模拟支付与出票环境，聚焦工程完整性：', options: { bold: true, color: PRIMARY_DK } },
    { text: ' 需求 → 设计 → 实现 → 测试 → 交付，每一环都有真实产物与流水线证据。', options: { color: MUTED } },
  ], { x: M, y: 5.55, w: 12.3, h: 0.5, fontSize: 15, fontFace: FONT, margin: 0 });
}

/* ---------------- S3 功能全景 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 3);
  header(s, 'FEATURE MAP', '19 个业务场景 · 五大业务域');
  const domains = [
    ['账号与旅客', 'UC01 · 16', ['注册登录与账户安全', '常用旅客管理', '用户主页与关注']],
    ['交通票务', 'UC02 · 03 · 04', ['航班查询与预订', '火车票与候补', '支付、退款与凭证']],
    ['住宿与本地生活', 'UC05 · 06 · 07 · 08 · 10', ['酒店搜索与订房', '库存回补与退款', '景点购票 / 一日游', '优惠券领取与核销']],
    ['AI 智能服务', 'UC11 · 12 · 13', ['AI 行程生成', 'AI 客服多轮对话', '通知中心与私信']],
    ['社区与运营', 'UC09 · 14 · 15 · 17 · 18 · 19', ['游记发布与审核', '评价、点赞、收藏', '管理后台', '内容安全与可观测']],
  ];
  const cw = 2.32, gap = 0.21, x0 = M, y0 = 1.85;
  domains.forEach(([t, ucs, items], i) => {
    const x = x0 + i * (cw + gap);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: y0, w: cw, h: 4.6, fill: { color: i === 2 ? TINT2 : TINT }, rectRadius: 0.07 });
    s.addText(t, { x: x + 0.18, y: y0 + 0.25, w: cw - 0.36, h: 0.75, fontSize: 16, fontFace: FONT, bold: true, color: PRIMARY_DK, margin: 0 });
    s.addText(ucs, { x: x + 0.18, y: y0 + 1.0, w: cw - 0.36, h: 0.55, fontSize: 10.5, fontFace: FONT, color: ACCENT, bold: true, margin: 0 });
    s.addText(items.map((it, j) => ({ text: it, options: { bullet: bu(), breakLine: j < items.length - 1 } })),
      { x: x + 0.18, y: y0 + 1.6, w: cw - 0.32, h: 2.8, fontSize: 11.5, fontFace: FONT, color: TEXT, paraSpaceAfter: 7, margin: 0 });
  });
  s.addText('每个用例均对应系统级、组件级、对象级三层设计模型与自动化测试，追溯覆盖率 100%。',
    { x: M, y: 6.7, w: 12, h: 0.35, fontSize: 13, fontFace: FONT, color: MUTED, margin: 0 });
}

/* ---------------- S4 系统架构 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 4);
  header(s, 'ARCHITECTURE', '前后端分离 · 渐进式走向微服务');
  s.addImage({ path: A('01_系统总体架构图.png'), x: 6.55, y: 1.78, w: 6.28, h: 4.19, shadow: shadow() });
  const rows = [
    ['接入层', 'Vue 3 + Element Plus 单页应用，Nginx 托管，统一 REST 网关'],
    ['应用层', 'Spring Boot 3.5 单体优先，按领域分包：票务、住宿、AI、社区、运营'],
    ['数据层', 'MySQL 8.0（Flyway 版本化迁移）+ Redis（缓存、限流、库存扣减）'],
    ['演进路线', '6 个目标微服务边界已冻结，identity / traffic / local / ai 四服务已落地'],
  ];
  rows.forEach(([t, d], i) => {
    const y = 1.95 + i * 1.08;
    s.addText(t, { x: M, y, w: 1.6, h: 0.4, fontSize: 16, fontFace: FONT, bold: true, color: PRIMARY, margin: 0 });
    s.addText(d, { x: 2.25, y: y + 0.02, w: 4.15, h: 0.95, fontSize: 12.5, fontFace: FONT, color: TEXT, lineSpacing: 19, margin: 0 });
    if (i < 3) s.addShape(pres.shapes.LINE, { x: M, y: y + 0.92, w: 5.9, h: 0, line: { color: 'D8E4E2', width: 1 } });
  });
}

/* ---------------- S5 技术栈 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 5);
  header(s, 'TECH STACK', '现代化技术栈，全流程工程化');
  const rows = [
    ['前端', 'Vue 3 · Vite · Element Plus · Pinia · ECharts', '组件化 SPA，健康检查内建'],
    ['后端', 'Java 21 · Spring Boot 3.5 · Spring Security · MyBatis-Plus', 'JWT 认证，模块化领域分包'],
    ['数据', 'MySQL 8.0 + Flyway · Redis', '版本化迁移；缓存 / 限流 / 防超卖'],
    ['微服务', 'Spring Cloud 风格 · Docker Compose · 独立 Maven 模块', 'identity / traffic / local / ai'],
    ['交付', 'Docker · Kubernetes · GitHub Actions · GHCR', '一条流水线从提交走到线上'],
  ];
  rows.forEach(([t, main, d], i) => {
    const y = 1.85 + i * 0.98;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: M, y: y + 0.06, w: 1.45, h: 0.52, fill: { color: PRIMARY }, rectRadius: 0.26 });
    s.addText(t, { x: M, y: y + 0.06, w: 1.45, h: 0.52, fontSize: 14, fontFace: FONT, bold: true, color: 'FFFFFF', align: 'center', valign: 'middle', margin: 0 });
    s.addText(main, { x: 2.3, y: y + 0.02, w: 6.7, h: 0.4, fontSize: 15.5, fontFace: FONT, bold: true, color: TEXT, margin: 0 });
    s.addText(d, { x: 2.3, y: y + 0.42, w: 6.7, h: 0.32, fontSize: 11.5, fontFace: FONT, color: MUTED, margin: 0 });
    s.addText(['全链路可观测', '安全内建', '声明式迁移', '边界已冻结', '不可变交付'][i], { x: 9.6, y: y + 0.12, w: 3.2, h: 0.4, fontSize: 13, fontFace: FONT, bold: true, color: ACCENT, align: 'right', margin: 0 });
    if (i < 4) s.addShape(pres.shapes.LINE, { x: 2.3, y: y + 0.86, w: 10.5, h: 0, line: { color: 'D8E4E2', width: 1 } });
  });
}

/* ---------------- S6 亮点① 交通票务 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 6);
  header(s, 'HIGHLIGHT 01', '交通票务：高并发下的库存一致性');
  s.addImage({ path: A('06_库存防超卖流程图.png'), x: 6.9, y: 1.85, w: 5.9, h: 3.93, shadow: shadow() });
  s.addText('从查票到出票的完整订单状态机', { x: M, y: 1.9, w: 6, h: 0.4, fontSize: 17, fontFace: FONT, bold: true, color: PRIMARY_DK, margin: 0 });
  s.addText([
    { text: '航班实时查询与预订，火车票支持候补队列', options: { bullet: bu(), breakLine: true } },
    { text: '订单支付、取消、退款全状态流转，凭证可追溯', options: { bullet: bu(), breakLine: true } },
    { text: 'Redis 预扣库存 + 确认制落库，杜绝超卖', options: { bullet: bu(), breakLine: true } },
    { text: '候补兑现、退款回补库存由事件驱动，最终一致', options: { bullet: bu() } },
  ], { x: M, y: 2.45, w: 6.0, h: 2.2, fontSize: 14, fontFace: FONT, color: TEXT, paraSpaceAfter: 12, margin: 0 });
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: M, y: 5.0, w: 6.0, h: 1.15, fill: { color: TINT2 }, rectRadius: 0.08 });
  s.addText([
    { text: '关键设计：', options: { bold: true, color: PRIMARY_DK } },
    { text: '库存扣减前置到 Redis 原子操作，数据库只承接确认订单，把热点航段的并发压力挡在数据库之外。', options: { color: TEXT } },
  ], { x: 0.75, y: 5.12, w: 5.6, h: 0.95, fontSize: 13, fontFace: FONT, lineSpacing: 20, margin: 0 });
}

/* ---------------- S7 亮点② AI 服务 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 7);
  header(s, 'HIGHLIGHT 02', 'AI 深度融入旅行全流程');
  s.addImage({ path: A('05_AI行程生成流程图.png'), x: 0.55, y: 1.85, w: 5.9, h: 3.93, shadow: shadow() });
  s.addText('会规划的行程助手，随时候命的客服', { x: 6.95, y: 1.9, w: 5.9, h: 0.4, fontSize: 17, fontFace: FONT, bold: true, color: PRIMARY_DK, margin: 0 });
  const items = [
    ['AI 行程生成（UC11）', '输入目的地、天数与偏好，生成可保存、可调整的逐日行程'],
    ['AI 客服多轮对话（UC12）', '订单查询、退改政策、平台使用问题的一站式应答'],
    ['通知与私信（UC13）', '订单状态、候补兑现实时触达；用户间站内私信'],
  ];
  items.forEach(([t, d], i) => {
    const y = 2.5 + i * 1.25;
    s.addText(t, { x: 6.95, y, w: 5.8, h: 0.38, fontSize: 15, fontFace: FONT, bold: true, color: PRIMARY, margin: 0 });
    s.addText(d, { x: 6.95, y: y + 0.4, w: 5.7, h: 0.7, fontSize: 13, fontFace: FONT, color: TEXT, lineSpacing: 19, margin: 0 });
    if (i < 2) s.addShape(pres.shapes.LINE, { x: 6.95, y: y + 1.08, w: 5.6, h: 0, line: { color: 'D8E4E2', width: 1 } });
  });
}

/* ---------------- S8 亮点③ 社区生态 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 8);
  header(s, 'HIGHLIGHT 03', '社区生态：内容生产与安全并重');
  s.addImage({ path: A('09_社区内容审核流程图.png'), x: 9.55, y: 1.7, w: 3.28, h: 4.63, shadow: shadow() });
  const items = [
    ['游记发布与审核（UC14）', '发布、编辑、删除全生命周期，机审 + 人审双通道'],
    ['评价与举报（UC09）', '订单绑定评价防刷分，举报进入处理工作流'],
    ['互动与关注（UC15 · 17）', '点赞、收藏、评论与关注关系，构建用户主页'],
    ['内容安全（UC19）', '敏感词过滤、审核留痕、全链路可观测性'],
  ];
  items.forEach(([t, d], i) => {
    const y = 1.95 + i * 1.12;
    s.addText(t, { x: M, y, w: 8.5, h: 0.38, fontSize: 15, fontFace: FONT, bold: true, color: PRIMARY, margin: 0 });
    s.addText(d, { x: M, y: y + 0.38, w: 8.4, h: 0.4, fontSize: 13, fontFace: FONT, color: TEXT, margin: 0 });
    if (i < 3) s.addShape(pres.shapes.LINE, { x: M, y: y + 0.95, w: 8.6, h: 0, line: { color: 'D8E4E2', width: 1 } });
  });
  s.addText('审核流程：发布 → 机器初审（敏感词）→ 人工复核 → 上架 / 驳回，全程留痕可审计。',
    { x: M, y: 6.5, w: 8.7, h: 0.4, fontSize: 12.5, fontFace: FONT, color: MUTED, margin: 0 });
}

/* ---------------- S9 微服务架构 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 9);
  header(s, 'MICROSERVICES', '按业务能力划分的六个目标服务');
  s.addImage({ path: A('svc.png'), x: 0.55, y: 1.75, w: 6.5, h: 5.04, shadow: shadow() });
  const svcs = [
    ['SVC-IDENTITY', '身份与用户', '已落地'],
    ['SVC-TRAFFIC', '交通与订单', '已落地'],
    ['SVC-LOCAL', '酒店景点与权益', '已落地'],
    ['SVC-AI', 'AI 与消息', '已落地'],
    ['SVC-COMMUNITY', '社区内容', '边界冻结'],
    ['SVC-OPS', '运营管理', '边界冻结'],
  ];
  svcs.forEach(([code, name, st], i) => {
    const y = 1.85 + i * 0.82;
    s.addText([
      { text: code + '  ', options: { bold: true, color: ACCENT, fontSize: 12 } },
      { text: name, options: { bold: true, color: TEXT, fontSize: 14.5 } },
    ], { x: 7.5, y, w: 3.6, h: 0.35, fontFace: FONT, margin: 0 });
    s.addText(st, { x: 11.35, y: y + 0.02, w: 1.45, h: 0.32, fontSize: 11, fontFace: FONT, bold: true, color: st === '已落地' ? PRIMARY : MUTED, align: 'right', margin: 0 });
    if (i < 5) s.addShape(pres.shapes.LINE, { x: 7.5, y: y + 0.6, w: 5.3, h: 0, line: { color: 'D8E4E2', width: 1 } });
  });
  s.addText('原则：一表一主，跨服务只能通过接口或领域事件访问数据，Outbox 保证业务与事件同事务。',
    { x: 7.5, y: 6.75, w: 5.4, h: 0.6, fontSize: 11.5, fontFace: FONT, color: MUTED, lineSpacing: 17, margin: 0 });
}

/* ---------------- S10 质量保障 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 10);
  header(s, 'QUALITY ASSURANCE', '测试金字塔 + 全链路追溯门禁');
  const stats = [
    ['161/161', '后端单元与集成测试', '34 个测试类，真实 MySQL 环境'],
    ['24/24', '前端单元测试', '3 个测试文件，流水线自动执行'],
    ['19/19', '业务场景全覆盖', '真实后端 E2E，0 partial / 0 planned'],
  ];
  stats.forEach(([n, t, d], i) => {
    const x = M + i * 4.28;
    s.addText(n, { x, y: 1.85, w: 4.0, h: 1.0, fontSize: 54, fontFace: FONT, bold: true, color: i === 2 ? ACCENT : PRIMARY, margin: 0 });
    s.addText(t, { x, y: 2.9, w: 4.0, h: 0.4, fontSize: 16, fontFace: FONT, bold: true, color: TEXT, margin: 0 });
    s.addText(d, { x, y: 3.3, w: 3.9, h: 0.4, fontSize: 12, fontFace: FONT, color: MUTED, margin: 0 });
  });
  s.addText('追溯链：每一个需求都能走到测试结果', { x: M, y: 4.35, w: 8, h: 0.4, fontSize: 16, fontFace: FONT, bold: true, color: PRIMARY_DK, margin: 0 });
  const chain = ['REQ', 'UC', 'SYS', 'COMP', 'OBJ', '代码', '测试'];
  const bw = 1.42, bgap = 0.36, bx0 = M, by = 5.05;
  chain.forEach((c, i) => {
    const x = bx0 + i * (bw + bgap);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: by, w: bw, h: 0.62, fill: { color: i === 6 ? ACCENT : TINT2 }, rectRadius: 0.08 });
    s.addText(c, { x, y: by, w: bw, h: 0.62, fontSize: 14, fontFace: FONT, bold: true, color: i === 6 ? 'FFFFFF' : PRIMARY_DK, align: 'center', valign: 'middle', margin: 0 });
    if (i < 6) s.addShape(pres.shapes.RIGHT_ARROW, { x: x + bw + 0.045, y: by + 0.17, w: 0.27, h: 0.28, fill: { color: PRIMARY } });
  });
  s.addText('流水线内置追溯校验脚本：追溯结构或证据不完整，质量门禁直接失败，代码无法合入主分支。',
    { x: M, y: 6.15, w: 12, h: 0.4, fontSize: 13, fontFace: FONT, color: MUTED, margin: 0 });
}

/* ---------------- S11 CI/CD 流水线 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 11);
  header(s, 'CI / CD PIPELINE', '一条流水线，从 git push 到线上健康检查');
  s.addImage({ path: A('28_容器化与CI-CD流水线拓扑图.png'), x: 6.75, y: 1.8, w: 6.08, h: 4.05, shadow: shadow() });
  const steps = ['配置与追溯校验', '后端 / 前端 / 微服务测试', '真实后端 E2E', '镜像构建 + Trivy 扫描', 'GHCR 发布（不可变 digest）', 'K8s 部署 + 健康检查', '交付门禁 delivery gate'];
  steps.forEach((t, i) => {
    const y = 1.82 + i * 0.66;
    s.addShape(pres.shapes.OVAL, { x: M, y: y + 0.02, w: 0.38, h: 0.38, fill: { color: i === 6 ? ACCENT : PRIMARY } });
    s.addText(String(i + 1), { x: M, y: y + 0.02, w: 0.38, h: 0.38, fontSize: 12, fontFace: FONT, bold: true, color: 'FFFFFF', align: 'center', valign: 'middle', margin: 0 });
    s.addText(t, { x: 1.05, y: y + 0.02, w: 5.4, h: 0.4, fontSize: 14, fontFace: FONT, color: TEXT, bold: i === 6, margin: 0 });
  });
  s.addText('任一必需阶段失败，发布与部署即中止；部署使用完整 commit 对应的 digest，绝不使用 latest。',
    { x: 6.85, y: 6.15, w: 6.0, h: 0.7, fontSize: 12, fontFace: FONT, color: MUTED, lineSpacing: 17, margin: 0 });
}

/* ---------------- S12 交付证据 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 12);
  header(s, 'DELIVERY EVIDENCE', '真实运行的成功流水线与线上健康状态');
  s.addImage({ path: A('01-流水线总览成功.png'), x: 0.6, y: 1.95, w: 5.95, h: 3.23, shadow: shadow() });
  s.addImage({ path: A('03-Kubernetes部署与健康检查成功.png'), x: 6.85, y: 1.95, w: 5.95, h: 3.04, shadow: shadow() });
  s.addText('GitHub Actions 全流水线 Success（Run #33154114496，总时长 11 分 44 秒）',
    { x: 0.6, y: 5.35, w: 5.95, h: 0.55, fontSize: 12, fontFace: FONT, color: MUTED, lineSpacing: 17, margin: 0 });
  s.addText('Kubernetes 部署与健康检查通过：前后端各 2/2 Ready，6 个 Pod 全部 Running',
    { x: 6.85, y: 5.35, w: 5.95, h: 0.55, fontSize: 12, fontFace: FONT, color: MUTED, lineSpacing: 17, margin: 0 });
  s.addText([
    { text: '证据可复核：', options: { bold: true, color: PRIMARY_DK } },
    { text: '流水线日志、镜像 digest、K8s 运行状态与测试报告共同构成验收证据链。', options: { color: MUTED } },
  ], { x: 0.6, y: 6.25, w: 12.2, h: 0.4, fontSize: 13, fontFace: FONT, margin: 0 });
}

/* ---------------- S13 安全工程 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 13);
  header(s, 'SECURITY ENGINEERING', '四道安全门，内建在流水线里');
  const gates = [
    ['Gitleaks', '密钥泄露扫描', '提交与全量历史扫描，Token、密码不入库'],
    ['SpotBugs', '后端静态分析', '字节码级缺陷检测，阻断高危问题合入'],
    ['Trivy', '镜像漏洞扫描', '前后端镜像逐层扫描，高危漏洞不放行'],
    ['CodeQL', '代码语义分析', 'GitHub 官方语义引擎，持续追踪注入与漏洞模式'],
  ];
  const cw = 2.92, gap = 0.24, x0 = M, y0 = 1.95;
  gates.forEach(([t, sub, d], i) => {
    const x = x0 + i * (cw + gap);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: y0, w: cw, h: 2.9, fill: { color: TINT }, rectRadius: 0.08, shadow: shadow() });
    s.addText('0' + (i + 1), { x: x + 0.25, y: y0 + 0.25, w: 1.5, h: 0.5, fontSize: 26, fontFace: FONT, bold: true, color: ACCENT, margin: 0 });
    s.addText(t, { x: x + 0.25, y: y0 + 0.85, w: cw - 0.5, h: 0.45, fontSize: 19, fontFace: FONT, bold: true, color: PRIMARY_DK, margin: 0 });
    s.addText(sub, { x: x + 0.25, y: y0 + 1.32, w: cw - 0.5, h: 0.35, fontSize: 12.5, fontFace: FONT, bold: true, color: PRIMARY, margin: 0 });
    s.addText(d, { x: x + 0.25, y: y0 + 1.72, w: cw - 0.5, h: 1.0, fontSize: 11.5, fontFace: FONT, color: MUTED, lineSpacing: 18, margin: 0 });
  });
  s.addText([
    { text: '安全左移：', options: { bold: true, color: PRIMARY_DK } },
    { text: '四道门全部为流水线必需阶段，任何一道不通过，镜像不会发布、应用不会部署。', options: { color: MUTED } },
  ], { x: M, y: 5.35, w: 12, h: 0.4, fontSize: 14, fontFace: FONT, margin: 0 });
}

/* ---------------- S14 项目数据一览 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 14);
  header(s, 'BY THE NUMBERS', '项目全景数据');
  const stats = [
    ['19', '个业务场景', 'UC01-UC19 全部自动化验收'],
    ['185', '个自动化测试', '后端 161 + 前端 24，全部通过'],
    ['60+', '张设计模型图', '系统级 / 组件级 / 对象级三层'],
    ['6', '个目标微服务', '4 个已落地，边界全部冻结'],
    ['100%', '追溯覆盖率', '需求 → 设计 → 代码 → 测试'],
    ["11'44\"", '端到端流水线', '提交到 Kubernetes 部署完成'],
  ];
  const cw = 3.94, ch = 2.2, gap = 0.25;
  stats.forEach(([n, t, d], i) => {
    const x = M + (i % 3) * (cw + gap), y = 1.9 + Math.floor(i / 3) * (ch + 0.3);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y, w: cw, h: ch, fill: { color: i === 4 ? TINT2 : TINT }, rectRadius: 0.08 });
    s.addText(n, { x: x + 0.3, y: y + 0.2, w: cw - 0.6, h: 0.95, fontSize: 48, fontFace: FONT, bold: true, color: i === 4 ? ACCENT : PRIMARY, margin: 0 });
    s.addText(t, { x: x + 0.3, y: y + 1.2, w: cw - 0.6, h: 0.4, fontSize: 16, fontFace: FONT, bold: true, color: TEXT, margin: 0 });
    s.addText(d, { x: x + 0.3, y: y + 1.62, w: cw - 0.6, h: 0.4, fontSize: 11.5, fontFace: FONT, color: MUTED, margin: 0 });
  });
}

/* ---------------- S15 团队 ---------------- */
{
  const s = pres.addSlide();
  pageChrome(s, 15);
  header(s, 'THE TEAM', 'Miracle 团队 · 五人分工');
  const members = [
    ['邹林利', '组长', '大交通票务与订单域'],
    ['莫谨瑞', '组员', '住宿与本地生活域'],
    ['陈一鸿', '组员', 'AI 规划与 Agent'],
    ['杜新诚', '组员', '社区与用户画像'],
    ['李科', '组员', '管理后台与可观测性'],
  ];
  const cw = 2.32, gap = 0.21, x0 = M, y0 = 2.1;
  members.forEach(([n, role, d], i) => {
    const x = x0 + i * (cw + gap);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: y0, w: cw, h: 3.3, fill: { color: TINT }, rectRadius: 0.08 });
    s.addShape(pres.shapes.OVAL, { x: x + (cw - 1.05) / 2, y: y0 + 0.35, w: 1.05, h: 1.05, fill: { color: i === 0 ? ACCENT : PRIMARY } });
    s.addText(n[0], { x: x + (cw - 1.05) / 2, y: y0 + 0.35, w: 1.05, h: 1.05, fontSize: 30, fontFace: FONT, bold: true, color: 'FFFFFF', align: 'center', valign: 'middle', margin: 0 });
    s.addText(n, { x, y: y0 + 1.55, w: cw, h: 0.4, fontSize: 17, fontFace: FONT, bold: true, color: TEXT, align: 'center', margin: 0 });
    s.addText(role, { x, y: y0 + 1.95, w: cw, h: 0.32, fontSize: 12, fontFace: FONT, bold: true, color: i === 0 ? ACCENT : PRIMARY, align: 'center', margin: 0 });
    s.addText(d, { x: x + 0.15, y: y0 + 2.35, w: cw - 0.3, h: 0.75, fontSize: 12, fontFace: FONT, color: MUTED, align: 'center', lineSpacing: 18, margin: 0 });
  });
  s.addText('按业务领域纵向分工：每人端到端负责自己领域的需求、设计、实现、测试与交付。',
    { x: M, y: 5.85, w: 12, h: 0.4, fontSize: 13.5, fontFace: FONT, color: MUTED, align: 'center', margin: 0 });
}

/* ---------------- S16 结尾 ---------------- */
{
  const s = pres.addSlide();
  s.background = { color: BG_DARK };
  s.addText('CLOSING', { x: 0.75, y: 1.9, w: 6, h: 0.35, fontSize: 13, fontFace: FONT, color: ACCENT, bold: true, charSpacing: 3, margin: 0 });
  s.addText('把一条完整的软件工程链路\n真正跑通', { x: 0.75, y: 2.35, w: 9.5, h: 1.9, fontSize: 44, fontFace: FONT, bold: true, color: ON_DARK, lineSpacing: 58, margin: 0 });
  s.addText([
    { text: '从 19 个用例到 185 个自动化测试，从单体到六个微服务边界，', options: { breakLine: true } },
    { text: '从一行提交到 Kubernetes 上的健康检查 —— 每一步都有证据。', options: {} },
  ], { x: 0.75, y: 4.45, w: 9.5, h: 0.9, fontSize: 16, fontFace: FONT, color: ON_DARK_MUTED, lineSpacing: 26, margin: 0 });
  s.addShape(pres.shapes.LINE, { x: 0.78, y: 5.75, w: 1.2, h: 0, line: { color: ACCENT, width: 2.5 } });
  s.addText([
    { text: 'github.com/Miracle060811/Software_engineering', options: { bold: true, color: ON_DARK, fontSize: 15 } },
  ], { x: 0.75, y: 5.92, w: 9, h: 0.4, fontFace: FONT, margin: 0 });
}

pres.writeFile({ fileName: OUT }).then(() => console.log('written:', OUT));
