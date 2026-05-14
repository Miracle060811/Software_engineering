const pptxgen = require("pptxgenjs");

const pptx = new pptxgen();
const Shape = pptx.ShapeType;

pptx.layout = "LAYOUT_WIDE";
pptx.author = "GitHub Copilot";
pptx.company = "TravelMate";
pptx.subject = "软件工程课程项目答辩";
pptx.title = "TravelMate 伴游出行旅游平台";
pptx.lang = "zh-CN";
pptx.theme = {
  headFontFace: "Microsoft YaHei",
  bodyFontFace: "Microsoft YaHei",
  lang: "zh-CN",
};

const OUTPUT = "TravelMate-defense-deck-warm.pptx";

const C = {
  bg: "F6F0E8",
  paper: "FFFDFC",
  sand: "E7E8D1",
  clay: "B85042",
  clayDark: "8A4B3E",
  sage: "A7BEAE",
  sageDark: "5F9B8A",
  ink: "2B2A28",
  muted: "6E675F",
  line: "E5D8CA",
  blue: "58A7D7",
  mint: "60BFA7",
  gold: "D9B064",
  coral: "F08E60",
  softMint: "EEF7F4",
  softBlue: "EAF4FB",
  softClay: "F7ECE6",
  softRose: "F8EEE9",
  softSage: "EEF4EE",
};

function bg(slide, color = C.bg) {
  slide.background = { color };
}

function addBox(slide, x, y, w, h, fill, line = C.line, rounded = true) {
  slide.addShape(rounded ? Shape.roundRect : Shape.rect, {
    x,
    y,
    w,
    h,
    fill: { color: fill },
    line: { color: line, pt: 1 },
  });
}

function addRule(slide, x, y, w, h = 0.03, color = C.line) {
  slide.addShape(Shape.rect, {
    x,
    y,
    w,
    h,
    fill: { color },
    line: { color },
  });
}

function addDot(slide, x, y, size, fill) {
  slide.addShape(Shape.ellipse, {
    x,
    y,
    w: size,
    h: size,
    fill: { color: fill },
    line: { color: fill, pt: 1 },
  });
}

function addText(slide, text, x, y, w, h, options = {}) {
  slide.addText(text, {
    x,
    y,
    w,
    h,
    margin: 0.03,
    fontFace: "Microsoft YaHei",
    fontSize: 15,
    color: C.ink,
    valign: "mid",
    fit: "shrink",
    ...options,
  });
}

function addTag(slide, text, x, y, w, fill = C.softClay, color = C.clay, h = 0.28) {
  addBox(slide, x, y, w, h, fill, fill, true);
  addText(slide, text, x, y + 0.01, w, h - 0.02, {
    fontSize: 8.5,
    bold: true,
    color,
    align: "center",
  });
}

function addTitleBlock(slide, kicker, title, subtitle) {
  addTag(slide, kicker, 0.65, 0.34, 1.35, C.softClay, C.clay);
  addText(slide, title, 0.65, 0.72, 7.2, 0.42, {
    fontSize: 24,
    bold: true,
    color: C.ink,
  });
  addText(slide, subtitle, 0.65, 1.12, 11.6, 0.28, {
    fontSize: 10.5,
    color: C.muted,
  });
}

function addPage(slide, page, dark = false) {
  addTag(
    slide,
    String(page).padStart(2, "0"),
    12.1,
    0.35,
    0.55,
    dark ? "E9D6CC" : "FFF7F0",
    dark ? C.clayDark : C.clay
  );
}

function addBulletList(slide, items, x, y, w, color = C.clay, gap = 0.36, fontSize = 10.6) {
  items.forEach((item, idx) => {
    addDot(slide, x, y + idx * gap + 0.09, 0.08, color);
    addText(slide, item, x + 0.16, y + idx * gap, w - 0.18, 0.24, {
      fontSize,
      color: C.ink,
    });
  });
}

function addMetricCard(slide, x, y, w, h, value, label, accent) {
  addBox(slide, x, y, w, h, C.paper, C.line, true);
  addBox(slide, x + 0.18, y + 0.18, 0.42, 0.42, accent, accent, true);
  addText(slide, value, x + 0.76, y + 0.1, w - 0.92, 0.3, {
    fontSize: 17,
    bold: true,
  });
  addText(slide, label, x + 0.76, y + 0.42, w - 0.92, 0.18, {
    fontSize: 8.8,
    color: C.muted,
  });
}

function addStepCard(slide, x, y, w, h, step, title, body, accent) {
  addBox(slide, x, y, w, h, C.paper, C.line, true);
  addDot(slide, x + 0.22, y + 0.2, 0.28, accent);
  addText(slide, String(step), x + 0.22, y + 0.16, 0.28, 0.28, {
    fontSize: 10,
    bold: true,
    color: "FFFFFF",
    align: "center",
  });
  addText(slide, title, x + 0.62, y + 0.16, w - 0.82, 0.24, {
    fontSize: 12,
    bold: true,
  });
  addText(slide, body, x + 0.22, y + 0.52, w - 0.44, h - 0.62, {
    fontSize: 9.5,
    color: C.muted,
  });
}

function drawHomeUI(slide, x, y, w, h) {
  addBox(slide, x, y, w, h, "FFFFFF", C.line, true);
  addBox(slide, x + 0.12, y + 0.12, w - 0.24, 0.44, C.paper, C.paper, true);
  addText(slide, "伴游 TravelMate", x + 0.34, y + 0.16, 2.1, 0.24, {
    fontSize: 15,
    bold: true,
    color: C.blue,
  });
  addTag(slide, "首页", x + w - 4.65, y + 0.16, 0.54, C.sageDark, "FFFFFF");
  const nav = ["社区", "AI规划", "优惠券", "搜索", "登录 / 注册"];
  nav.forEach((item, idx) => {
    addText(slide, item, x + w - 3.82 + idx * 0.7, y + 0.16, 0.66, 0.22, {
      fontSize: 8.4,
      bold: item === "登录 / 注册",
      color: item === "登录 / 注册" ? C.clay : C.ink,
      align: "center",
    });
  });

  addBox(slide, x + 0.32, y + 0.78, w - 0.64, 2.95, C.softMint, C.softMint, true);
  addTag(slide, "AI 驱动的智能旅行规划", x + 5.1, y + 1.02, 1.65, "FFFFFF", C.sageDark);
  slide.addText(
    [
      { text: "探索世界，", options: { color: C.ink } },
      { text: "从这里开始", options: { color: C.blue } },
    ],
    {
      x: x + 3.65,
      y: y + 1.45,
      w: 4.6,
      h: 0.55,
      fontFace: "Microsoft YaHei",
      fontSize: 25,
      bold: true,
      margin: 0.02,
      align: "center",
      fit: "shrink",
    }
  );
  addText(
    slide,
    "机票 · 火车票 · 酒店 · 景点，一站式智慧出行，让每次旅途都精彩",
    x + 3.2,
    y + 2.0,
    5.5,
    0.22,
    {
      fontSize: 10,
      color: C.muted,
      align: "center",
    }
  );

  addBox(slide, x + 1.9, y + 2.35, w - 3.8, 1.55, "FFFFFF", C.line, true);
  addText(slide, "机票", x + 2.18, y + 2.48, 0.45, 0.18, {
    fontSize: 10.5,
    bold: true,
    color: C.sageDark,
  });
  addText(slide, "火车票", x + 2.98, y + 2.48, 0.56, 0.18, {
    fontSize: 10.5,
    bold: true,
  });
  addText(slide, "酒店", x + 3.86, y + 2.48, 0.4, 0.18, {
    fontSize: 10.5,
    bold: true,
  });
  addRule(slide, x + 2.16, y + 2.78, w - 4.32, 0.02, C.line);

  addBox(slide, x + 2.18, y + 3.02, 1.92, 0.48, "FFFFFF", C.line, true);
  addBox(slide, x + 4.34, y + 3.02, 1.92, 0.48, "FFFFFF", C.line, true);
  addBox(slide, x + 6.5, y + 3.02, 1.68, 0.48, "FFFFFF", C.line, true);
  addText(slide, "出发城市", x + 2.34, y + 3.1, 1.3, 0.16, {
    fontSize: 9,
    color: "9A958D",
  });
  addText(slide, "到达城市", x + 4.5, y + 3.1, 1.3, 0.16, {
    fontSize: 9,
    color: "9A958D",
  });
  addText(slide, "出发日期", x + 6.66, y + 3.1, 1.1, 0.16, {
    fontSize: 9,
    color: "9A958D",
  });
  addBox(slide, x + 8.45, y + 3.0, 1.02, 0.52, C.sageDark, C.sageDark, true);
  addText(slide, "搜索", x + 8.45, y + 3.07, 1.02, 0.22, {
    fontSize: 10.5,
    bold: true,
    color: "FFFFFF",
    align: "center",
  });

  addBox(slide, x + 0.72, y + 4.24, w - 1.44, 0.96, "FFFFFF", C.line, true);
  const metrics = [
    ["1,280", "航线覆盖", C.blue],
    ["523,600", "用户信赖", "7C67E8"],
    ["99.9", "出票成功率 (%)", C.coral],
    ["24", "小时客服在线", "#5BC57E"],
  ];
  metrics.forEach((m, idx) => {
    addMetricCard(slide, x + 0.96 + idx * 2.8, y + 4.39, 2.5, 0.64, m[0], m[1], m[2]);
  });
}

