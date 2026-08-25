from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import textwrap


ROOT = Path(__file__).resolve().parent.parent
SOURCE = Path(__file__).resolve().parent
FONT = "C:/Windows/Fonts/msyh.ttc"
FONT_BOLD = "C:/Windows/Fonts/msyhbd.ttc"

COLORS = {
    "ink": "#172033",
    "muted": "#526079",
    "blue": "#2F6FED",
    "blue_soft": "#EAF1FF",
    "green": "#16846B",
    "green_soft": "#E7F6F1",
    "orange": "#D97706",
    "orange_soft": "#FFF3DF",
    "red": "#C2413B",
    "red_soft": "#FDECEC",
    "line": "#9AA8BE",
    "panel": "#F6F8FC",
    "white": "#FFFFFF",
}


def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT, size)


def rounded(draw, box, fill, outline=COLORS["line"], radius=18, width=2):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def center_text(draw, box, text, fnt, fill=COLORS["ink"], spacing=7):
    x1, y1, x2, y2 = box
    lines = text.split("\n")
    heights = [draw.textbbox((0, 0), line, font=fnt)[3] for line in lines]
    total = sum(heights) + spacing * (len(lines) - 1)
    y = y1 + (y2 - y1 - total) / 2
    for line, h in zip(lines, heights):
        w = draw.textbbox((0, 0), line, font=fnt)[2]
        draw.text(((x1 + x2 - w) / 2, y), line, font=fnt, fill=fill)
        y += h + spacing


def header(draw, title, subtitle, width):
    draw.text((80, 52), title, font=font(42, True), fill=COLORS["ink"])
    draw.text((82, 112), subtitle, font=font(23), fill=COLORS["muted"])
    draw.line((80, 158, width - 80, 158), fill=COLORS["blue"], width=4)


def arrow(draw, start, end, color=COLORS["blue"], width=3, dashed=False):
    x1, y1 = start
    x2, y2 = end
    if dashed:
        segments = 18
        for i in range(0, segments, 2):
            a, b = i / segments, min((i + 1) / segments, 1)
            draw.line((x1 + (x2 - x1) * a, y1 + (y2 - y1) * a,
                       x1 + (x2 - x1) * b, y1 + (y2 - y1) * b), fill=color, width=width)
    else:
        draw.line((x1, y1, x2, y2), fill=color, width=width)
    direction = 1 if x2 >= x1 else -1
    draw.polygon([(x2, y2), (x2 - 13 * direction, y2 - 7), (x2 - 13 * direction, y2 + 7)], fill=color)


