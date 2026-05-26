const seedAsset = (file) => `${import.meta.env.BASE_URL}images/seed/${file}`;
const apiBaseURL = import.meta.env.VITE_API_BASE_URL?.trim().replace(/\/+$/, "") || "";

export const FALLBACK_IMAGE = seedAsset("fallback.svg");

export const seedImage = (file) => seedAsset(file);

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
  if (matched) return seedAsset(`${matched[1]}.svg`);
  return seedAsset(`${type === "hotel" ? "hotel" : "attraction"}.svg`);
};

const hotelImageRules = [
  [/北京国贸大酒店/, "https://dimg04.c-ctrip.com/images//200l1g000001hgwwk8DB1_R_550_412.jpg"],
  [/上海外滩华尔道夫酒店/, "https://ak-d.tripcdn.com/images/1mc3d12000rs1ln328F37.jpg"],
  [/广州白天鹅宾馆/, "https://ak-d.tripcdn.com/images/1mc6f12000hrejeww92B0.jpg"],
  [/成都锦江宾馆/, "https://dimg04.c-ctrip.com/images/220i1b000001aohx93B7F_R_960_660_R5_D.jpg"],
  [/西安大唐芙蓉园精品酒店/, "https://ak-d.tripcdn.com/images/200w13000000vir8yDBA5_R_960_660_R5_D.jpg"],
  [/三亚亚龙湾万豪度假酒店/, "https://ak-d.tripcdn.com/images/200q050000000ghm177AC.jpg"],
  [/丽江古城铂尔曼大酒店/, "https://ak-d.tripcdn.com/images/1mc0f12000iw62mfx8BDD.jpg"],
  [/杭州西湖喜来登大酒店/, "https://ak-d.tripcdn.com/images/fd/hotel/g4/M08/FC/23/CggYHlX__YOAVc9JAAP-iECj334734_R_960_660_R5_D.jpg"],
  [/重庆解放碑威斯汀酒店/, "https://ak-d.tripcdn.com/images/0220t12000plokdzi4C79_R_960_660_R5_D.jpg"],
  [/北京王府井万豪酒店/, "https://ak-d.tripcdn.com/images/200v14000000w7mnt5A2C.jpg"],
  [/上海静安香格里拉大酒店/, "https://ak-d.tripcdn.com/images/hotel/452000/451368/00f6bba719044a4394311d9aaf47eeb7.jpg"],
  [/厦门悦华酒店/, "https://ak-d.tripcdn.com/images/1mc1f12000b9nz5c03B02_R_960_660_R5_D.jpg"],
  [/桂林香格里拉大酒店/, "https://ak-d.tripcdn.com/images/1mc0m12000aq6mt0h4457.jpg"],
  [/青岛海景花园大酒店/, "https://ak-d.tripcdn.com/images/200m0800000034723792B_R_960_660_R5_D.jpg"],
  [/全季酒店.*成都太古里春熙路/, "https://ak-d.tripcdn.com/images/1mc4r12000repen2v8026.jpg"],
  [/成都太古里春熙美居酒店/, "https://ak-d.tripcdn.com/images/20060v000000jo62xE3EB.jpg"],
  [/成都瑞城名人酒店/, "https://ak-d.tripcdn.com/images/200m1e000001fvyd2AB3D_R_960_660_R5_D.jpg"],
  [/成都东大明宇豪雅饭店/, "https://ak-d.tripcdn.com/images/1mc1712000epu78z56B04_R_960_660_R5_D.jpg"],
  [/南京金陵饭店/, "https://ak-d.tripcdn.com/images/02064120008bk81fz92B1_R_960_660_R5_D.jpg"],
  [/苏州吴宫泛太平洋酒店/, "https://ak-d.tripcdn.com/images/1mc6o12000br4n333F7C1_R_960_660_R5_D.jpg"],
];

const isSeedImageUrl = (url) => String(url || "").includes("/images/seed/");

export const hotelCoverImage = (hotel) => {
  const cover = hotel?.coverImage || hotel?.coverImg;
  if (cover && !isSeedImageUrl(cover)) return cover;

  const name = String(hotel?.name || "");
  const matched = hotelImageRules.find(([pattern]) => pattern.test(name));
  return matched ? matched[1] : localSeedImage(name, "hotel");
};

const stripWrappingQuotes = (value) => String(value || "").trim().replace(/^['"]|['"]$/g, "");

const toBackendAssetUrl = (path) => {
  if (apiBaseURL && /^https?:\/\//i.test(apiBaseURL)) {
    return `${apiBaseURL}${path}`;
  }
  return path;
};

export const normalizeImageUrl = (url, fallback = FALLBACK_IMAGE) => {
  const raw = stripWrappingQuotes(url).replace(/\\/g, "/");
  if (!raw) return fallback;
  if (raw.startsWith("https://commons.wikimedia.org/wiki/File:")) {
    return raw.replace(
      "https://commons.wikimedia.org/wiki/File:",
      "https://commons.wikimedia.org/wiki/Special:Redirect/file/",
    );
  }
  if (/^(https?:)?\/\//i.test(raw) || /^(data|blob):/i.test(raw)) return raw;
  if (raw.startsWith("/uploads/")) return toBackendAssetUrl(raw);
  if (raw.startsWith("uploads/")) return toBackendAssetUrl(`/${raw}`);
  if (raw.startsWith("/images/")) return raw;
  if (raw.startsWith("images/")) return `${import.meta.env.BASE_URL}${raw}`;
  if (raw.startsWith("/")) return raw;
  return raw || fallback;
};

export const parseImageList = (images, fallback = "") => {
  if (!images) return fallback ? [fallback] : [];
  if (Array.isArray(images)) {
    return images.map((item) => normalizeImageUrl(item, fallback)).filter(Boolean);
  }

  const text = String(images).trim();
  if (!text) return fallback ? [fallback] : [];

  if (text.startsWith("[") || text.startsWith("{")) {
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => normalizeImageUrl(item, fallback)).filter(Boolean);
      }
      if (parsed && typeof parsed === "object") {
        return Object.values(parsed).map((item) => normalizeImageUrl(item, fallback)).filter(Boolean);
      }
    } catch (e) {
      // Legacy rows can contain malformed JSON-like image strings; split them below.
    }
  }

  return text
    .replace(/^\[|\]$/g, "")
    .split(/[,;\n]+/)
    .map((item) => normalizeImageUrl(item, fallback))
    .filter(Boolean);
};