function drawAiPlanUI(slide, x, y, w, h) {
  addBox(slide, x, y, w, h, "FFFFFF", C.line, true);
  const leftW = 3.2;
  const rightX = x + leftW + 0.22;
  const rightW = w - leftW - 0.34;

  addBox(slide, x + 0.15, y + 0.16, leftW - 0.15, h - 0.32, C.paper, C.line, true);
  addText(slide, "AI 行程规划", x + 0.36, y + 0.28, leftW - 0.55, 0.24, {
    fontSize: 16,
    bold: true,
  });

  function field(label, value, yy, hh = 0.42) {
    addText(slide, label, x + 0.36, yy, 1.0, 0.16, { fontSize: 9.3, color: C.ink });
    addBox(slide, x + 0.36, yy + 0.18, leftW - 0.72, hh, "FFFFFF", C.line, true);
    addText(slide, value, x + 0.5, yy + 0.23, leftW - 1.0, hh - 0.08, {
      fontSize: 10,
      color: C.muted,
    });
  }

  field("目的地", "云南大理", y + 0.7);
  addText(slide, "出行天数", x + 0.36, y + 1.4, 1.0, 0.16, { fontSize: 9.3 });
  addBox(slide, x + 0.36, y + 1.58, 0.28, 0.34, "FFFFFF", C.line, true);
  addBox(slide, x + 0.64, y + 1.58, 1.92, 0.34, "FFFFFF", C.line, false);
  addBox(slide, x + 2.56, y + 1.58, 0.28, 0.34, "FFFFFF", C.line, true);
  addText(slide, "-", x + 0.36, y + 1.6, 0.28, 0.2, { fontSize: 13, align: "center", color: C.muted });
  addText(slide, "4", x + 0.64, y + 1.6, 1.92, 0.2, { fontSize: 10.5, align: "center", color: C.ink });
  addText(slide, "+", x + 2.56, y + 1.6, 0.28, 0.2, { fontSize: 13, align: "center", color: C.muted });

  addText(slide, "出行人数", x + 0.36, y + 2.02, 1.0, 0.16, { fontSize: 9.3 });
  addBox(slide, x + 0.36, y + 2.2, 0.28, 0.34, "FFFFFF", C.line, true);
  addBox(slide, x + 0.64, y + 2.2, 1.92, 0.34, "FFFFFF", C.line, false);
  addBox(slide, x + 2.56, y + 2.2, 0.28, 0.34, "FFFFFF", C.line, true);
  addText(slide, "-", x + 0.36, y + 2.22, 0.28, 0.2, { fontSize: 13, align: "center", color: C.muted });
  addText(slide, "5", x + 0.64, y + 2.22, 1.92, 0.2, { fontSize: 10.5, align: "center", color: C.ink });
  addText(slide, "+", x + 2.56, y + 2.22, 0.28, 0.2, { fontSize: 13, align: "center", color: C.muted });

  field("预算（元）", "50000", y + 2.62);
  field("出发日期", "2026-04-28", y + 3.32);

  addText(slide, "出行偏好", x + 0.36, y + 4.0, 1.0, 0.16, { fontSize: 9.3 });
  const chips = [
    ["文化历史", C.sageDark, "FFFFFF"],
    ["自然风光", "FFFFFF", C.muted],
    ["美食体验", C.sageDark, "FFFFFF"],
    ["购物娱乐", "FFFFFF", C.muted],
    ["亲子游", C.sageDark, "FFFFFF"],
  ];
  chips.forEach((chip, idx) => {
    const cx = x + 0.36 + (idx % 3) * 0.84;
    const cy = y + 4.23 + Math.floor(idx / 3) * 0.32;
    addTag(slide, chip[0], cx, cy, 0.72, chip[1], chip[2], 0.24);
  });

  addBox(slide, x + 0.36, y + 4.95, leftW - 0.72, 0.34, C.sageDark, C.sageDark, true);
  addText(slide, "生成行程", x + 0.36, y + 5.0, leftW - 0.72, 0.18, {
    fontSize: 10,
    bold: true,
    color: "FFFFFF",
    align: "center",
  });

  addBox(slide, x + 0.36, y + 5.42, leftW - 0.72, 0.62, "FFFFFF", C.line, true);
  addText(slide, "历史行程", x + 0.5, y + 5.5, 0.9, 0.18, { fontSize: 10.5, bold: true });
  addText(
    slide,
    "云南大理4天文化历史亲子美食之旅",
    x + 0.5,
    y + 5.73,
    leftW - 1.0,
    0.18,
    { fontSize: 8.8, color: C.muted }
  );

  addBox(slide, rightX, y + 0.16, rightW - 0.16, 0.9, "FFFFFF", C.line, true);
  addText(
    slide,
    "云南大理4天文化历史亲子美食之旅",
    rightX + 0.24,
    y + 0.28,
    rightW - 0.46,
    0.24,
    { fontSize: 17, bold: true }
  );
  addText(
    slide,
    "深度体验大理古城、洱海、崇圣寺三塔等文化景点，品尝白族美食，参与扎染等亲子活动，享受美食与自然风光。",
    rightX + 0.24,
    y + 0.52,
    rightW - 0.5,
    0.16,
    { fontSize: 8.8, color: C.muted }
  );
  addTag(slide, "总预估费用：¥2510", rightX + 0.24, y + 0.76, 1.34, C.softSage, C.sageDark, 0.24);
  addTag(slide, "导出行程", rightX + 1.78, y + 0.76, 0.86, C.softMint, C.sageDark, 0.24);
  addTag(slide, "查看关联订单", rightX + 2.78, y + 0.76, 1.12, C.softBlue, "#6C78D5", 0.24);

  addBox(slide, rightX, y + 1.24, rightW - 0.16, h - 1.4, "FFFFFF", C.line, true);
  addText(slide, "第 1 天  大理古城文化探访", rightX + 0.22, y + 1.38, 2.8, 0.18, {
    fontSize: 12,
    bold: true,
    color: C.sageDark,
  });

  const lineX = rightX + 0.46;
  addShapeRect(slide, lineX, y + 1.84, 0.02, 2.95, C.line);

  const events = [
    ["09:00", "抵达大理站", "乘坐高铁抵达大理站，出站后打车或乘坐景区直通车前往古城，车程约40分钟，费用约50元。", "预估费用：¥50"],
    ["11:00", "大理古城漫步", "从南门进入古城，游览文献楼、洋人街、人民路，欣赏白族建筑，感受古城历史氛围。", ""],
    ["12:30", "再回首凉鸡米线", "品尝大理著名小吃凉鸡米线，推荐人民路再回首店，人均15元，酸辣爽口。", "预估费用：¥30"],
    ["14:00", "洱海生态廊道", "下午前往洱海生态廊道骑行，体验湖岸风景与亲子休闲时光，适合家庭同行。", "预估费用：¥40"],
  ];

  events.forEach((e, idx) => {
    const ey = y + 1.78 + idx * 0.72;
    addDot(slide, lineX - 0.045, ey + 0.1, 0.1, C.sageDark);
    addText(slide, e[0], rightX + 0.72, ey + 0.04, 0.5, 0.16, {
      fontSize: 8.8,
      color: C.muted,
    });
    addBox(slide, rightX + 1.15, ey, rightW - 1.45, 0.56, "FFFFFF", C.line, true);
    addText(slide, e[1], rightX + 1.35, ey + 0.08, rightW - 1.85, 0.16, {
      fontSize: 10.3,
      bold: true,
    });
    addText(slide, e[2], rightX + 1.35, ey + 0.24, rightW - 1.88, 0.16, {
      fontSize: 8.3,
      color: C.muted,
    });
    if (e[3]) {
      addText(slide, e[3], rightX + 1.35, ey + 0.4, 1.0, 0.12, {
        fontSize: 8.1,
        color: C.coral,
      });
    }
  });
}

