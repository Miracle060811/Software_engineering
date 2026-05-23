export const FALLBACK_IMAGE = "/images/seed/fallback.svg";

export const localSeedImage = (name, type = "attraction") => {
  const text = String(name || "").toLowerCase();

  const rules = [
    [/北京|故宫|长城|天坛|王府井/, "beijing"],
    [/上海|外滩|东方明珠|豫园|迪士尼|静安/, "shanghai"],
    [/三亚|亚龙湾|蜈支洲|天涯|海棠湾/, "sanya"],
    [/成都|熊猫|都江堰|锦江/, "chengdu"],
    [/杭州|西湖|湖滨/, "hangzhou"],
    [/西安|兵马俑|秦始皇|大唐|钟楼|城墙/, "xian"],
    [/南京|中山陵|夫子庙|秦淮/, "nanjing"],
    [/重庆|洪崖洞|解放碑/, "chongqing"],
    [/桂林|漓江|象鼻山|龙脊/, "guilin"],
    [/青岛|栈桥|崂山/, "qingdao"],
    [/张家界|武陵源|天门山|黄龙洞/, "zhangjiajie"],
    [/黄山|泰山|峨眉|天山|海螺沟|武夷山/, "mountain"],
    [/九寨|纳木错|天池|喀纳斯|黄龙/, "lake"],
    [/鼓浪屿|海岸|海湾|海景|厦门/, "coast"],
    [/拙政园|周庄|宏村|园林/, "garden"],
    [/布达拉|莫高窟|龙门|黄鹤楼|古城/, "temple"],
    [/乐园|迪士尼/, "theme-park"],
    [/熊猫/, "panda"],
  ];

  const matched = rules.find(([pattern]) => pattern.test(text));
  if (matched) return `/images/seed/${matched[1]}.svg`;
  return `/images/seed/${type === "hotel" ? "hotel" : "attraction"}.svg`;
};

export const normalizeImageUrl = (url, fallback = FALLBACK_IMAGE) => {
  if (!url) return fallback;
  return String(url).trim() || fallback;
};