def draw_sequence(number, uc, title, participants, steps, note):
    width, height = 2200, 1420
    im = Image.new("RGB", (width, height), COLORS["white"])
    d = ImageDraw.Draw(im)
    header(d, f"{uc} {title}——对象级顺序图", "当前代码基线｜独立覆盖一个用例｜异常与状态变化写入交互", width)
    left, right = 100, width - 100
    n = len(participants)
    gap = (right - left) / n
    xs = [left + gap * (i + 0.5) for i in range(n)]
    top_box_y, box_h = 205, 95
    for x, p in zip(xs, participants):
        box = (x - gap * 0.40, top_box_y, x + gap * 0.40, top_box_y + box_h)
        rounded(d, box, COLORS["blue_soft"], COLORS["blue"], 14, 2)
        center_text(d, box, p.replace("/", "/\n"), font(20, True))
        d.line((x, top_box_y + box_h, x, height - 170), fill=COLORS["line"], width=2)

    y = 350
    step_gap = min(92, 780 // max(1, len(steps)))
    for idx, (src, dst, label, kind) in enumerate(steps, 1):
        sx, dx = xs[src], xs[dst]
        color = COLORS["red"] if kind == "error" else COLORS["green"] if kind == "return" else COLORS["blue"]
        arrow(d, (sx, y), (dx, y), color=color, width=3, dashed=(kind == "return"))
        label_lines = textwrap.wrap(f"{idx}. {label}", width=34)
        label_text = "\n".join(label_lines[:2])
        tw = max(d.textbbox((0, 0), line, font=font(18))[2] for line in label_text.split("\n"))
        tx = (sx + dx) / 2 - tw / 2
        d.rectangle((tx - 8, y - 43, tx + tw + 8, y - 5), fill=COLORS["white"])
        d.multiline_text((tx, y - 42), label_text, font=font(18), fill=color, spacing=2, align="center")
        y += step_gap

    note_box = (100, height - 145, width - 100, height - 55)
    rounded(d, note_box, COLORS["orange_soft"], COLORS["orange"], 14, 2)
    d.text((130, height - 120), "设计约束：" + note, font=font(20), fill=COLORS["ink"])
    out = ROOT / f"{number:02d}_{uc}_{title}对象级顺序图.png"
    im.save(out, quality=95)
    write_sequence_mermaid(number, uc, title, participants, steps, note)


def write_sequence_mermaid(number, uc, title, participants, steps, note):
    aliases = [f"P{i}" for i in range(len(participants))]
    lines = ["sequenceDiagram", f"    title {uc} {title}——对象级顺序图"]
    for alias, participant in zip(aliases, participants):
        lines.append(f"    participant {alias} as {participant}")
    for src, dst, label, kind in steps:
        op = "-->>" if kind == "return" else "->>"
        lines.append(f"    {aliases[src]}{op}{aliases[dst]}: {label}")
    lines.append(f"    Note over {aliases[0]},{aliases[-1]}: {note}")
    (SOURCE / f"{number:02d}_{uc}_{title}对象级顺序图.mmd").write_text("\n".join(lines) + "\n", encoding="utf-8")


def draw_class(number, title, classes, relations):
    parsed = []
    auto_index = 0
    for item in classes:
        if len(item) == 5:
            name, stereotype, methods, col, row = item
        else:
            name, stereotype, methods = item
            row, col = divmod(auto_index, 4)
            auto_index += 1
        parsed.append((name, stereotype, methods, col, row))

    max_row = max(item[4] for item in parsed)
    width = 2360
    height = 330 + (max_row + 1) * 235
    im = Image.new("RGB", (width, height), COLORS["white"])
    d = ImageDraw.Draw(im)
    header(d, title, "当前实现 UML｜Controller/Security → Service → Mapper → Entity｜虚线表示横切或校验依赖", width)
    column_titles = ["入口与控制", "业务与安全", "持久化接口", "领域实体"]
    x_positions = [70, 645, 1220, 1795]
    card_w, card_h = 495, 175
    y0, gy = 225, 235
    for x, heading in zip(x_positions, column_titles):
        d.text((x + 4, 178), heading, font=font(22, True), fill=COLORS["blue"])

    boxes = {}
    for name, stereotype, methods, col, row in parsed:
        x, y = x_positions[col], y0 + row * gy
        box = (x, y, x + card_w, y + card_h)
        if stereotype == "Mapper":
            fill, outline = COLORS["green_soft"], COLORS["green"]
        elif stereotype == "Entity":
            fill, outline = COLORS["orange_soft"], COLORS["orange"]
        elif stereotype in ("Security", "Scheduler", "Aspect"):
            fill, outline = COLORS["panel"], COLORS["muted"]
        else:
            fill, outline = COLORS["blue_soft"], COLORS["blue"]
        rounded(d, box, fill, outline, 14, 2)
        d.text((x + 18, y + 10), f"«{stereotype}»", font=font(15), fill=COLORS["muted"])
        name_font = font(21 if len(name) < 27 else 18, True)
        d.text((x + 18, y + 40), name, font=name_font, fill=COLORS["ink"])
        d.line((x + 14, y + 74, x + card_w - 14, y + 74), fill=COLORS["line"], width=2)
        yy = y + 87
        for method in methods[:3]:
            d.text((x + 20, yy), ("- " if stereotype == "Entity" else "+ ") + method, font=font(15), fill=COLORS["ink"])
            yy += 25
        boxes[name] = box

    lane = 0
    for src, dst, label, *style in relations:
        if src not in boxes or dst not in boxes:
            continue
        sb, db = boxes[src], boxes[dst]
        dashed = bool(style and style[0] == "dashed")
        if sb[0] < db[0]:
            start = (sb[2], (sb[1] + sb[3]) / 2)
            end = (db[0], (db[1] + db[3]) / 2)
        elif sb[0] > db[0]:
            start = (sb[0], (sb[1] + sb[3]) / 2)
            end = (db[2], (db[1] + db[3]) / 2)
        else:
            side_x = sb[0] - 28 - (lane % 3) * 16 if sb[0] > 100 else sb[2] + 28 + (lane % 3) * 16
            start = ((sb[0] if sb[0] > 100 else sb[2]), (sb[1] + sb[3]) / 2)
            end = ((db[0] if db[0] > 100 else db[2]), (db[1] + db[3]) / 2)
            color = COLORS["muted"]
            d.line((start[0], start[1], side_x, start[1], side_x, end[1]), fill=color, width=2)
            arrow(d, (side_x, end[1]), end, color=color, width=2, dashed=dashed)
            lane += 1
            continue

        color = COLORS["muted"]
        if abs(start[1] - end[1]) < 4:
            arrow(d, start, end, color=color, width=2, dashed=dashed)
            label_x, label_y = (start[0] + end[0]) / 2, start[1] - 24
        else:
            mid_x = (start[0] + end[0]) / 2 + (lane % 3 - 1) * 14
            d.line((start[0], start[1], mid_x, start[1], mid_x, end[1]), fill=color, width=2)
            arrow(d, (mid_x, end[1]), end, color=color, width=2, dashed=dashed)
            label_x, label_y = mid_x + 6, (start[1] + end[1]) / 2 - 12
            lane += 1
        tw = d.textbbox((0, 0), label, font=font(14))[2]
        d.rectangle((label_x - 4, label_y - 2, label_x + tw + 5, label_y + 19), fill=COLORS["white"])
        d.text((label_x, label_y), label, font=font(14), fill=COLORS["muted"])
    im.save(ROOT / f"{number:02d}_{title}.png", quality=95)
    write_class_mermaid(number, title, [(n, s, m) for n, s, m, _, _ in parsed], relations)


def write_class_mermaid(number, title, classes, relations):
    lines = ["classDiagram", f"    %% {title}"]
    for name, stereotype, methods in classes:
        lines.append(f"    class {name} {{")
        lines.append(f"        <<{stereotype}>>")
        for method in methods[:5]:
            safe = method.replace("(", "_").replace(")", "")
            lines.append(f"        +{safe}")
        lines.append("    }")
    for src, dst, label, *_ in relations:
        lines.append(f"    {src} --> {dst} : {label}")
    (SOURCE / f"{number:02d}_{title}.mmd").write_text("\n".join(lines) + "\n", encoding="utf-8")


def draw_component(number, title, columns, footer):
    width, height = 2200, 1450
    im = Image.new("RGB", (width, height), COLORS["white"])
    d = ImageDraw.Draw(im)
    header(d, title, "当前实现组件图｜只展示仓库中可定位的组件", width)
    col_w = 620
    x_positions = [100, 790, 1480]
    centers = []
    for ci, (heading, items) in enumerate(columns):
        x = x_positions[ci]
        d.text((x, 205), heading, font=font(25, True), fill=COLORS["blue"])
        local = []
        for ri, item in enumerate(items):
            y = 260 + ri * 150
            box = (x, y, x + col_w, y + 105)
            rounded(d, box, COLORS["blue_soft"] if ci < 2 else COLORS["green_soft"], COLORS["blue"] if ci < 2 else COLORS["green"], 15, 2)
            center_text(d, box, item, font(20, True))
            local.append((x + col_w / 2, y + 52))
            if ri > 0:
                arrow(d, (local[ri - 1][0], local[ri - 1][1] + 53), (local[ri][0], local[ri][1] - 53), COLORS["muted"], 2)
        centers.append(local)
    for i in range(2):
        for row in range(min(len(centers[i]), len(centers[i + 1]))):
            arrow(d, (centers[i][row][0] + col_w / 2 - 5, centers[i][row][1]), (centers[i + 1][row][0] - col_w / 2 + 5, centers[i + 1][row][1]), COLORS["blue"], 2)
    note_box = (100, height - 150, width - 100, height - 55)
    rounded(d, note_box, COLORS["orange_soft"], COLORS["orange"], 14, 2)
    d.text((130, height - 122), footer, font=font(20), fill=COLORS["ink"])
    im.save(ROOT / f"{number:02d}_{title}.png", quality=95)


def draw_use_cases():
    width, height = 2200, 1700
    im = Image.new("RGB", (width, height), COLORS["white"])
    d = ImageDraw.Draw(im)
    header(d, "TravelMate 完整系统用例图（候选基线）", "UC01—UC19 与正文编号一致；最终范围需教师/助教确认后冻结", width)
    groups = [
        ("游客/用户", ["UC01 注册登录与账户安全", "UC02 查询并预订航班", "UC03 查询火车票/候补", "UC04 交通订单履约退款", "UC05 搜索酒店并订房", "UC06 酒店订单履约退款", "UC07 景点浏览与购票", "UC08 浏览一日游产品", "UC10 优惠券领取使用", "UC11 AI 行程生成保存", "UC12 AI 客服对话", "UC13 通知与私信", "UC14 游记发布审核", "UC15 点赞收藏评论", "UC16 常用旅客管理", "UC17 用户主页与关注"]),
        ("商家/运营人员", ["UC09 评价回复与举报处理"]),
        ("管理员", ["UC18 资源订单用户管理", "UC19 内容安全与可观测性"]),
    ]
    y = 210
    colors = [(COLORS["blue_soft"], COLORS["blue"]), (COLORS["green_soft"], COLORS["green"]), (COLORS["orange_soft"], COLORS["orange"])]
    for gi, (actor, cases) in enumerate(groups):
        fill, outline = colors[gi]
        d.text((105, y + 14), actor, font=font(27, True), fill=outline)
        cols = 4 if gi == 0 else len(cases)
        for i, case in enumerate(cases):
            row, col = divmod(i, cols)
            x = 360 + col * 445
            yy = y + row * 120
            box = (x, yy, x + 390, yy + 82)
            d.ellipse(box, fill=fill, outline=outline, width=3)
            center_text(d, box, case, font(18, True))
            d.line((260, y + 40, x, yy + 41), fill=COLORS["line"], width=1)
        y += ((len(cases) + cols - 1) // cols) * 120 + 70
    im.save(ROOT / "12_TravelMate完整系统用例图.png", quality=95)


def draw_trace():
    width, height = 2200, 1300
    im = Image.new("RGB", (width, height), COLORS["white"])
    d = ImageDraw.Draw(im)
    header(d, "需求—设计—代码—测试追溯关系", "编号统一采用 REQ / UC / SYS / COMP / OBJ / UNIT / INT / E2E；状态来自证据，不预填通过", width)
    labels = [
        ("REQ-xx\n需求", COLORS["blue_soft"], COLORS["blue"]),
        ("UCxx\n用例", COLORS["blue_soft"], COLORS["blue"]),
        ("SYS-xx\n系统级模型", COLORS["green_soft"], COLORS["green"]),
        ("COMP-xx\n组件级模型", COLORS["green_soft"], COLORS["green"]),
        ("OBJ-xx\n对象级模型", COLORS["green_soft"], COLORS["green"]),
        ("代码路径\nController/Service/Mapper", COLORS["panel"], COLORS["muted"]),
        ("UNIT / INT / E2E\n测试编号", COLORS["orange_soft"], COLORS["orange"]),
        ("实际结果\n报告路径 + commit", COLORS["red_soft"], COLORS["red"]),
    ]
    card_w, card_h, gap = 225, 150, 35
    total = len(labels) * card_w + (len(labels) - 1) * gap
    x = (width - total) / 2
    y = 330
    for i, (label, fill, outline) in enumerate(labels):
        box = (x, y, x + card_w, y + card_h)
        rounded(d, box, fill, outline, 18, 3)
        center_text(d, box, label, font(20, True))
        if i < len(labels) - 1:
            arrow(d, (x + card_w, y + card_h / 2), (x + card_w + gap, y + card_h / 2), COLORS["blue"], 3)
        x += card_w + gap
    checks = [
        "每个 UC 独占一行，禁止把多个用例合并成无法核验的状态",
        "设计列同时记录 SYS、COMP、OBJ 编号；详细设计至少包含实现类图和 OBJ 模型",
        "测试状态只允许：未执行、失败、通过；只有原始报告和提交号齐全才能标记通过",
        "当前文档中的新增测试仍为待补，图中不使用绿色‘已验证’假状态",
    ]
    box = (180, 650, width - 180, 1080)
    rounded(d, box, COLORS["panel"], COLORS["line"], 20, 2)
    d.text((230, 700), "追溯门禁", font=font(30, True), fill=COLORS["ink"])
    yy = 770
    for item in checks:
        d.ellipse((235, yy + 8, 249, yy + 22), fill=COLORS["blue"])
        d.text((270, yy), item, font=font(22), fill=COLORS["ink"])
        yy += 70
    im.save(ROOT / "29_需求设计代码测试追溯图.png", quality=95)


def main():
    draw_use_cases()
    draw_component(20, "社区与用户关系域组件图", [
        ("前端与接口", ["Community.vue / PostCreate.vue", "FileController / PostController", "LikeController / CommentController", "FollowController / UserProfileController"]),
        ("当前业务实现", ["PostServiceImpl", "FileController（本地文件落盘）", "LikeServiceImpl / CommentServiceImpl", "FollowServiceImpl"]),
        ("持久化与辅助", ["PostMapper → tm_post", "本地 uploads 目录", "LikeMapper / CommentMapper", "FollowMapper / UserMapper"]),
    ], "边界说明：当前没有 FileServiceImpl、tm_file、OSS/MinIO、Redis Cluster、WebSocket/FCM 实现。")
    draw_component(24, "管理后台与可观测性域组件图", [
        ("前端与入口", ["AdminDashboard.vue", "AdminController /api/admin/**", "SecurityConfig + JwtFilter", "SysLogAspect"]),
        ("当前协调组件", ["AdminController（资源 CRUD）", "HotelRoomStockService", "NotificationCenterService", "PostAuditScheduler"]),
        ("直接持久化依赖", ["Flight/Train/Hotel Mapper", "Order/User/Coupon Mapper", "Post/Review/SensitiveWord Mapper", "SysLogMapper → sys_log"]),
    ], "边界说明：当前没有独立 ResourceService、OrderService、ContentAuditService、ReportService、LogMetricService。")
    draw_trace()

    class_sets = [
        (30, "身份与用户关系实现类图", [
            ("UserController", "Controller", ["register()", "login()", "changePassword()"], 0, 0),
            ("UserService", "Service", ["register()", "login()", "getUserByUsername()"], 1, 0),
            ("UserMapper", "Mapper", ["selectOne()", "insert()", "updateById()"], 2, 0),
            ("User", "Entity", ["id: Long", "username: String", "role/status: Integer"], 3, 0),
            ("FollowController", "Controller", ["toggleFollow()", "fans()", "following()"], 0, 1),
            ("FollowServiceImpl", "Service", ["toggleFollow()", "fans()", "followStatus()"], 1, 1),
            ("FollowMapper", "Mapper", ["selectOne()", "insert()", "delete()"], 2, 1),
            ("Follow", "Entity", ["followerId: Long", "followeeId: Long", "createTime: LocalDateTime"], 3, 1),
            ("JwtFilter", "Security", ["doFilterInternal()"], 0, 2),
            ("JwtUtil", "Security", ["generateToken()", "validateToken()", "getUsername()"], 1, 2),
        ], [("UserController", "UserService", "调用"), ("UserService", "UserMapper", "读写"), ("UserMapper", "User", "映射"),
            ("FollowController", "FollowServiceImpl", "调用"), ("FollowServiceImpl", "FollowMapper", "读写"), ("FollowMapper", "Follow", "映射"),
            ("UserService", "JwtUtil", "签发 JWT", "dashed"), ("JwtFilter", "JwtUtil", "解析校验", "dashed")]),
        (31, "大交通与订单实现类图", [
            ("FlightController", "Controller", ["searchFlights()", "getFlightById()"], 0, 0),
            ("FlightServiceImpl", "Service", ["searchFlights()", "getById()"], 1, 0),
            ("FlightMapper", "Mapper", ["selectList()", "selectById()", "update()"], 2, 0),
            ("Flight", "Entity", ["flightNo: String", "availableSeats: Integer", "price: BigDecimal"], 3, 0),
            ("TrainController", "Controller", ["searchTrains()", "createWaitlist()"], 0, 1),
            ("TrainServiceImpl", "Service", ["searchTrains()", "getTransferPlan()"], 1, 1),
            ("TrainMapper", "Mapper", ["selectList()", "selectById()", "update()"], 2, 1),
            ("Train", "Entity", ["trainNo: String", "seatInventory: Integer", "depDate: LocalDate"], 3, 1),
            ("TrafficOrderController", "Controller", ["createFlightOrder()", "createTrainOrder()", "mockPay()/cancel()"], 0, 2),
            ("TrafficOrderServiceImpl", "Service", ["createFlightOrder()", "createTrainOrder()", "pay/cancel/refund()"], 1, 2),
            ("TrafficOrderMapper", "Mapper", ["insert()", "selectOne()", "updateById()"], 2, 2),
            ("TrafficOrder", "Entity", ["orderNo: String", "orderType/status: Integer", "userId: Long"], 3, 2),
            ("PassengerController", "Controller", ["list()", "add()", "delete()"], 0, 3),
            ("PassengerServiceImpl", "Service", ["getPassengerList()", "addPassenger()", "deletePassenger()"], 1, 3),
            ("PassengerMapper", "Mapper", ["selectList()", "insert()", "delete()"], 2, 3),
            ("Passenger", "Entity", ["userId: Long", "name: String", "idCard: String"], 3, 3),
            ("OrderTimeoutScheduler", "Scheduler", ["closeExpiredOrders()"], 0, 4),
        ], [("FlightController", "FlightServiceImpl", "查询"), ("FlightServiceImpl", "FlightMapper", "读写"), ("FlightMapper", "Flight", "映射"),
            ("TrainController", "TrainServiceImpl", "查询"), ("TrainServiceImpl", "TrainMapper", "读写"), ("TrainMapper", "Train", "映射"),
            ("TrafficOrderController", "TrafficOrderServiceImpl", "下单/状态命令"), ("TrafficOrderServiceImpl", "TrafficOrderMapper", "读写"), ("TrafficOrderMapper", "TrafficOrder", "映射"),
            ("PassengerController", "PassengerServiceImpl", "维护"), ("PassengerServiceImpl", "PassengerMapper", "读写"), ("PassengerMapper", "Passenger", "映射"),
            ("OrderTimeoutScheduler", "TrafficOrderServiceImpl", "超时关闭", "dashed")]),
        (32, "酒店景点与权益实现类图", [
            ("HotelController", "Controller", ["searchHotels()", "createOrder()", "pay/cancel/refund()"], 0, 0),
            ("HotelOrderServiceImpl", "Service", ["createOrder()", "payOrder()", "cancel/refund()"], 1, 0),
            ("HotelOrderMapper", "Mapper", ["insert()", "selectOne()", "updateById()"], 2, 0),
            ("HotelOrder", "Entity", ["orderNo: String", "roomCount: Integer", "status: Integer"], 3, 0),
            ("HotelRoomStockServiceImpl", "Service", ["preDeductRoom()", "rollbackPreDeduct()", "syncWithDatabase()"], 1, 1),
            ("HotelRoomMapper", "Mapper", ["selectById()", "deductStock()", "updateById()"], 2, 1),
            ("HotelRoom", "Entity", ["hotelId: Long", "availableRooms: Integer", "price: BigDecimal"], 3, 1),
            ("AttractionController", "Controller", ["search()", "buyTicket()", "receipt()"], 0, 2),
            ("AttractionServiceImpl", "Service", ["searchAttractions()", "buyTicket()", "getOrderDetail()"], 1, 2),
            ("AttractionOrderMapper", "Mapper", ["insert()", "selectOne()"], 2, 2),
            ("AttractionOrder", "Entity", ["orderNo: String", "adult/childCount: Integer", "totalAmount: BigDecimal"], 3, 2),
            ("CouponController", "Controller", ["list()", "claim()", "myCoupons()"], 0, 3),
            ("CouponServiceImpl", "Service", ["listAvailable()", "claimCoupon()", "useCoupon()"], 1, 3),
            ("UserCouponMapper", "Mapper", ["selectOne()", "insert()", "updateById()"], 2, 3),
            ("UserCoupon", "Entity", ["userId/couponId: Long", "status: Integer", "usedTime: LocalDateTime"], 3, 3),
            ("ReviewController", "Controller", ["addReview()", "listReviews()"], 0, 4),
            ("ReviewServiceImpl", "Service", ["addReview()", "getReviews()"], 1, 4),
            ("ReviewMapper", "Mapper", ["insert()", "selectList()"], 2, 4),
            ("Review", "Entity", ["targetId/type: Long/Integer", "rating: Integer", "content: String"], 3, 4),
        ], [("HotelController", "HotelOrderServiceImpl", "订单命令"), ("HotelOrderServiceImpl", "HotelOrderMapper", "读写"), ("HotelOrderMapper", "HotelOrder", "映射"),
            ("HotelOrderServiceImpl", "HotelRoomStockServiceImpl", "预减/回补", "dashed"), ("HotelRoomStockServiceImpl", "HotelRoomMapper", "库存更新"), ("HotelRoomMapper", "HotelRoom", "映射"),
            ("AttractionController", "AttractionServiceImpl", "查询/购票"), ("AttractionServiceImpl", "AttractionOrderMapper", "写订单"), ("AttractionOrderMapper", "AttractionOrder", "映射"),
            ("CouponController", "CouponServiceImpl", "领券"), ("CouponServiceImpl", "UserCouponMapper", "读写"), ("UserCouponMapper", "UserCoupon", "映射"),
            ("ReviewController", "ReviewServiceImpl", "评价"), ("ReviewServiceImpl", "ReviewMapper", "读写"), ("ReviewMapper", "Review", "映射")]),
        (33, "AI与消息实现类图", [
            ("AiController", "Controller", ["generatePlan()", "chat()", "listNotifications()"], 0, 0),
            ("AiServiceImpl", "Service", ["generatePlan()", "chat()", "auditPost()"], 1, 0),
            ("AiPlanMapper", "Mapper", ["insert()", "selectList()", "selectById()"], 2, 0),
            ("AiPlan", "Entity", ["userId: Long", "requestJson: String", "planJson: String"], 3, 0),
            ("AiChatMapper", "Mapper", ["insert()", "selectList()"], 2, 1),
            ("AiChat", "Entity", ["userId: Long", "sessionId: String", "content/role: String"], 3, 1),
            ("NotificationCenterServiceImpl", "Service", ["createNotification()", "markRead()", "unreadCount()"], 1, 2),
            ("NotificationMapper", "Mapper", ["insert()", "selectList()", "update()"], 2, 2),
            ("Notification", "Entity", ["userId: Long", "type/title: String", "isRead: Integer"], 3, 2),
            ("PrivateMessageController", "Controller", ["contacts()", "conversation()", "send()"], 0, 3),
            ("PrivateMessageServiceImpl", "Service", ["listContacts()", "listConversation()", "sendMessage()"], 1, 3),
            ("PrivateMessageMapper", "Mapper", ["insert()", "selectList()", "update()"], 2, 3),
            ("PrivateMessage", "Entity", ["sender/receiverId: Long", "content: String", "isRead: Integer"], 3, 3),
        ], [("AiController", "AiServiceImpl", "行程/问答"), ("AiServiceImpl", "AiPlanMapper", "保存计划"), ("AiPlanMapper", "AiPlan", "映射"),
            ("AiServiceImpl", "AiChatMapper", "保存会话"), ("AiChatMapper", "AiChat", "映射"),
            ("AiController", "NotificationCenterServiceImpl", "通知接口"), ("NotificationCenterServiceImpl", "NotificationMapper", "读写"), ("NotificationMapper", "Notification", "映射"),
            ("PrivateMessageController", "PrivateMessageServiceImpl", "私信接口"), ("PrivateMessageServiceImpl", "PrivateMessageMapper", "读写"), ("PrivateMessageMapper", "PrivateMessage", "映射")]),
        (34, "社区内容实现类图", [
            ("PostController", "Controller", ["listPosts()", "createPost()", "update/deletePost()"], 0, 0),
            ("PostServiceImpl", "Service", ["listPosts()", "createPost()", "update/deletePost()"], 1, 0),
            ("PostMapper", "Mapper", ["insert()", "selectPage()", "updateById()"], 2, 0),
            ("Post", "Entity", ["userId: Long", "title/content: String", "status: Integer"], 3, 0),
            ("SensitiveWordServiceImpl", "Service", ["containsSensitiveWord()", "filter()"], 1, 1),
            ("SysSensitiveWordMapper", "Mapper", ["selectList()", "insert()", "delete()"], 2, 1),
            ("SysSensitiveWord", "Entity", ["word: String", "status: Integer", "createTime: LocalDateTime"], 3, 1),
            ("LikeController", "Controller", ["toggle()", "status()", "myCollects()"], 0, 2),
            ("LikeServiceImpl", "Service", ["toggleLike()", "likeStatus()", "getMyCollects()"], 1, 2),
            ("LikeMapper", "Mapper", ["selectOne()", "insert()", "delete()"], 2, 2),
            ("Like", "Entity", ["userId/targetId: Long", "targetType: Integer", "createTime: LocalDateTime"], 3, 2),
            ("CommentController", "Controller", ["list()", "add()", "delete()"], 0, 3),
            ("CommentServiceImpl", "Service", ["listComments()", "addComment()", "deleteComment()"], 1, 3),
            ("CommentMapper", "Mapper", ["selectList()", "insert()", "delete()"], 2, 3),
            ("Comment", "Entity", ["postId/userId: Long", "parentId: Long", "content: String"], 3, 3),
        ], [("PostController", "PostServiceImpl", "帖子接口"), ("PostServiceImpl", "PostMapper", "读写"), ("PostMapper", "Post", "映射"),
            ("PostServiceImpl", "SensitiveWordServiceImpl", "发布前检测", "dashed"), ("SensitiveWordServiceImpl", "SysSensitiveWordMapper", "读取词库"), ("SysSensitiveWordMapper", "SysSensitiveWord", "映射"),
            ("LikeController", "LikeServiceImpl", "互动接口"), ("LikeServiceImpl", "LikeMapper", "幂等读写"), ("LikeMapper", "Like", "映射"),
            ("CommentController", "CommentServiceImpl", "评论接口"), ("CommentServiceImpl", "CommentMapper", "树状读写"), ("CommentMapper", "Comment", "映射")]),
        (35, "管理后台与可观测性实现类图", [
            ("SecurityConfig", "Security", ["securityFilterChain()"], 0, 0),
            ("JwtFilter", "Security", ["doFilterInternal()"], 1, 0),
            ("AdminController", "Controller", ["dashboardData()", "manageResources()", "audit/resolve()"], 0, 1),
            ("HotelRoomStockServiceImpl", "Service", ["preDeductRoom()", "rollbackPreDeduct()", "syncWithDatabase()"], 1, 1),
            ("HotelRoomMapper", "Mapper", ["selectById()", "updateById()"], 2, 1),
            ("HotelRoom", "Entity", ["hotelId: Long", "availableRooms: Integer", "price: BigDecimal"], 3, 1),
            ("PostMapper", "Mapper", ["selectPage()", "updateById()"], 2, 2),
            ("Post", "Entity", ["id/userId: Long", "status: Integer", "auditReason: String"], 3, 2),
            ("NotificationCenterServiceImpl", "Service", ["createNotification()", "listNotifications()"], 1, 3),
            ("NotificationMapper", "Mapper", ["insert()", "selectList()"], 2, 3),
            ("Notification", "Entity", ["userId: Long", "type/title: String", "isRead: Integer"], 3, 3),
            ("SysLogAspect", "Aspect", ["logAround()"], 0, 4),
            ("SysLogMapper", "Mapper", ["insert()", "selectPage()"], 2, 4),
            ("SysLog", "Entity", ["username/ip: String", "method: String", "duration/status: Long/Integer"], 3, 4),
        ], [("SecurityConfig", "JwtFilter", "注册过滤器"), ("JwtFilter", "AdminController", "鉴权放行", "dashed"),
            ("AdminController", "HotelRoomStockServiceImpl", "库存管理"), ("HotelRoomStockServiceImpl", "HotelRoomMapper", "读写"), ("HotelRoomMapper", "HotelRoom", "映射"),
            ("AdminController", "PostMapper", "审核状态更新"), ("PostMapper", "Post", "映射"),
            ("AdminController", "NotificationCenterServiceImpl", "审核通知"), ("NotificationCenterServiceImpl", "NotificationMapper", "写通知"), ("NotificationMapper", "Notification", "映射"),
            ("SysLogAspect", "AdminController", "AOP 拦截", "dashed"), ("SysLogAspect", "SysLogMapper", "写审计日志"), ("SysLogMapper", "SysLog", "映射")]),
    ]
    for args in class_sets:
        draw_class(*args)

    sequences = [
        (36, "UC01", "用户注册登录与账户安全", ["用户", "Login.vue", "UserController", "UserService", "UserMapper", "JwtUtil"], [(0,1,"提交注册/登录信息","call"),(1,2,"POST /user/register 或 /login","call"),(2,3,"register()/login()","call"),(3,4,"查询用户并写入 BCrypt 密码","call"),(4,3,"返回用户记录/写入结果","return"),(3,5,"generateToken(username)","call"),(5,3,"返回 JWT；失败则不签发","return"),(3,2,"返回登录结果","return"),(2,1,"Result<Token>","return")], "用户名重复、密码错误、账号禁用或 Token 过期时拒绝访问；密码不得明文落库。"),
        (37, "UC02", "查询并预订航班", ["用户", "FlightSearch.vue", "FlightController", "TrafficOrderController", "TrafficOrderServiceImpl", "FlightMapper/OrderMapper"], [(0,1,"输入城市、日期并选择乘客","call"),(1,2,"GET /api/flight/search","call"),(2,5,"查询航班","call"),(5,2,"返回航班列表","return"),(1,3,"POST /api/order/flight/create","call"),(3,4,"createFlightOrder(userId,dto)","call"),(4,5,"条件扣减座位并新增订单","call"),(5,4,"返回 orderNo；库存不足则失败","return"),(4,3,"返回待支付订单号","return")], "库存扣减与订单写入处于同一事务；库存不足时不得创建订单。"),
        (38, "UC03", "查询火车票与提交候补", ["用户", "TrainSearch.vue", "TrainController", "TrainServiceImpl", "TrainWaitlistServiceImpl", "TrainMapper/WaitlistMapper"], [(0,1,"输入车站、日期和席别","call"),(1,2,"GET /api/train/search","call"),(2,3,"searchTrains()","call"),(3,5,"查询本地/同步缓存","call"),(5,3,"返回车次","return"),(1,2,"无票时 POST /waitlist","call"),(2,4,"createWaitlist(userId,dto)","call"),(4,5,"校验车次旅客并写候补","call"),(5,4,"返回候补 ID","return")], "公共余票同步失败时回退本地数据；候补记录必须绑定当前用户和有效旅客。"),
        (39, "UC04", "交通订单支付取消退款", ["用户", "MyOrders.vue", "TrafficOrderController", "TrafficOrderServiceImpl", "TrafficOrderMapper", "NotificationCenterService"], [(0,1,"选择支付/取消/退款","call"),(1,2,"POST /api/order/{orderNo}/动作","call"),(2,3,"校验当前用户与状态","call"),(3,4,"读取并条件更新订单","call"),(4,3,"返回更新行数","return"),(3,5,"成功后创建通知","call"),(5,3,"通知失败仅记录告警","error"),(3,2,"返回当前终态","return")], "仅允许合法状态跃迁；取消和退款的库存回补必须幂等。"),
        (40, "UC05", "搜索酒店并完成订房", ["用户", "HotelDetail.vue", "HotelController", "HotelOrderServiceImpl", "HotelRoomStockServiceImpl", "HotelRoom/OrderMapper"], [(0,1,"选择日期、房型和房间数","call"),(1,2,"POST /api/hotel/order/create","call"),(2,3,"createOrder(userId,dto)","call"),(3,4,"preDeductRoom(roomId,count)","call"),(4,3,"成功/Redis 不可用/库存不足","return"),(3,5,"数据库条件扣减并插入订单","call"),(5,3,"返回 orderNo；失败则回滚预减","return"),(3,2,"返回待支付订单号","return")], "数据库是库存最终事实源；Redis 不可用可降级，数据库失败必须回滚缓存预减。"),
        (41, "UC06", "酒店订单支付取消退款", ["用户", "MyOrders.vue", "HotelController", "HotelOrderServiceImpl", "HotelOrderMapper", "HotelRoomStockServiceImpl"], [(0,1,"选择订单操作","call"),(1,2,"POST /api/hotel/order/{orderNo}/动作","call"),(2,3,"pay/cancel/requestRefund","call"),(3,4,"按用户和当前状态条件更新","call"),(4,3,"返回更新结果","return"),(3,5,"取消/退款时回补库存","call"),(5,3,"回补成功或幂等跳过","return"),(3,2,"返回终态","return")], "重复请求不得重复回补；超时关闭由 OrderTimeoutScheduler 复用同一状态与库存规则。"),
        (42, "UC07", "景点浏览与购票", ["用户", "AttractionList.vue", "AttractionController", "AttractionServiceImpl", "AttractionMapper", "AttractionOrderMapper"], [(0,1,"选择景点和票数","call"),(1,2,"GET /search 并 POST /{id}/ticket","call"),(2,3,"searchAttractions()/buyTicket()","call"),(3,4,"读取景点价格与票量","call"),(4,3,"返回景点","return"),(3,5,"计算金额并新增门票订单","call"),(5,3,"返回 orderNo","return"),(3,2,"返回订单凭证入口","return")], "成人/儿童票数与金额必须服务端计算；用户只能查看自己的凭证。"),
        (43, "UC08", "浏览一日游周边游产品", ["游客", "AttractionList.vue", "TourProductController", "TourProductServiceImpl", "TourProductMapper"], [(0,1,"进入本地游列表","call"),(1,2,"GET /api/tour/list?tourType=","call"),(2,3,"listByType(tourType)","call"),(3,4,"selectList()","call"),(4,3,"返回产品摘要","return"),(3,2,"返回列表","return"),(2,1,"Result<List<TourProduct>>","return")], "当前仅覆盖浏览目标，不包含购买闭环；正文和追溯状态均标记为部分实现、待确认用例。"),
        (44, "UC09", "提交评价回复与举报", ["用户/管理员", "详情页/后台", "Review/Reply/ReportController", "ReviewServiceImpl", "Review/Reply/ReportMapper", "AdminController"], [(0,1,"提交评价、回复或举报","call"),(1,2,"调用对应 /api 接口","call"),(2,3,"校验评分与目标","call"),(3,4,"写入关联记录","call"),(4,3,"返回记录 ID","return"),(0,5,"管理员处理举报","call"),(5,4,"条件更新工单或删除评价","call"),(4,5,"返回终态","return")], "重复评价、重复举报与越权回复必须拒绝；举报处理需保留终态。"),
        (45, "UC10", "优惠券领取与使用", ["用户", "CouponCenter.vue", "CouponController", "CouponServiceImpl", "CouponMapper/UserCouponMapper", "订单服务"], [(0,1,"查看并领取优惠券","call"),(1,2,"GET /list；POST /claim/{id}","call"),(2,3,"listAvailable()/claimCoupon()","call"),(3,4,"校验库存有效期并写用户券","call"),(4,3,"返回领取结果","return"),(5,3,"下单时 useCoupon()","call"),(3,4,"校验门槛并更新 used_time","call"),(4,3,"返回减免金额","return")], "领取逻辑已实现；订单核销调用和测试证据仍需补齐，不能标记已验证。"),
        (46, "UC11", "生成并保存AI行程", ["用户", "AiPlan.vue", "AiController", "AiServiceImpl", "DeepSeek API/本地模板", "AiPlanMapper"], [(0,1,"提交目的地日期预算偏好","call"),(1,2,"POST /api/ai/plan/generate","call"),(2,3,"generatePlan(dto,userId)","call"),(3,4,"调用模型并校验 JSON","call"),(4,3,"返回结构化结果或失败","return"),(3,4,"失败时切换本地模板","error"),(3,5,"保存计划与降级标记","call"),(5,3,"返回计划 ID","return"),(3,2,"返回可渲染计划","return")], "API Key 缺失、超时或非法 JSON 时必须降级；不得把模型文本当实时库存承诺。"),
        (47, "UC12", "AI客服多轮对话", ["用户", "AiPlan.vue", "AiController", "AiServiceImpl", "DeepSeek API", "AiChatMapper"], [(0,1,"发送问题与 sessionId","call"),(1,2,"POST /api/ai/chat","call"),(2,3,"chat(dto,userId)","call"),(3,5,"读取必要历史消息","call"),(5,3,"返回上下文","return"),(3,4,"发送受约束 Prompt","call"),(4,3,"返回回答或失败","return"),(3,5,"保存问答记录","call"),(3,2,"返回回答/可解释失败","return")], "订单、价格和库存必须由站内接口确认；外部失败时不得编造答案。"),
        (48, "UC13", "通知中心与站内私信", ["用户", "NotificationCenter/PrivateMessages.vue", "AiController/PrivateMessageController", "NotificationCenterServiceImpl", "PrivateMessageServiceImpl", "Notification/MessageMapper"], [(0,1,"查看通知或发送私信","call"),(1,2,"调用 notification/private-message 接口","call"),(2,3,"list/markRead/unreadCount","call"),(3,5,"查询或更新通知","call"),(2,4,"listConversation/sendMessage","call"),(4,5,"校验接收者并写消息","call"),(5,4,"返回记录","return"),(4,2,"返回会话/未读数","return")], "用户只能读取自己的通知和会话；通知失败不回滚核心交易。"),
        (49, "UC14", "发布编辑删除游记并审核", ["用户/管理员", "PostCreate.vue/AdminDashboard.vue", "PostController/AdminController", "PostServiceImpl", "SensitiveWordServiceImpl", "PostMapper"], [(0,1,"提交游记或审核动作","call"),(1,2,"POST /api/post/create 或 /admin/posts/{id}/approve","call"),(2,3,"createPost()/条件审核","call"),(3,4,"containsSensitiveWord()","call"),(4,3,"返回检测结果","return"),(3,5,"保存待审或条件更新终态","call"),(5,3,"返回状态","return"),(3,2,"返回审核结果","return")], "帖子仅允许 0待审→1通过/2拒绝；命中敏感词或越权编辑时拒绝。"),
        (50, "UC15", "社区点赞收藏与评论", ["用户", "PostDetail.vue", "Like/CommentController", "LikeServiceImpl", "CommentServiceImpl", "Like/CommentMapper"], [(0,1,"点赞收藏或提交评论","call"),(1,2,"POST /api/like/toggle 或 /comment/add","call"),(2,3,"toggleLike(userId,body)","call"),(3,5,"按用户目标幂等写入/删除","call"),(2,4,"addComment(body,userId)","call"),(4,5,"校验父评论并写入","call"),(5,4,"返回评论记录","return"),(3,2,"返回当前互动状态","return")], "重复互动保持幂等；只能删除自己的评论，父评论必须属于同一帖子。"),
        (51, "UC16", "常用旅客管理", ["用户", "FlightSearch/TrainSearch.vue", "PassengerController", "PassengerServiceImpl", "PassengerMapper"], [(0,1,"查看新增或删除旅客","call"),(1,2,"GET /list；POST /add；DELETE /{id}","call"),(2,3,"get/add/deletePassenger","call"),(3,4,"按 userId 查询或写入","call"),(4,3,"返回结果","return"),(3,2,"返回本人旅客数据","return"),(2,1,"刷新旅客列表","return")], "旅客必须绑定当前用户；删除操作按 id + userId 校验归属。"),
        (52, "UC17", "用户主页与关注关系", ["用户", "UserProfile.vue", "UserProfile/FollowController", "FollowServiceImpl", "UserMapper/FollowMapper", "NotificationCenterService"], [(0,1,"查看主页或切换关注","call"),(1,2,"GET profile；POST /api/follow/{userId}","call"),(2,3,"toggleFollow(follower,followee)","call"),(3,4,"校验用户并幂等更新关系","call"),(4,3,"返回关注状态","return"),(3,5,"关注成功时通知对方","call"),(5,3,"通知失败不回滚关系","error"),(3,2,"返回主页和关系状态","return")], "禁止关注自己；主页响应不得暴露密码、手机号等非公开字段。"),
        (53, "UC18", "管理后台资源订单用户管理", ["管理员", "AdminDashboard.vue", "JwtFilter/SecurityConfig", "AdminController", "各业务Mapper", "SysLogAspect/SysLogMapper"], [(0,1,"执行资源 CRUD 或订单用户操作","call"),(1,2,"携带 Bearer Token 请求 /api/admin/**","call"),(2,3,"管理员角色校验后放行","call"),(3,4,"按资源类型查询/写入/条件更新","call"),(4,3,"返回影响行数或冲突","return"),(3,5,"AOP 写操作摘要和耗时","call"),(5,3,"日志失败仅告警","error"),(3,1,"返回管理结果","return")], "当前由 AdminController 直接协调 Mapper/少量 Service；不得写成尚不存在的独立微服务。"),
        (54, "UC19", "内容安全与可观测性", ["管理员", "AdminDashboard.vue", "AdminController", "Post/Report/SensitiveWordMapper", "SysLogMapper", "NotificationCenterService"], [(0,1,"查看待审举报日志与指标","call"),(1,2,"GET/POST /api/admin/**","call"),(2,3,"查询待审并条件处理","call"),(3,2,"返回审核/工单终态","return"),(2,4,"查询 sys_log 和聚合指标","call"),(4,2,"返回日志与统计","return"),(2,5,"审核完成通知用户","call"),(5,2,"通知失败不回滚审核","error")], "重复处理保持终态；统计或通知故障不得回滚已经提交的审核结果。"),
    ]
    for seq in sequences:
        draw_sequence(*seq)

    # 旧域级顺序图改为真实类名，并明确其仅为代表流程。
    draw_sequence(15, "UC02", "大交通订单代表流程", ["用户", "FlightSearch.vue", "TrafficOrderController", "TrafficOrderServiceImpl", "FlightMapper", "TrafficOrderMapper"], [(0,1,"选择航班与乘客","call"),(1,2,"POST /api/order/flight/create","call"),(2,3,"createFlightOrder()","call"),(3,4,"条件扣减座位","call"),(4,3,"扣减成功/库存不足","return"),(3,5,"新增待支付订单","call"),(5,3,"返回 orderNo","return"),(3,2,"返回订单号","return")], "此图仅为 UC02 代表流程；UC03、UC04、UC16 分别见图片 38、39、51。")
    draw_sequence(25, "UC19", "管理员内容审核代表流程", ["管理员", "AdminDashboard.vue", "JwtFilter", "AdminController", "PostMapper", "SysLogAspect/SysLogMapper"], [(0,1,"选择审核通过或拒绝","call"),(1,2,"POST /api/admin/posts/{id}/approve|reject","call"),(2,3,"校验管理员角色并放行","call"),(3,4,"按待审状态条件更新","call"),(4,3,"返回更新结果","return"),(3,5,"写审计日志","call"),(5,3,"日志失败仅告警","error"),(3,1,"返回审核终态","return")], "当前没有 /admin/login、AuthService、ContentAuditController、ContentMapper；管理员登录复用 /user/login。")


if __name__ == "__main__":
    main()