function addShapeRect(slide, x, y, w, h, fill) {
  slide.addShape(Shape.rect, {
    x,
    y,
    w,
    h,
    fill: { color: fill },
    line: { color: fill, pt: 1 },
  });
}

function drawCommunityBoard(slide, x, y, w, h) {
  addBox(slide, x, y, w, h, "FFFFFF", C.line, true);

  const postCards = [
    [x + 0.25, y + 0.26, 2.5, 1.62, "北京三日游｜故宫+长城+颐和园", "1.2k 点赞 · 86 评论", C.softBlue],
    [x + 0.45, y + 2.05, 2.2, 1.34, "西安两日夜游攻略", "864 点赞 · 43 收藏", C.softClay],
    [x + 2.88, y + 0.6, 2.3, 1.48, "成都慢生活美食地图", "998 点赞 · 61 评论", C.softSage],
  ];

  postCards.forEach((p) => {
    addBox(slide, p[0], p[1], p[2], p[3], C.paper, C.line, true);
    addBox(slide, p[0] + 0.12, p[1] + 0.12, p[2] - 0.24, p[3] * 0.48, p[4], p[4], true);
    addText(slide, p[5], p[0] + 0.18, p[1] + p[3] - 0.28, p[2] - 0.36, 0.12, {
      fontSize: 8.1,
      color: C.muted,
    });
    addText(slide, p[4] === C.softBlue ? "目的地游记" : p[4] === C.softClay ? "城市微旅行" : "美食体验", p[0] + 0.18, p[1] + 0.94, 0.72, 0.14, {
      fontSize: 7.8,
      color: C.clay,
      bold: true,
    });
    addText(slide, p[4] === C.softBlue ? "北京三日游｜故宫+长城+颐和园" : p[4] === C.softClay ? "西安两日夜游攻略" : "成都慢生活美食地图", p[0] + 0.18, p[1] + 1.1, p[2] - 0.36, 0.2, {
      fontSize: 9.4,
      bold: true,
    });
  });

  addBox(slide, x + 5.5, y + 0.26, 2.95, 2.02, "FFFFFF", C.line, true);
  addText(slide, "社区与用户中心", x + 5.72, y + 0.38, 2.4, 0.22, {
    fontSize: 16,
    bold: true,
  });
  addBulletList(
    slide,
    [
      "游记发布、图片上传、标签与可见范围",
      "点赞、收藏、评论、关注与粉丝关系",
      "支持保存草稿，兼顾创作与沉淀",
      "通知中心与个人资料管理形成闭环",
      "敏感词检测保障内容合规",
    ],
    x + 5.78,
    y + 0.72,
    2.3,
    C.clay,
    0.32,
    9.3
  );

  addBox(slide, x + 5.5, y + 2.46, 2.95, 1.48, "FFFFFF", C.line, true);
  addText(slide, "用户画像", x + 5.72, y + 2.6, 0.9, 0.18, {
    fontSize: 11.5,
    bold: true,
  });
  addBox(slide, x + 5.74, y + 2.92, 0.58, 0.58, C.softBlue, C.softBlue, true);
  addText(slide, "杜新诚", x + 6.46, y + 2.96, 1.0, 0.18, {
    fontSize: 10.5,
    bold: true,
  });
  addText(slide, "帖子 18 · 关注 152 · 粉丝 93", x + 6.46, y + 3.16, 1.6, 0.16, {
    fontSize: 8.4,
    color: C.muted,
  });
  addTag(slide, "通知中心", x + 5.74, y + 3.48, 0.74, C.softMint, C.sageDark, 0.24);
  addTag(slide, "资料编辑", x + 6.62, y + 3.48, 0.72, C.softBlue, C.blue, 0.24);
  addTag(slide, "草稿箱", x + 7.46, y + 3.48, 0.64, C.softClay, C.clay, 0.24);
}

function drawAdminScreen(slide, x, y, w, h) {
  addBox(slide, x, y, w, h, "FFFFFF", C.line, true);
  addBox(slide, x + 0.12, y + 0.12, 2.0, h - 0.24, C.sageDark, C.sageDark, true);
  addText(slide, "Admin Dashboard", x + 0.34, y + 0.3, 1.4, 0.24, {
    fontSize: 14,
    bold: true,
    color: "FFFFFF",
  });

  const menus = ["仪表盘", "航班管理", "酒店与房态", "优惠券", "订单流水", "内容审核", "敏感词", "日志", "用户"];
  menus.forEach((m, idx) => {
    addText(slide, m, x + 0.38, y + 0.78 + idx * 0.44, 1.3, 0.18, {
      fontSize: 9.3,
      color: idx === 0 ? "FFFFFF" : "EAF3EE",
      bold: idx === 0,
    });
  });

  const mainX = x + 2.34;
  addText(slide, "管理后台与可观测性", mainX + 0.08, y + 0.28, 2.8, 0.2, {
    fontSize: 15.5,
    bold: true,
  });

  const top = [
    ["总用户", "523,600", C.blue],
    ["总订单", "128,420", C.clay],
    ["今日订单", "1,836", C.mint],
    ["待审核内容", "32", C.gold],
  ];
  top.forEach((item, idx) => {
    addMetricCard(slide, mainX + idx * 2.36, y + 0.62, 2.15, 0.74, item[1], item[0], item[2]);
  });

  const chartX = [mainX, mainX + 2.9, mainX + 5.8, mainX + 8.7];
  chartX.forEach((cx, idx) => {
    addBox(slide, cx, y + 1.62, 2.55, 1.48, "FFFFFF", C.line, true);
  });

  addText(slide, "订单趋势", chartX[0] + 0.16, y + 1.76, 1.0, 0.18, {
    fontSize: 10.5,
    bold: true,
  });
  [0.32, 0.58, 0.86, 0.52, 1.0, 0.72].forEach((bar, idx) => {
    addShapeRect(slide, chartX[0] + 0.24 + idx * 0.34, y + 2.72 - bar, 0.16, bar, idx % 2 ? C.clay : C.sageDark);
  });

  addText(slide, "类型分布", chartX[1] + 0.16, y + 1.76, 1.0, 0.18, {
    fontSize: 10.5,
    bold: true,
  });
  addBox(slide, chartX[1] + 0.28, y + 2.1, 0.68, 0.68, C.softBlue, C.softBlue, true);
  addBox(slide, chartX[1] + 1.06, y + 2.1, 0.46, 0.46, C.softClay, C.softClay, true);
  addBox(slide, chartX[1] + 1.62, y + 2.1, 0.3, 0.3, C.softSage, C.softSage, true);
  addText(slide, "航班 42%", chartX[1] + 0.22, y + 2.88, 0.7, 0.12, { fontSize: 7.8, color: C.muted });
  addText(slide, "酒店 33%", chartX[1] + 1.0, y + 2.88, 0.7, 0.12, { fontSize: 7.8, color: C.muted });
  addText(slide, "火车 25%", chartX[1] + 1.56, y + 2.88, 0.7, 0.12, { fontSize: 7.8, color: C.muted });

  addText(slide, "QPS / 延迟", chartX[2] + 0.16, y + 1.76, 1.0, 0.18, {
    fontSize: 10.5,
    bold: true,
  });
  [0.46, 0.52, 0.68, 0.58, 0.72, 0.64].forEach((p, idx) => {
    addDot(slide, chartX[2] + 0.3 + idx * 0.32, y + 2.58 - p, 0.08, C.sageDark);
    if (idx < 5) addShapeRect(slide, chartX[2] + 0.34 + idx * 0.32, y + 2.6 - Math.max(p, [0.46, 0.52, 0.68, 0.58, 0.72, 0.64][idx + 1]), 0.18, 0.02, C.sageDark);
  });
  addText(slide, "当前 QPS 18.2 / 延迟 86ms", chartX[2] + 0.16, y + 2.88, 1.7, 0.12, {
    fontSize: 7.8,
    color: C.muted,
  });

  addText(slide, "告警看板", chartX[3] + 0.16, y + 1.76, 1.0, 0.18, {
    fontSize: 10.5,
    bold: true,
  });
  addTag(slide, "低库存房型 6", chartX[3] + 0.16, y + 2.1, 0.84, C.softClay, C.clay, 0.24);
  addTag(slide, "失败日志 3", chartX[3] + 1.08, y + 2.1, 0.72, C.softBlue, C.blue, 0.24);
  addTag(slide, "待审核内容 32", chartX[3] + 0.16, y + 2.44, 0.96, C.softSage, C.sageDark, 0.24);

  const panelY = y + 3.3;
  addBox(slide, mainX, panelY, 5.1, 1.56, "FFFFFF", C.line, true);
  addText(slide, "资源管理", mainX + 0.16, panelY + 0.16, 1.0, 0.18, {
    fontSize: 11.5,
    bold: true,
  });
  addBulletList(
    slide,
    [
      "航班 CRUD、酒店 CRUD、房态库存和价格调整",
      "优惠券新增、编辑、删除，支持满减券 / 折扣券",
      "订单流水分页查看，交通订单退款审批",
    ],
    mainX + 0.18,
    panelY + 0.46,
    4.5,
    C.clay,
    0.32,
    9
  );

  addBox(slide, mainX + 5.34, panelY, 5.86, 1.56, "FFFFFF", C.line, true);
  addText(slide, "内容与系统治理", mainX + 5.5, panelY + 0.16, 1.2, 0.18, {
    fontSize: 11.5,
    bold: true,
  });
  addBulletList(
    slide,
    [
      "游记审核、评价举报工单、用户启用 / 禁用",
      "敏感词管理、操作日志分页、用户画像侧边查看",
      "QPS、延迟、告警卡片用于值班观察和答辩展示",
    ],
    mainX + 5.52,
    panelY + 0.46,
    5.0,
    C.sageDark,
    0.32,
    9
  );
}

function teamCard(slide, x, y, w, h, owner, title, bullets, accent) {
  addBox(slide, x, y, w, h, C.paper, C.line, true);
  addBox(slide, x, y, w, 0.16, accent, accent, true);
  addText(slide, owner, x + 0.16, y + 0.22, w - 0.32, 0.2, {
    fontSize: 9.5,
    color: C.muted,
  });
  addText(slide, title, x + 0.16, y + 0.46, w - 0.32, 0.3, {
    fontSize: 12,
    bold: true,
  });
  addBulletList(slide, bullets, x + 0.16, y + 0.86, w - 0.28, accent, 0.34, 8.9);
}

async function build() {
  let slide;

  slide = pptx.addSlide();
  bg(slide, C.clayDark);
  addPage(slide, 1, true);
  addDot(slide, 8.65, 0.92, 2.4, "A6604F");
  addDot(slide, 10.7, 1.52, 1.4, "A6604F");
  addDot(slide, 9.32, 4.9, 1.8, "9C5B4B");
  addText(slide, "TravelMate 伴游\n出行旅游平台", 0.78, 1.05, 4.8, 1.1, {
    fontSize: 28,
    bold: true,
    color: "FFF8F2",
    breakLine: true,
  });
  addText(slide, "软件工程课程项目答辩", 0.8, 2.26, 2.8, 0.22, {
    fontSize: 12,
    color: "F3E7DD",
  });
  addText(
    slide,
    "围绕“行前规划 · 行中预订 · 行后分享”构建的一站式智慧旅游平台",
    0.8,
    2.58,
    4.8,
    0.3,
    {
      fontSize: 10.5,
      color: "F0DFD2",
    }
  );
  addTag(slide, "AI 规划", 0.82, 3.08, 0.78, "A96656", "FFF8F2", 0.26);
  addTag(slide, "票务预订", 1.76, 3.08, 0.94, "A96656", "FFF8F2", 0.26);
  addTag(slide, "社区互动", 2.86, 3.08, 0.94, "A96656", "FFF8F2", 0.26);
  addTag(slide, "管理后台", 3.96, 3.08, 0.94, "A96656", "FFF8F2", 0.26);
  addText(slide, "Miracle 开发小组", 0.84, 6.2, 2.4, 0.2, {
    fontSize: 11.2,
    color: "FFF4EC",
    bold: true,
  });
  addText(slide, "2026 年 5 月 13 日", 0.84, 6.48, 2.2, 0.18, {
    fontSize: 9.5,
    color: "EED6C6",
  });

  addBox(slide, 7.55, 0.94, 4.6, 5.25, "FDF8F4", "D7C0B2", true);
  addBox(slide, 7.84, 1.25, 4.02, 0.52, "FFFDFC", "FFFDFC", true);
  addText(slide, "伴游 TravelMate", 8.08, 1.4, 1.8, 0.18, {
    fontSize: 13,
    bold: true,
    color: C.blue,
  });
  addTag(slide, "首页", 10.98, 1.38, 0.48, C.sageDark, "FFFFFF", 0.22);
  addText(slide, "社区", 11.6, 1.4, 0.38, 0.16, { fontSize: 7.6, align: "center" });
  addText(slide, "AI规划", 12.02, 1.4, 0.5, 0.16, { fontSize: 7.6, align: "center" });
  addBox(slide, 8.04, 2.0, 3.34, 1.7, C.softMint, C.softMint, true);
  addTag(slide, "AI 驱动的智能旅行规划", 9.16, 2.2, 1.3, "FFFFFF", C.sageDark, 0.22);
  slide.addText(
    [
      { text: "探索世界，", options: { color: C.ink } },
      { text: "从这里开始", options: { color: C.blue } },
    ],
    {
      x: 8.45,
      y: 2.56,
      w: 2.54,
      h: 0.38,
      fontFace: "Microsoft YaHei",
      fontSize: 16,
      bold: true,
      margin: 0.02,
      align: "center",
      fit: "shrink",
    }
  );
  addText(slide, "机票 · 火车票 · 酒店 · 景点", 8.76, 3.0, 1.92, 0.14, {
    fontSize: 7.8,
    color: C.muted,
    align: "center",
  });
  addBox(slide, 8.52, 3.4, 2.56, 0.72, "FFFFFF", C.line, true);
  addText(slide, "机票    火车票    酒店", 8.68, 3.5, 1.66, 0.14, {
    fontSize: 8.1,
    bold: true,
  });
  addRule(slide, 8.66, 3.7, 2.28, 0.02, C.line);
  addBox(slide, 8.7, 3.84, 0.72, 0.18, "FFFFFF", C.line, true);
  addBox(slide, 9.5, 3.84, 0.72, 0.18, "FFFFFF", C.line, true);
  addBox(slide, 10.3, 3.84, 0.62, 0.18, C.sageDark, C.sageDark, true);
  addText(slide, "搜索", 10.3, 3.86, 0.62, 0.12, {
    fontSize: 6.9,
    color: "FFFFFF",
    align: "center",
    bold: true,
  });
  addBox(slide, 8.3, 4.45, 3.08, 0.7, "FFFFFF", C.line, true);
  [
    ["1,280", "航线覆盖", C.blue],
    ["523k", "用户信赖", "7C67E8"],
    ["99.9", "出票成功率", C.coral],
  ].forEach((m, idx) => {
    addMetricCard(slide, 8.5 + idx * 1.02, 4.55, 0.9, 0.48, m[0], m[1], m[2]);
  });
  addText(slide, "暖陶土叙事风格答辩版", 7.92, 5.62, 2.0, 0.16, {
    fontSize: 8.8,
    color: C.clay,
    bold: true,
  });

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 2);
  addTitleBlock(
    slide,
    "UI主界面",
    "从首页开始，统一承接出行入口",
    "这一页对应真实实现界面的视觉重绘，重点展示导航、统一搜索框、热门搜索与数据看板。"
  );
  drawHomeUI(slide, 0.58, 1.52, 12.1, 5.38);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 3);
  addTitleBlock(
    slide,
    "项目背景",
    "为什么要做 TravelMate",
    "项目不只是功能拼接，而是围绕真实用户旅程，尝试把规划、预订、分享与后台治理做成一套完整系统。"
  );
  addStepCard(slide, 0.78, 1.74, 3.8, 1.22, 1, "信息分散", "机票、酒店、攻略和评价分散在多个平台，用户频繁跳转，决策成本高。", C.clay);
  addStepCard(slide, 0.78, 3.12, 3.8, 1.22, 2, "流程割裂", "规划、下单、支付、回看与分享之间缺少连贯体验，难以形成完整闭环。", C.blue);
  addStepCard(slide, 0.78, 4.5, 3.8, 1.22, 3, "工程验证不足", "课程项目常停留在页面演示，我们希望补齐脚本、测试与后台能力。", C.sageDark);

  addBox(slide, 5.1, 2.02, 7.52, 3.86, C.paper, C.line, true);
  addText(slide, "我们的目标", 5.4, 2.24, 1.4, 0.2, {
    fontSize: 16,
    bold: true,
  });
  addStepCard(slide, 5.42, 2.74, 2.08, 1.48, "A", "一站式平台", "统一首页和导航，覆盖票务、酒店、AI、社区、后台。", C.clay);
  addStepCard(slide, 7.82, 2.74, 2.08, 1.48, "B", "模块化协作", "五位成员分工明确，最终在前后端和数据库上完成集成。", C.sageDark);
  addStepCard(slide, 10.22, 2.74, 2.08, 1.48, "C", "可验证交付", "补齐权限、脚本、数据库修复、测试与文档。", C.blue);
  addRule(slide, 6.4, 4.58, 4.8, 0.03, C.line);
  addText(
    slide,
    "我们希望老师看到的不只是“能点开的页面”，而是一套可运行、可修复、可解释的课程项目系统。",
    5.46,
    4.72,
    6.5,
    0.3,
    {
      fontSize: 10.4,
      color: C.muted,
      italic: true,
    }
  );

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 4);
  addTitleBlock(
    slide,
    "总体架构",
    "前后端分离 + 鉴权 + 缓存 + AI 服务",
    "技术架构上既关注业务覆盖，也关注工程稳定性，因此把权限、缓存、降级和脚本化启动都纳入体系。"
  );
  const layers = [
    ["前端交互层", ["Home", "Flight", "Hotel", "AI Plan", "Community", "Admin"], C.softClay, C.clay],
    ["认证与接口层", ["Vue Router", "Axios", "JWT", "Spring Security"], C.softBlue, C.blue],
    ["业务服务层", ["Flight", "Hotel", "AiService", "Post", "Admin"], C.softSage, C.sageDark],
    ["数据与缓存层", ["MySQL", "MyBatis-Plus", "Redis", "Mapper"], C.softRose, C.clayDark],
    ["AI与任务层", ["DeepSeek API", "Fallback", "Notification", "Scheduler"], C.sand, C.gold],
  ];
  layers.forEach((layer, idx) => {
    const yy = 1.74 + idx * 0.8;
    addBox(slide, 0.86, yy, 8.76, 0.58, layer[2], layer[2], true);
    addText(slide, layer[0], 1.08, yy + 0.08, 1.46, 0.18, {
      fontSize: 10.5,
      bold: true,
      color: layer[3],
    });
    layer[1].forEach((item, j) => {
      addTag(slide, item, 2.5 + j * 1.0, yy + 0.16, 0.86, "FFFFFF", C.ink, 0.22);
    });
  });
  addBox(slide, 10.06, 1.78, 2.56, 1.08, C.paper, C.line, true);
  addText(slide, "安全边界", 10.28, 1.94, 1.0, 0.18, { fontSize: 11.5, bold: true });
  addBulletList(slide, ["JWT 鉴权", "精确 GET 放行", "管理员 RBAC"], 10.24, 2.18, 2.0, C.clay, 0.24, 8.8);

  addBox(slide, 10.06, 3.02, 2.56, 1.08, C.paper, C.line, true);
  addText(slide, "一致性控制", 10.28, 3.18, 1.0, 0.18, { fontSize: 11.5, bold: true });
  addBulletList(slide, ["Redis 预减", "乐观锁", "超时回滚"], 10.24, 3.42, 2.0, C.sageDark, 0.24, 8.8);

  addBox(slide, 10.06, 4.26, 2.56, 1.08, C.paper, C.line, true);
  addText(slide, "运行与恢复", 10.28, 4.42, 1.0, 0.18, { fontSize: 11.5, bold: true });
  addBulletList(slide, ["start.ps1", "setup.ps1", "ResetDb 重建"], 10.24, 4.66, 2.0, C.blue, 0.24, 8.8);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 5);
  addTitleBlock(
    slide,
    "团队协作",
    "五个模块并行推进，最终汇聚到同一套系统",
    "分工不是把系统拆散，而是保证每个模块各自可交付、最终可联调。"
  );
  teamCard(slide, 0.66, 1.72, 2.38, 4.84, "成员 A · 邹林利", "大交通票务", ["航班/火车搜索", "订单创建与支付", "价格趋势与旅客管理"], C.clay);
  teamCard(slide, 3.18, 1.72, 2.38, 4.84, "成员 B · 莫谨瑞", "酒店与本地生活", ["酒店搜索与房型", "景点、评价、举报", "库存与房态管理"], C.sageDark);
  teamCard(slide, 5.7, 1.72, 2.38, 4.84, "成员 C · 陈一鸿", "AI 智能规划", ["结构化行程生成", "AI 客服与工具调用", "通知与降级兜底"], C.blue);
  teamCard(slide, 8.22, 1.72, 2.38, 4.84, "成员 D · 杜新诚", "社区与用户中心", ["游记发布与草稿", "点赞评论关注", "通知与用户资料"], C.coral);
  teamCard(slide, 10.74, 1.72, 1.9, 4.84, "成员 E · 李科", "管理后台", ["仪表盘与告警", "用户/日志/敏感词", "房态/优惠券/订单"], C.gold);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 6);
  addTitleBlock(
    slide,
    "用户端闭环",
    "从打开首页到完成分享，业务链路被串成了一次完整旅程",
    "这一页不再按模块展示，而是按用户视角展示平台如何完成“查、订、用、分享”的闭环。"
  );
  addStepCard(slide, 0.72, 2.0, 2.8, 1.56, 1, "统一入口", "首页聚合机票、火车票、酒店与 AI 规划，降低用户第一次进入平台时的选择成本。", C.clay);
  addStepCard(slide, 3.54, 2.0, 2.8, 1.56, 2, "搜索与预订", "用户可完成资源检索、下单、模拟支付和订单回看，核心交易链路可闭环运行。", C.sageDark);
  addStepCard(slide, 6.36, 2.0, 2.8, 1.56, 3, "AI 辅助决策", "AI 规划与旅行助手帮助用户更快完成方案选择与日程组织。", C.blue);
  addStepCard(slide, 9.18, 2.0, 2.8, 1.56, 4, "社区沉淀内容", "游记、评论、关注和通知让平台具备内容生态和用户沉淀能力。", C.coral);
  addRule(slide, 1.2, 4.06, 10.8, 0.03, C.line);
  addMetricCard(slide, 1.2, 4.48, 2.5, 0.82, "首页", "统一触点", C.clay);
  addMetricCard(slide, 4.0, 4.48, 2.5, 0.82, "订单", "交易闭环", C.sageDark);
  addMetricCard(slide, 6.8, 4.48, 2.5, 0.82, "AI", "规划增强", C.blue);
  addMetricCard(slide, 9.6, 4.48, 2.5, 0.82, "社区", "内容留存", C.coral);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 7);
  addTitleBlock(
    slide,
    "大交通票务成果",
    "围绕“能查、能订、能支付、能回看”的核心交易链路完成实现",
    "大交通模块承担了系统最典型的交易流程，也体现了我们在订单与库存控制上的处理方式。"
  );
  addBox(slide, 0.72, 1.7, 4.26, 4.9, C.paper, C.line, true);
  addText(slide, "功能覆盖", 0.96, 1.9, 1.2, 0.2, { fontSize: 16, bold: true });
  addBulletList(
    slide,
    [
      "航班搜索、火车票搜索与筛选",
      "订单创建、模拟支付、取消订单",
      "常用旅客管理与订单回执",
      "历史价格趋势图表展示",
      "优惠券中心与订单抵扣逻辑",
    ],
    0.94,
    2.28,
    3.6,
    C.clay,
    0.38,
    10
  );

  addBox(slide, 5.28, 1.7, 7.3, 2.0, C.paper, C.line, true);
  addText(slide, "订单流程", 5.52, 1.92, 1.0, 0.18, {
    fontSize: 14,
    bold: true,
  });
  const flow = ["搜索资源", "选择乘客", "创建订单", "模拟支付", "订单回看"];
  flow.forEach((item, idx) => {
    addStepCard(slide, 5.56 + idx * 1.38, 2.26, 1.14, 0.92, idx + 1, item, "", idx % 2 ? C.sageDark : C.clay);
  });

  addBox(slide, 5.28, 4.02, 4.1, 2.58, C.paper, C.line, true);
  addText(slide, "价格趋势（示意）", 5.52, 4.22, 1.5, 0.18, {
    fontSize: 13,
    bold: true,
  });
  [0.6, 0.9, 0.72, 1.05, 0.82, 1.18, 0.94].forEach((bar, idx) => {
    addShapeRect(slide, 5.72 + idx * 0.46, 6.04 - bar, 0.22, bar, idx === 6 ? C.clay : C.sageDark);
    addText(slide, ["周一", "周二", "周三", "周四", "周五", "周六", "周日"][idx], 5.64 + idx * 0.46, 6.08, 0.38, 0.12, {
      fontSize: 7.3,
      color: C.muted,
      align: "center",
    });
  });

  addBox(slide, 9.58, 4.02, 3.0, 2.58, C.paper, C.line, true);
  addText(slide, "工程点", 9.82, 4.22, 0.8, 0.18, {
    fontSize: 13,
    bold: true,
  });
  addBulletList(
    slide,
    [
      "订单状态机流转",
      "乐观锁防超卖",
      "旅客信息复用",
      "订单导出与回看",
    ],
    9.8,
    4.54,
    2.4,
    C.blue,
    0.34,
    9.4
  );

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 8);
  addTitleBlock(
    slide,
    "酒店与本地生活",
    "把搜索、预订、评价与举报都放进住宿场景里",
    "这一块不仅完成了资源查询，还覆盖了房态、评价、举报和防超卖等更接近真实业务的问题。"
  );
  addBox(slide, 0.72, 1.72, 4.36, 4.86, C.paper, C.line, true);
  addText(slide, "酒店预订界面（示意）", 0.96, 1.94, 1.8, 0.2, {
    fontSize: 14,
    bold: true,
  });
  addBox(slide, 0.98, 2.26, 3.82, 0.42, "FFFFFF", C.line, true);
  addText(slide, "城市 / 星级 / 价格区间", 1.12, 2.34, 2.2, 0.14, { fontSize: 8.8, color: C.muted });
  addBox(slide, 0.98, 2.86, 3.82, 1.06, "FFFFFF", C.line, true);
  addText(slide, "大理古城伴游酒店", 1.14, 3.02, 1.6, 0.16, { fontSize: 11, bold: true });
  addText(slide, "高分亲子酒店 · 洱海景观 · 距古城 1.2km", 1.14, 3.24, 2.3, 0.14, { fontSize: 8.6, color: C.muted });
  addTag(slide, "双床房", 1.14, 3.54, 0.56, C.softBlue, C.blue, 0.22);
  addTag(slide, "剩余 4 间", 1.82, 3.54, 0.64, C.softClay, C.clay, 0.22);
  addTag(slide, "¥468/晚", 2.6, 3.54, 0.66, C.softSage, C.sageDark, 0.22);

  addBox(slide, 0.98, 4.2, 3.82, 1.92, "FFFFFF", C.line, true);
  addText(slide, "评价与举报", 1.14, 4.38, 1.2, 0.16, { fontSize: 11, bold: true });
  addBulletList(
    slide,
    [
      "支持图文评价与标签",
      "商家回复补齐反馈闭环",
      "用户可提交举报工单",
      "后台可查看并处理结果",
    ],
    1.12,
    4.7,
    3.1,
    C.coral,
    0.3,
    8.9
  );

  addBox(slide, 5.34, 1.72, 7.26, 4.86, C.paper, C.line, true);
  addText(slide, "防超卖机制", 5.58, 1.94, 1.2, 0.2, {
    fontSize: 15,
    bold: true,
  });
  addStepCard(slide, 5.58, 2.36, 1.5, 1.02, 1, "查询房型", "按城市、价格、日期获取可用房型。", C.clay);
  addStepCard(slide, 7.28, 2.36, 1.5, 1.02, 2, "Redis 预减", "先拦截超量请求，减少热点写入冲击。", C.sageDark);
  addStepCard(slide, 8.98, 2.36, 1.5, 1.02, 3, "MySQL 乐观锁", "落库时再次校验库存，确保最终一致性。", C.blue);
  addStepCard(slide, 10.68, 2.36, 1.5, 1.02, 4, "回滚与恢复", "支付失败或超时取消时恢复库存。", C.coral);
  addRule(slide, 5.78, 3.7, 6.16, 0.03, C.line);
  addText(slide, "扩展能力", 5.58, 4.0, 1.0, 0.18, {
    fontSize: 13,
    bold: true,
  });
  addBulletList(
    slide,
    [
      "景点搜索与门票购买",
      "一日游 / 周边游产品展示",
      "房态管理与库存展示为后台联动提供基础",
      "评价标签、举报、回复共同构成内容治理链路",
    ],
    5.58,
    4.34,
    6.2,
    C.sageDark,
    0.36,
    9.6
  );

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 9);
  addTitleBlock(
    slide,
    "AI 行程规划",
    "让“搜索旅行信息”升级为“直接生成出行方案”",
    "这里结合真实实现页面重绘，重点展示结构化规划、多偏好输入、历史行程和导出能力。"
  );
  drawAiPlanUI(slide, 0.56, 1.52, 12.14, 5.58);
  addTag(slide, "结构化输出", 8.88, 1.58, 0.92, C.softBlue, C.blue, 0.22);
  addTag(slide, "历史行程", 9.98, 1.58, 0.84, C.softMint, C.sageDark, 0.22);
  addTag(slide, "导出 / 关联订单", 10.96, 1.58, 1.28, C.softClay, C.clay, 0.22);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 10);
  addTitleBlock(
    slide,
    "AI 客服与工具调用",
    "不仅能聊天，还能联动天气、航班和酒店查询",
    "为了保证 AI 不只是“会说话”，我们把工具调用、上下文延续和降级兜底一起做了进去。"
  );
  addBox(slide, 0.76, 1.8, 3.36, 4.8, C.paper, C.line, true);
  addText(slide, "AI 旅行助手", 1.0, 2.0, 1.3, 0.2, { fontSize: 15, bold: true });
  addBox(slide, 1.0, 2.38, 1.78, 0.48, C.softBlue, C.softBlue, true);
  addText(slide, "明天大理天气怎么样？", 1.14, 2.48, 1.46, 0.18, { fontSize: 9.2, color: C.ink });
  addBox(slide, 2.12, 3.02, 1.72, 0.66, C.softMint, C.softMint, true);
  addText(slide, "明天大理晴转多云，22°C~28°C，适合古城步行和洱海骑行。", 2.24, 3.08, 1.48, 0.42, {
    fontSize: 8.4,
    color: C.ink,
  });
  addBox(slide, 1.0, 3.92, 1.66, 0.48, C.softBlue, C.softBlue, true);
  addText(slide, "帮我查一下北京到上海的航班。", 1.12, 4.02, 1.4, 0.18, { fontSize: 9.0 });
  addBox(slide, 2.02, 4.56, 1.82, 1.06, C.softMint, C.softMint, true);
  addText(slide, "已查询到多班航班，可按时间和价格进一步筛选，也可以继续帮你组合酒店方案。", 2.14, 4.66, 1.58, 0.66, {
    fontSize: 8.4,
    color: C.ink,
  });

  addBox(slide, 4.5, 1.8, 8.12, 4.8, C.paper, C.line, true);
  addText(slide, "工具调用链路", 4.76, 2.04, 1.3, 0.2, { fontSize: 15, bold: true });
  addStepCard(slide, 4.86, 2.46, 1.66, 1.06, 1, "用户问题", "输入天气、航班、酒店或路线等旅行相关问题。", C.clay);
  addStepCard(slide, 6.76, 2.46, 1.66, 1.06, 2, "模型理解", "识别意图并判断是否需要调用工具函数。", C.sageDark);
  addStepCard(slide, 8.66, 2.46, 1.66, 1.06, 3, "工具执行", "调用 get_weather、search_flights、search_hotels。", C.blue);
  addStepCard(slide, 10.56, 2.46, 1.66, 1.06, 4, "生成回复", "把工具返回结果拼回消息上下文，输出最终答案。", C.coral);
  addRule(slide, 4.92, 3.86, 7.0, 0.03, C.line);
  addBox(slide, 4.86, 4.16, 3.52, 1.6, "FFFFFF", C.line, true);
  addText(slide, "对话特性", 5.04, 4.32, 0.9, 0.18, { fontSize: 12, bold: true });
  addBulletList(slide, ["支持多轮上下文", "按 sessionId 保存历史对话", "可继续追问并保留语义"], 5.02, 4.6, 2.9, C.sageDark, 0.32, 9.2);

  addBox(slide, 8.62, 4.16, 3.6, 1.6, "FFFFFF", C.line, true);
  addText(slide, "稳定性设计", 8.8, 4.32, 1.0, 0.18, { fontSize: 12, bold: true });
  addBulletList(slide, ["DeepSeek API 默认 deepseek-v4-flash", "请求失败自动降级为模板 / 兜底回复", "确保答辩和演示阶段功能稳定"], 8.78, 4.6, 3.0, C.clay, 0.32, 9.2);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 11);
  addTitleBlock(
    slide,
    "社区与用户中心",
    "把游记发布、互动关系和通知系统做成内容闭环",
    "社区不是附属页面，而是平台从“交易系统”走向“内容平台”的关键一步。"
  );
  drawCommunityBoard(slide, 0.66, 1.7, 12.0, 4.96);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 12);
  addTitleBlock(
    slide,
    "管理后台与可观测性",
    "从用户侧走到管理侧，系统才真正具备运营能力",
    "这一页展示成员 E 的交付成果：可观测仪表盘、资源管理、内容治理和系统日志。"
  );
  drawAdminScreen(slide, 0.58, 1.52, 12.1, 5.58);

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 13);
  addTitleBlock(
    slide,
    "关键问题与修复",
    "答辩里展示的不只是一堆功能，还包括遇到的问题与修复能力",
    "我们把最关键的三个工程问题拆开说明：安全边界、数据库导入乱码和启动流程稳定性。"
  );
  const cols = [
    {
      x: 0.74,
      accent: C.clay,
      label: "安全边界",
      problem: "SecurityConfig 早期匿名放行过宽，未登录请求可能打到不应公开的接口。",
      fix: "按只读资源精确收敛到 GET 接口，并补了 5 条 SecurityConfig 匿名访问回归测试。",
      result: "社区写接口、酒店私有接口和“我的内容”请求都被正确拦截为 403。",
    },
    {
      x: 4.46,
      accent: C.sageDark,
      label: "数据库乱码",
      problem: "PowerShell 的 Get-Content | mysql 管道会把中文种子数据导成 ?。",
      fix: "重构 setup.ps1，直接查找 mysql.exe 并通过 SOURCE 导入，同时支持 -InitDb -ResetDb。",
      result: "数据库已实际删库重导验证，中文昵称和游记标题恢复正常。",
    },
    {
      x: 8.18,
      accent: C.blue,
      label: "启动流程",
      problem: "环境变量、数据库密码和前后端启动顺序不稳定，容易导致本地运行失败。",
      fix: "完善 start.ps1 与 .env 加载逻辑，等待后端可访问后再启动前端。",
      result: "支持一键启动、DryRun 检查和本地密码继承，联调稳定性明显提升。",
    },
  ];
  cols.forEach((col) => {
    addBox(slide, col.x, 1.9, 3.06, 4.9, C.paper, C.line, true);
    addTag(slide, col.label, col.x + 0.18, 2.08, 0.92, col.accent, "FFFFFF", 0.24);
    addText(slide, "问题", col.x + 0.2, 2.48, 0.6, 0.16, { fontSize: 10.5, bold: true, color: col.accent });
    addText(slide, col.problem, col.x + 0.2, 2.68, 2.66, 0.66, { fontSize: 9.4, color: C.muted });
    addRule(slide, col.x + 0.18, 3.52, 2.7, 0.02, C.line);
    addText(slide, "修复", col.x + 0.2, 3.7, 0.6, 0.16, { fontSize: 10.5, bold: true, color: col.accent });
    addText(slide, col.fix, col.x + 0.2, 3.9, 2.66, 0.76, { fontSize: 9.4, color: C.muted });
    addRule(slide, col.x + 0.18, 4.9, 2.7, 0.02, C.line);
    addText(slide, "结果", col.x + 0.2, 5.08, 0.6, 0.16, { fontSize: 10.5, bold: true, color: col.accent });
    addText(slide, col.result, col.x + 0.2, 5.28, 2.66, 0.76, { fontSize: 9.4, color: C.muted });
  });

  slide = pptx.addSlide();
  bg(slide);
  addPage(slide, 14);
  addTitleBlock(
    slide,
    "测试与验证",
    "我们不是只“演示功能”，而是真实做了回归与验证",
    "这一页把已经明确验证过的结果收敛成答辩时最有说服力的证据。"
  );
  addMetricCard(slide, 0.92, 1.86, 2.6, 0.92, "6", "后端自动化测试通过", C.clay);
  addMetricCard(slide, 3.74, 1.86, 2.6, 0.92, "5", "匿名访问安全回归通过", C.sageDark);
  addMetricCard(slide, 6.56, 1.86, 2.6, 0.92, "1", "前端生产构建通过", C.blue);
  addMetricCard(slide, 9.38, 1.86, 2.6, 0.92, "1", "数据库重导恢复验证完成", C.coral);

  addBox(slide, 0.92, 3.2, 6.12, 3.12, C.paper, C.line, true);
  addText(slide, "验证清单", 1.16, 3.4, 1.1, 0.18, {
    fontSize: 14,
    bold: true,
  });
  const checks = [
    "BackendApplicationTests 显式指向 TravelMateApplication",
    "SecurityConfigTests 验证匿名创建/删除/我的内容访问均被拒绝",
    "前端 npm run build 已成功通过",
    "start.ps1 -DryRun 可输出完整启动流程",
    "setup.ps1 -InitDb -ResetDb 已用于恢复中文种子数据",
  ];
  checks.forEach((c, idx) => {
    addDot(slide, 1.18, 3.84 + idx * 0.42, 0.08, C.sageDark);
    addText(slide, c, 1.34, 3.76 + idx * 0.42, 5.3, 0.18, {
      fontSize: 9.5,
      color: C.ink,
    });
  });

  addBox(slide, 7.34, 3.2, 4.64, 3.12, C.paper, C.line, true);
  addText(slide, "当前仍需继续完善", 7.58, 3.4, 1.5, 0.18, {
    fontSize: 14,
    bold: true,
  });
  addBulletList(
    slide,
    [
      "更深层的并发压测与自动化回归仍可继续加强",
      "QPS / 延迟 / 告警目前为轻量模拟指标，未接入真实 APM",
      "AI 行程与订单、同行人共享等扩展功能可作为后续演进方向",
    ],
    7.56,
    3.78,
    3.9,
    C.clay,
    0.4,
    9.6
  );
  addText(
    slide,
    "但就课程项目答辩而言，当前版本已经具备完整演示链路、工程修复案例和验证证据。",
    7.58,
    5.4,
    3.8,
    0.4,
    {
      fontSize: 9.5,
      color: C.muted,
      italic: true,
    }
  );

  slide = pptx.addSlide();
  bg(slide, C.clayDark);
  addPage(slide, 15, true);
  addText(slide, "我们完成的不只是功能，\n而是一套可运行、可验证、可维护的课程项目系统。", 0.86, 1.02, 6.0, 1.1, {
    fontSize: 24,
    bold: true,
    color: "FFF8F2",
    breakLine: true,
  });
  addText(slide, "TravelMate 伴游出行旅游平台", 0.9, 2.42, 3.2, 0.22, {
    fontSize: 12,
    color: "F1E1D5",
  });
  addBox(slide, 0.92, 3.12, 3.5, 1.18, "A05B4D", "A05B4D", true);
  addText(slide, "业务闭环", 1.16, 3.34, 0.8, 0.18, { fontSize: 12.5, bold: true, color: "FFF7F1" });
  addText(slide, "首页、搜索、下单、AI、社区与后台都能串起来展示。", 1.16, 3.64, 2.9, 0.32, { fontSize: 9.3, color: "F3E5DB" });
  addBox(slide, 4.7, 3.12, 3.5, 1.18, "A05B4D", "A05B4D", true);
  addText(slide, "工程修复", 4.94, 3.34, 0.8, 0.18, { fontSize: 12.5, bold: true, color: "FFF7F1" });
  addText(slide, "安全边界、数据库乱码、一键启动流程都完成了实际修复。", 4.94, 3.64, 2.9, 0.32, { fontSize: 9.3, color: "F3E5DB" });
  addBox(slide, 8.48, 3.12, 3.5, 1.18, "A05B4D", "A05B4D", true);
  addText(slide, "验证证据", 8.72, 3.34, 0.8, 0.18, { fontSize: 12.5, bold: true, color: "FFF7F1" });
  addText(slide, "自动化测试、构建验证、脚本校验和重导恢复都已完成。", 8.72, 3.64, 2.9, 0.32, { fontSize: 9.3, color: "F3E5DB" });
  addText(slide, "Thanks / Q&A", 4.4, 5.4, 4.4, 0.42, {
    fontSize: 28,
    bold: true,
    color: "FFF8F2",
    align: "center",
  });
  addText(slide, "感谢老师聆听，欢迎提问", 4.62, 5.96, 4.0, 0.2, {
    fontSize: 11.5,
    color: "F0DED2",
    align: "center",
  });

  await pptx.writeFile({ fileName: OUTPUT });
  console.log(`PPT generated: ${OUTPUT}`);
}

build().catch((error) => {
  console.error(error);
  process.exit(1);
});