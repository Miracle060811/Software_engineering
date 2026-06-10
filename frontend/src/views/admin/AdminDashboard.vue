<template>
  <div class="admin-page">
    <el-container>
      <el-aside width="220px" class="admin-aside">
        <div class="admin-logo">成员 E 管理后台</div>
        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="stats"
            ><el-icon><DataLine /></el-icon
            ><span>可观测仪表盘</span></el-menu-item
          >
          <el-menu-item index="flights"
            ><el-icon><Promotion /></el-icon><span>航班资源</span></el-menu-item
          >
          <el-menu-item index="trains"
            ><el-icon><Promotion /></el-icon><span>火车资源</span></el-menu-item
          >
          <el-menu-item index="hotels"
            ><el-icon><House /></el-icon><span>酒店与房态</span></el-menu-item
          >
          <el-menu-item index="attractions"
            ><el-icon><Promotion /></el-icon><span>景点资源</span></el-menu-item
          >
          <el-menu-item index="destinations"
            ><el-icon><Promotion /></el-icon><span>城市资源</span></el-menu-item
          >
          <el-menu-item index="coupons"
            ><el-icon><Tickets /></el-icon><span>促销券配置</span></el-menu-item
          >
          <el-menu-item index="orders"
            ><el-icon><Tickets /></el-icon><span>订单流水</span></el-menu-item
          >
          <el-menu-item index="posts"
            ><el-icon><Document /></el-icon><span>内容审核</span></el-menu-item
          >
          <el-menu-item index="sensitive"
            ><el-icon><Document /></el-icon><span>敏感词库</span></el-menu-item
          >
          <el-menu-item index="logs"
            ><el-icon><Document /></el-icon><span>系统日志</span></el-menu-item
          >
          <el-menu-item index="users"
            ><el-icon><User /></el-icon><span>用户画像</span></el-menu-item
          >
        </el-menu>
      </el-aside>

      <el-main class="admin-main">
        <section v-if="activeMenu === 'stats'" v-loading="dashboardLoading">
          <h2 class="section-title">可观测仪表盘</h2>
          <el-row :gutter="16" class="stat-row">
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.totalUsers || 0 }}
                </div>
                <div class="stat-label">总用户数</div></el-card
              ></el-col
            >
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.totalOrders || 0 }}
                </div>
                <div class="stat-label">总订单数</div></el-card
              ></el-col
            >
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.todayOrders || 0 }}
                </div>
                <div class="stat-label">今日订单</div></el-card
              ></el-col
            >
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.pendingPosts || 0 }}
                </div>
                <div class="stat-label">待审核内容</div></el-card
              ></el-col
            >
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">{{ latestQps }}</div>
                <div class="stat-label">近分钟请求量</div></el-card
              ></el-col
            >
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">{{ latestLatency }}ms</div>
                <div class="stat-label">近分钟平均延迟</div></el-card
              ></el-col
            >
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  ¥{{ dashboardData.todayGmv || 0 }}
                </div>
                <div class="stat-label">今日 GMV</div></el-card
              ></el-col
            >
            <el-col :span="3"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.onlineUsers || 0 }}
                </div>
                <div class="stat-label">近 15 分钟活跃</div></el-card
              ></el-col
            >
          </el-row>

          <el-row :gutter="16" class="chart-row">
            <el-col :span="12"
              ><el-card class="panel-card"
                ><div ref="trendChartRef" class="chart-panel"></div></el-card
            ></el-col>
            <el-col :span="12"
              ><el-card class="panel-card"
                ><div ref="typeChartRef" class="chart-panel"></div></el-card
            ></el-col>
          </el-row>

          <el-row :gutter="16" class="chart-row">
            <el-col :span="12"
              ><el-card class="panel-card"
                ><div ref="destChartRef" class="chart-panel"></div></el-card
            ></el-col>
            <el-col :span="12"
              ><el-card class="panel-card"
                ><div ref="growthChartRef" class="chart-panel"></div></el-card
            ></el-col>
          </el-row>

          <el-row :gutter="16" class="chart-row">
            <el-col :span="12"
              ><el-card class="panel-card"
                ><div ref="qpsChartRef" class="chart-panel"></div></el-card
            ></el-col>
            <el-col :span="12"
              ><el-card class="panel-card"
                ><div ref="latencyChartRef" class="chart-panel"></div></el-card
            ></el-col>
          </el-row>

          <el-card class="alert-card">
            <template #header>
              <div class="card-header">
                <span>异常预警与运营待办</span>
                <el-tag type="info" size="small">{{ alerts.length }} 条</el-tag>
              </div>
            </template>
            <div class="alert-list">
              <div
                v-for="alert in alerts"
                :key="alert.title + alert.message"
                class="alert-item"
              >
                <el-tag :type="mapAlertType(alert.level)" effect="dark">{{
                  alert.title
                }}</el-tag>
                <div class="alert-message">{{ alert.message }}</div>
              </div>
            </div>
          </el-card>
        </section>

        <section v-else-if="activeMenu === 'flights'">
          <div class="toolbar">
            <h2 class="section-title">航班资源管理</h2>
            <div>
              <el-button @click="triggerImport('flights')">导入 CSV</el-button>
              <el-button type="primary" @click="openFlightDialog()"
                >新增航班</el-button
              >
            </div>
          </div>
          <el-table :data="flights" v-loading="flightLoading" stripe>
            <el-table-column prop="flightNo" label="航班号" width="120" />
            <el-table-column prop="airline" label="航司" width="120" />
            <el-table-column label="航线" min-width="160">
              <template #default="scope"
                >{{ scope.row.departureCity }} →
                {{ scope.row.arrivalCity }}</template
              >
            </el-table-column>
            <el-table-column
              prop="departureTime"
              label="出发时间"
              width="180"
            />
            <el-table-column label="价格" width="140">
              <template #default="scope"
                >¥{{ scope.row.economyPrice }} / ¥{{
                  scope.row.businessPrice
                }}</template
              >
            </el-table-column>
            <el-table-column prop="availableSeats" label="余票" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="scope"
                ><el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{
                  scope.row.status === 1 ? "正常" : "停运"
                }}</el-tag></template
              >
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openFlightDialog(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  size="small"
                  type="danger"
                  @click="removeFlight(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'trains'">
          <div class="toolbar">
            <h2 class="section-title">火车资源管理</h2>
            <div>
              <el-button @click="triggerImport('trains')">导入 CSV</el-button>
              <el-button type="primary" @click="openTrainDialog()"
                >新增车次</el-button
              >
            </div>
          </div>
          <el-table :data="trains" v-loading="trainLoading" stripe>
            <el-table-column prop="trainNo" label="车次" width="120" />
            <el-table-column prop="trainType" label="车型" width="100" />
            <el-table-column label="线路" min-width="180">
              <template #default="scope"
                >{{ scope.row.departureStation }} →
                {{ scope.row.arrivalStation }}</template
              >
            </el-table-column>
            <el-table-column
              prop="departureTime"
              label="出发时间"
              width="180"
            />
            <el-table-column label="票价" width="150">
              <template #default="scope"
                >¥{{ scope.row.firstClassPrice }} / ¥{{
                  scope.row.secondClassPrice
                }}</template
              >
            </el-table-column>
            <el-table-column label="余票" width="140">
              <template #default="scope"
                >一等 {{ scope.row.firstClassSeats }} / 二等
                {{ scope.row.secondClassSeats }}</template
              >
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="scope"
                ><el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{
                  scope.row.status === 1 ? "正常" : "停运"
                }}</el-tag></template
              >
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openTrainDialog(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  size="small"
                  type="danger"
                  @click="removeTrain(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'hotels'">
          <div class="toolbar">
            <h2 class="section-title">酒店与房态管理</h2>
            <div>
              <el-button @click="triggerImport('hotels')">导入 CSV</el-button>
              <el-button type="primary" @click="openHotelDialog()"
                >新增酒店</el-button
              >
            </div>
          </div>
          <el-table :data="hotels" v-loading="hotelLoading" stripe>
            <el-table-column prop="name" label="酒店名称" min-width="180" />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="avgPrice" label="均价" width="110">
              <template #default="scope">¥{{ scope.row.avgPrice }}</template>
            </el-table-column>
            <el-table-column prop="score" label="评分" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="scope"
                ><el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{
                  scope.row.status === 1 ? "营业中" : "已下线"
                }}</el-tag></template
              >
            </el-table-column>
            <el-table-column prop="address" label="地址" min-width="220" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openHotelDialog(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  size="small"
                  type="primary"
                  plain
                  @click="openRoomDrawer(scope.row)"
                  >房态</el-button
                >
                <el-button
                  size="small"
                  type="danger"
                  @click="removeHotel(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'attractions'">
          <div class="toolbar">
            <h2 class="section-title">景点资源管理</h2>
            <div>
              <el-button @click="triggerImport('attractions')"
                >导入 CSV</el-button
              >
              <el-button type="primary" @click="openAttractionDialog()"
                >新增景点</el-button
              >
            </div>
          </div>
          <el-table :data="attractions" v-loading="attractionLoading" stripe>
            <el-table-column prop="name" label="景点名称" min-width="180" />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column label="门票" width="150">
              <template #default="scope"
                >成人 ¥{{ scope.row.adultPrice }} / 儿童 ¥{{
                  scope.row.childPrice
                }}</template
              >
            </el-table-column>
            <el-table-column label="余票" width="130">
              <template #default="scope"
                >{{ scope.row.availableTickets }} /
                {{ scope.row.totalTickets }}</template
              >
            </el-table-column>
            <el-table-column
              prop="openTime"
              label="开放时间"
              min-width="160"
              show-overflow-tooltip
            />
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{
                  scope.row.status === 1 ? "开放" : "下线"
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openAttractionDialog(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  size="small"
                  type="danger"
                  @click="removeAttraction(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'destinations'">
          <div class="toolbar">
            <h2 class="section-title">城市资源管理</h2>
            <div>
              <el-button @click="triggerImport('destinations')"
                >导入 CSV</el-button
              >
            </div>
          </div>
          <el-table :data="destinations" v-loading="destinationLoading" stripe>
            <el-table-column prop="slug" label="标识" width="120" />
            <el-table-column prop="name" label="城市" width="100" />
            <el-table-column prop="tag" label="标签" width="120" />
            <el-table-column
              prop="keywords"
              label="关键词"
              min-width="160"
              show-overflow-tooltip
            />
            <el-table-column
              prop="desc"
              label="短描述"
              min-width="240"
              show-overflow-tooltip
            />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{
                  scope.row.status === 1 ? "展示" : "下线"
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  size="small"
                  type="danger"
                  @click="removeDestination(scope.row)"
                  >下线</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'coupons'">
          <div class="toolbar">
            <h2 class="section-title">促销券配置</h2>
            <el-button type="primary" @click="openCouponDialog()"
              >新增优惠券</el-button
            >
          </div>
          <el-table :data="coupons" v-loading="couponLoading" stripe>
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column label="适用类别" width="100">
              <template #default="scope">{{
                couponCategoryLabel(scope.row.category)
              }}</template>
            </el-table-column>
            <el-table-column label="类型" width="90">
              <template #default="scope">{{
                scope.row.discountType === 0 ? "满减" : "折扣"
              }}</template>
            </el-table-column>
            <el-table-column label="优惠力度" width="120">
              <template #default="scope">{{
                scope.row.discountType === 0
                  ? `¥${scope.row.discountValue}`
                  : `${scope.row.discountValue} 折`
              }}</template>
            </el-table-column>
            <el-table-column prop="minAmount" label="门槛" width="100">
              <template #default="scope">¥{{ scope.row.minAmount }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="80" />
            <el-table-column prop="expireDate" label="到期时间" width="180" />
            <el-table-column label="状态" width="90">
              <template #default="scope"
                ><el-tag :type="scope.row.status === 0 ? 'success' : 'info'">{{
                  scope.row.status === 0 ? "有效" : "已过期"
                }}</el-tag></template
              >
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  @click="openCouponClaims(scope.row)"
                  >领取记录</el-button
                >
                <el-button size="small" @click="openCouponDialog(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  size="small"
                  type="danger"
                  @click="removeCoupon(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'orders'">
          <div class="toolbar">
            <h2 class="section-title">平台订单流水</h2>
          </div>
          <el-tabs
            v-model="orderTypeFilter"
            @tab-change="handleOrderTypeChange"
          >
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="机票" name="flight" />
            <el-tab-pane label="火车票" name="train" />
            <el-tab-pane label="酒店" name="hotel" />
          </el-tabs>
          <div class="toolbar toolbar-inline">
            <div class="muted-text">
              按订单状态筛选流水，可处理用户提交的退票/退款申请。
            </div>
            <el-select
              v-model="orderStatusFilter"
              placeholder="全部状态"
              clearable
              style="width: 180px"
              @change="handleOrderTypeChange"
            >
              <el-option label="待支付" :value="0" />
              <el-option label="已支付/出票中" :value="1" />
              <el-option label="已出票/入住中" :value="2" />
              <el-option label="已取消/已完成" :value="3" />
              <el-option label="已退票/已退款/已取消" :value="4" />
              <el-option label="退款申请中" :value="5" />
            </el-select>
          </div>
          <el-table :data="orders" v-loading="orderLoading" stripe>
            <el-table-column prop="orderNo" label="订单号" width="260" />
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column prop="route" label="详情" min-width="220" />
            <el-table-column prop="passenger" label="乘客/住客" width="120" />
            <el-table-column label="金额" width="100"
              ><template #default="scope"
                >¥{{ scope.row.amount }}</template
              ></el-table-column
            >
            <el-table-column label="状态" width="120">
              <template #default="scope"
                ><el-tag :type="orderStatusType(scope.row)">{{
                  orderStatusLabel(scope.row)
                }}</el-tag></template
              >
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="canReviewRefund(scope.row)"
                  size="small"
                  type="success"
                  @click="approveRefund(scope.row.orderNo)"
                  >办理退款</el-button
                >
                <el-button
                  v-if="canReviewRefund(scope.row)"
                  size="small"
                  type="danger"
                  plain
                  @click="rejectRefund(scope.row.orderNo)"
                  >驳回</el-button
                >
                <span v-else class="muted-text">无人工操作</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager-wrap">
            <el-pagination
              v-model:current-page="orderPage"
              v-model:page-size="orderSize"
              :total="orderTotal"
              layout="prev, pager, next"
              @current-change="fetchOrders"
            />
          </div>
        </section>

        <section v-else-if="activeMenu === 'posts'">
          <div class="toolbar">
            <h2 class="section-title">内容审核与举报处理</h2>
          </div>
          <el-tabs v-model="reviewAuditTab" @tab-change="handleAuditTabChange">
            <el-tab-pane label="游记审核" name="posts">
              <div class="toolbar toolbar-inline">
                <div class="muted-text">审核公开游记内容，处理待审队列。</div>
                <el-radio-group v-model="postStatusFilter" @change="fetchPosts">
                  <el-radio-button label="pending">待审核</el-radio-button>
                  <el-radio-button label="approved">已通过</el-radio-button>
                  <el-radio-button label="rejected">已拒绝</el-radio-button>
                  <el-radio-button label="all">全部</el-radio-button>
                </el-radio-group>
              </div>
              <el-table :data="reviewPosts" v-loading="postLoading" stripe>
                <el-table-column prop="title" label="标题" min-width="180" />
                <el-table-column
                  prop="content"
                  label="正文预览"
                  min-width="220"
                  show-overflow-tooltip
                />
                <el-table-column
                  prop="authorUsername"
                  label="作者"
                  width="120"
                />
                <el-table-column
                  prop="destination"
                  label="目的地"
                  width="120"
                />
                <el-table-column
                  prop="aiSuggestion"
                  label="AI建议"
                  min-width="180"
                  show-overflow-tooltip
                />
                <el-table-column
                  prop="rejectReason"
                  label="拒绝原因"
                  min-width="160"
                  show-overflow-tooltip
                />
                <el-table-column label="状态" width="100">
                  <template #default="scope"
                    ><el-tag :type="postStatusType(scope.row.status)">{{
                      postStatusLabel(scope.row.status)
                    }}</el-tag></template
                  >
                </el-table-column>
                <el-table-column
                  prop="createTime"
                  label="发布时间"
                  width="180"
                />
                <el-table-column label="操作" width="430" fixed="right">
                  <template #default="scope">
                    <div class="post-audit-actions">
                      <el-button
                        v-if="scope.row.status !== 1"
                        size="small"
                        type="success"
                        @click="approvePost(scope.row.id)"
                        >改为通过</el-button
                      >
                      <el-button
                        size="small"
                        type="primary"
                        plain
                        @click="openPostMetricsDialog(scope.row)"
                        >修改数据</el-button
                      >
                      <el-button
                        size="small"
                        type="danger"
                        @click="rejectPost(scope.row.id)"
                        >{{
                          scope.row.status === 2 ? "修改原因" : "改为拒绝"
                        }}</el-button
                      >
                      <el-button
                        size="small"
                        type="danger"
                        plain
                        @click="disablePostAuthor(scope.row)"
                        >封禁作者</el-button
                      >
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="举报工单" name="reports">
              <div class="toolbar toolbar-inline">
                <div class="muted-text">处理用户提交的评价举报工单。</div>
              </div>
              <el-table :data="reviewReports" v-loading="reportLoading" stripe>
                <el-table-column prop="reviewId" label="评价ID" width="100" />
                <el-table-column
                  prop="targetName"
                  label="评价对象"
                  min-width="140"
                  show-overflow-tooltip
                />
                <el-table-column
                  prop="reviewContent"
                  label="评价内容"
                  min-width="220"
                  show-overflow-tooltip
                />
                <el-table-column prop="rating" label="评分" width="70" />
                <el-table-column
                  prop="reporterUsername"
                  label="举报人"
                  width="120"
                />
                <el-table-column
                  prop="reason"
                  label="举报原因"
                  min-width="220"
                  show-overflow-tooltip
                />
                <el-table-column
                  prop="handleRemark"
                  label="处理备注"
                  min-width="160"
                  show-overflow-tooltip
                />
                <el-table-column label="状态" width="100">
                  <template #default="scope"
                    ><el-tag
                      :type="scope.row.status === 0 ? 'warning' : 'success'"
                      >{{
                        scope.row.status === 0 ? "待处理" : "已处理"
                      }}</el-tag
                    ></template
                  >
                </el-table-column>
                <el-table-column
                  prop="createTime"
                  label="提交时间"
                  width="180"
                />
                <el-table-column label="操作" width="330" fixed="right">
                  <template #default="scope">
                    <el-button
                      v-if="scope.row.status === 0"
                      size="small"
                      type="primary"
                      @click="resolveReviewReport(scope.row.id)"
                      >标记已处理</el-button
                    >
                    <el-button
                      v-if="scope.row.status === 0"
                      size="small"
                      @click="rejectReviewReport(scope.row.id)"
                      >驳回举报</el-button
                    >
                    <el-button
                      v-if="scope.row.status === 0"
                      size="small"
                      type="danger"
                      @click="deleteReportedReview(scope.row.id)"
                      >删除评价</el-button
                    >
                    <el-button
                      size="small"
                      type="success"
                      plain
                      @click="openReplyDrawer(scope.row)"
                      >商家回复</el-button
                    >
                    <span v-if="scope.row.status !== 0" class="muted-text"
                      >已完成</span
                    >
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </section>

        <section v-else-if="activeMenu === 'sensitive'">
          <div class="toolbar">
            <h2 class="section-title">敏感词过滤配置</h2>
            <el-button type="primary" @click="openSensitiveDialog()"
              >新增敏感词</el-button
            >
          </div>
          <el-table :data="sensitiveWords" v-loading="sensitiveLoading" stripe>
            <el-table-column prop="word" label="敏感词" min-width="180" />
            <el-table-column prop="level" label="等级" width="100" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openSensitiveDialog(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  size="small"
                  type="danger"
                  @click="removeSensitiveWord(scope.row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'logs'">
          <div class="toolbar">
            <h2 class="section-title">系统操作日志</h2>
          </div>
          <el-table :data="logs" v-loading="logLoading" stripe>
            <el-table-column prop="username" label="操作人" width="120" />
            <el-table-column prop="operation" label="操作" min-width="180" />
            <el-table-column prop="method" label="方法" min-width="180" />
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column prop="timeMs" label="耗时(ms)" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="scope"
                ><el-tag
                  :type="scope.row.status === 1 ? 'success' : 'danger'"
                  >{{ scope.row.status === 1 ? "成功" : "失败" }}</el-tag
                ></template
              >
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="180" />
            <el-table-column
              prop="errorMsg"
              label="异常信息"
              min-width="220"
              show-overflow-tooltip
            />
          </el-table>
          <div class="pager-wrap">
            <el-pagination
              v-model:current-page="logPage"
              v-model:page-size="logSize"
              :total="logTotal"
              layout="total, prev, pager, next"
              @current-change="fetchLogs"
            />
          </div>
        </section>

        <section v-else-if="activeMenu === 'users'">
          <div class="toolbar">
            <h2 class="section-title">用户画像与封禁管理</h2>
          </div>
          <el-table :data="users" v-loading="userLoading" stripe>
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="nickname" label="昵称" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="200" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column label="角色" width="100">
              <template #default="scope"
                ><el-tag :type="scope.row.role === 1 ? 'danger' : 'info'">{{
                  scope.row.role === 1 ? "管理员" : "普通用户"
                }}</el-tag></template
              >
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope"
                ><el-tag
                  :type="scope.row.status === 1 ? 'success' : 'danger'"
                  >{{ scope.row.status === 1 ? "正常" : "禁用" }}</el-tag
                ></template
              >
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" width="180" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openUserDrawer(scope.row)"
                  >画像</el-button
                >
                <el-button
                  size="small"
                  :type="scope.row.status === 1 ? 'danger' : 'success'"
                  @click="toggleUserStatus(scope.row)"
                  >{{ scope.row.status === 1 ? "禁用" : "启用" }}</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-main>
    </el-container>

    <el-dialog
      v-model="importDialogVisible"
      title="CSV 批量导入"
      width="780px"
      class="import-dialog"
    >
      <div class="import-grid">
        <section class="import-panel">
          <el-form label-position="top">
            <el-form-item label="导入资源">
              <el-select v-model="importForm.type" style="width: 100%">
                <el-option
                  v-for="item in importTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="写入方式">
              <el-radio-group v-model="importForm.mode">
                <el-radio-button label="insert">仅新增</el-radio-button>
                <el-radio-button label="upsert">重复则更新</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-checkbox v-model="importForm.dryRun">
              只预检，不写入数据库
            </el-checkbox>
            <el-upload
              class="csv-upload"
              drag
              accept=".csv,text/csv"
              :auto-upload="false"
              :limit="1"
              :on-change="handleImportFileChange"
              :on-remove="clearImportFile"
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                拖入 CSV 或 <em>点击选择</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  请使用 UTF-8 编码，首行必须是字段名。
                </div>
              </template>
            </el-upload>
          </el-form>
        </section>

        <section class="import-panel import-help">
          <div class="import-help-title">
            <span>{{ currentImportType.label }}</span>
            <el-button text type="primary" @click="downloadCsvTemplate">
              下载模板
            </el-button>
          </div>
          <div class="field-list">
            <el-tag
              v-for="field in currentImportType.required"
              :key="field"
              size="small"
              type="danger"
              effect="plain"
            >
              {{ field }}
            </el-tag>
            <el-tag
              v-for="field in currentImportType.optional"
              :key="field"
              size="small"
              effect="plain"
            >
              {{ field }}
            </el-tag>
          </div>
          <p class="muted-text">
            红色字段必填；日期时间支持 2026-06-01T08:00:00 或
            2026-06-01 08:00:00。
          </p>
        </section>
      </div>

      <el-alert
        v-if="importResult"
        class="import-result"
        :type="importResult.failed ? 'warning' : 'success'"
        :closable="false"
        show-icon
      >
        <template #title>
          共 {{ importResult.total || 0 }} 行，成功
          {{ importResult.success || 0 }} 行，失败
          {{ importResult.failed || 0 }} 行
        </template>
        <div>
          新增 {{ importResult.inserted || 0 }}，更新
          {{ importResult.updated || 0 }}，预检
          {{ importResult.validated || 0 }}
        </div>
      </el-alert>

      <el-table
        v-if="importFailures.length"
        :data="importFailures"
        class="import-failure-table"
        max-height="220"
        stripe
      >
        <el-table-column prop="line" label="行号" width="90" />
        <el-table-column prop="reason" label="失败原因" min-width="420" />
      </el-table>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="importLoading"
          :disabled="!importForm.file"
          @click="submitCsvImport"
        >
          {{ importForm.dryRun ? "开始预检" : "开始导入" }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="flightDialogVisible"
      :title="flightForm.id ? '编辑航班' : '新增航班'"
      width="760px"
    >
      <el-form :model="flightForm" label-width="100px" class="entity-form">
        <el-row :gutter="16">
          <el-col :span="12"
            ><el-form-item label="航班号"
              ><el-input v-model="flightForm.flightNo" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="航司"
              ><el-input v-model="flightForm.airline" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="出发城市"
              ><el-input v-model="flightForm.departureCity" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="到达城市"
              ><el-input v-model="flightForm.arrivalCity" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="出发时间"
              ><el-date-picker
                v-model="flightForm.departureTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="到达时间"
              ><el-date-picker
                v-model="flightForm.arrivalTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="经济舱"
              ><el-input-number
                v-model="flightForm.economyPrice"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="公务舱"
              ><el-input-number
                v-model="flightForm.businessPrice"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="总座位数"
              ><el-input-number
                v-model="flightForm.totalSeats"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="可售座位"
              ><el-input-number
                v-model="flightForm.availableSeats"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="状态"
              ><el-select v-model="flightForm.status" style="width: 100%"
                ><el-option label="正常" :value="1" /><el-option
                  label="停运"
                  :value="0" /></el-select></el-form-item
          ></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="flightDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFlight">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="trainDialogVisible"
      :title="trainForm.id ? '编辑车次' : '新增车次'"
      width="760px"
    >
      <el-form :model="trainForm" label-width="100px" class="entity-form">
        <el-row :gutter="16">
          <el-col :span="12"
            ><el-form-item label="车次"
              ><el-input v-model="trainForm.trainNo" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="车型"
              ><el-input v-model="trainForm.trainType" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="出发站"
              ><el-input v-model="trainForm.departureStation" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="到达站"
              ><el-input v-model="trainForm.arrivalStation" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="出发时间"
              ><el-date-picker
                v-model="trainForm.departureTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="到达时间"
              ><el-date-picker
                v-model="trainForm.arrivalTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="一等座票价"
              ><el-input-number
                v-model="trainForm.firstClassPrice"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="二等座票价"
              ><el-input-number
                v-model="trainForm.secondClassPrice"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="一等座余票"
              ><el-input-number
                v-model="trainForm.firstClassSeats"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="二等座余票"
              ><el-input-number
                v-model="trainForm.secondClassSeats"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="状态"
              ><el-select v-model="trainForm.status" style="width: 100%"
                ><el-option label="正常" :value="1" /><el-option
                  label="停运"
                  :value="0" /></el-select></el-form-item
          ></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="trainDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTrain">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="hotelDialogVisible"
      :title="hotelForm.id ? '编辑酒店' : '新增酒店'"
      width="760px"
    >
      <el-form :model="hotelForm" label-width="100px" class="entity-form">
        <el-row :gutter="16">
          <el-col :span="12"
            ><el-form-item label="酒店名称"
              ><el-input v-model="hotelForm.name" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="城市"
              ><el-input v-model="hotelForm.city" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="星级"
              ><el-input-number
                v-model="hotelForm.starRating"
                :min="1"
                :max="5"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="均价"
              ><el-input-number
                v-model="hotelForm.avgPrice"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="评分"
              ><el-input-number
                v-model="hotelForm.score"
                :min="0"
                :max="5"
                :precision="1"
                :step="0.1"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="状态"
              ><el-select v-model="hotelForm.status" style="width: 100%"
                ><el-option label="营业中" :value="1" /><el-option
                  label="已下线"
                  :value="0" /></el-select></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="地址"
              ><el-input v-model="hotelForm.address" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="封面图"
              ><el-input
                v-model="hotelForm.coverImg"
                placeholder="真实图片 URL 或 /uploads/... 本地路径" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="描述"
              ><el-input
                v-model="hotelForm.description"
                type="textarea"
                :rows="3" /></el-form-item
          ></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="hotelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveHotel">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="postMetricsDialogVisible"
      title="修改游记互动数据"
      width="420px"
    >
      <el-form :model="postMetricsForm" label-width="90px" class="entity-form">
        <el-form-item label="游记标题">
          <div class="form-readonly-text">{{ postMetricsForm.title || "-" }}</div>
        </el-form-item>
        <el-form-item label="点赞量">
          <el-input-number
            v-model="postMetricsForm.likeCount"
            :min="0"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="收藏量">
          <el-input-number
            v-model="postMetricsForm.collectCount"
            :min="0"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="postMetricsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePostMetrics">确认保存</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="roomDrawerVisible"
      :title="
        activeHotel ? `${activeHotel.name} · 房态库存管理` : '房态库存管理'
      "
      size="760px"
    >
      <div class="toolbar toolbar-inline">
        <div class="muted-text">成员 E 可在此干预房态、价格和上下架。</div>
        <div>
          <el-button @click="triggerImport('rooms')">导入 CSV</el-button>
          <el-button type="primary" @click="openRoomDialog()"
            >新增房型</el-button
          >
        </div>
      </div>
      <el-table :data="hotelRooms" v-loading="roomLoading" stripe>
        <el-table-column prop="roomType" label="房型" min-width="160" />
        <el-table-column prop="bedType" label="床型" width="120" />
        <el-table-column prop="price" label="价格" width="100"
          ><template #default="scope"
            >¥{{ scope.row.price }}</template
          ></el-table-column
        >
        <el-table-column prop="totalRooms" label="总房量" width="90" />
        <el-table-column prop="availableRooms" label="可售" width="90" />
        <el-table-column label="状态" width="90"
          ><template #default="scope"
            ><el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{
              scope.row.status === 1 ? "正常" : "下线"
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openRoomDialog(scope.row)"
              >编辑</el-button
            >
            <el-button size="small" type="danger" @click="removeRoom(scope.row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog
      v-model="roomDialogVisible"
      :title="roomForm.id ? '编辑房型' : '新增房型'"
      width="700px"
    >
      <el-form :model="roomForm" label-width="100px" class="entity-form">
        <el-row :gutter="16">
          <el-col :span="12"
            ><el-form-item label="房型"
              ><el-input v-model="roomForm.roomType" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="床型"
              ><el-input v-model="roomForm.bedType" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="面积"
              ><el-input-number
                v-model="roomForm.area"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="价格"
              ><el-input-number
                v-model="roomForm.price"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="总房量"
              ><el-input-number
                v-model="roomForm.totalRooms"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="可售房量"
              ><el-input-number
                v-model="roomForm.availableRooms"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="状态"
              ><el-select v-model="roomForm.status" style="width: 100%"
                ><el-option label="正常" :value="1" /><el-option
                  label="下线"
                  :value="0" /></el-select></el-form-item
          ></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="attractionDialogVisible"
      :title="attractionForm.id ? '编辑景点' : '新增景点'"
      width="820px"
    >
      <el-form :model="attractionForm" label-width="110px" class="entity-form">
        <el-row :gutter="16">
          <el-col :span="12"
            ><el-form-item label="景点名称"
              ><el-input v-model="attractionForm.name" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="城市"
              ><el-input v-model="attractionForm.city" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="地址"
              ><el-input v-model="attractionForm.address" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="成人票价"
              ><el-input-number
                v-model="attractionForm.adultPrice"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="儿童票价"
              ><el-input-number
                v-model="attractionForm.childPrice"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="总票数"
              ><el-input-number
                v-model="attractionForm.totalTickets"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="可售票数"
              ><el-input-number
                v-model="attractionForm.availableTickets"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="开放时间"
              ><el-input v-model="attractionForm.openTime" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="状态"
              ><el-select v-model="attractionForm.status" style="width: 100%"
                ><el-option label="开放" :value="1" /><el-option
                  label="下线"
                  :value="0" /></el-select></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="纬度"
              ><el-input-number
                v-model="attractionForm.lat"
                :precision="6"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="经度"
              ><el-input-number
                v-model="attractionForm.lng"
                :precision="6"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="封面图"
              ><el-input v-model="attractionForm.coverImg" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="来源名称"
              ><el-input v-model="attractionForm.sourceName" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="核验日期"
              ><el-date-picker
                v-model="attractionForm.dataCheckedDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="来源URL"
              ><el-input v-model="attractionForm.officialUrl" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="描述"
              ><el-input
                v-model="attractionForm.description"
                type="textarea"
                :rows="3" /></el-form-item
          ></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="attractionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAttraction">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="couponDialogVisible"
      :title="couponForm.id ? '编辑优惠券' : '新增优惠券'"
      width="760px"
    >
      <el-form :model="couponForm" label-width="100px" class="entity-form">
        <el-row :gutter="16">
          <el-col :span="12"
            ><el-form-item label="名称"
              ><el-input v-model="couponForm.name" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="类型"
              ><el-select v-model="couponForm.discountType" style="width: 100%"
                ><el-option label="满减" :value="0" /><el-option
                  label="折扣"
                  :value="1" /></el-select></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="适用类别"
              ><el-select v-model="couponForm.category" style="width: 100%"
                ><el-option label="全部通用" value="all" /><el-option
                  label="机票"
                  value="flight" /><el-option
                  label="火车票"
                  value="train" /><el-option
                  label="酒店"
                  value="hotel" /></el-select></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="优惠值"
              ><el-input-number
                v-model="couponForm.discountValue"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="最低消费"
              ><el-input-number
                v-model="couponForm.minAmount"
                :min="0"
                :precision="2"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="库存"
              ><el-input-number
                v-model="couponForm.stock"
                :min="0"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="状态"
              ><el-select v-model="couponForm.status" style="width: 100%"
                ><el-option label="有效" :value="0" /><el-option
                  label="已过期"
                  :value="1" /></el-select></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="到期时间"
              ><el-date-picker
                v-model="couponForm.expireDate"
                type="datetime"
                value-format="YYYY-MM-DD[T]HH:mm:ss"
                style="width: 100%" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="描述"
              ><el-input
                v-model="couponForm.description"
                type="textarea"
                :rows="3" /></el-form-item
          ></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="couponDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCoupon">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="sensitiveDialogVisible"
      :title="sensitiveForm.id ? '编辑敏感词' : '新增敏感词'"
      width="520px"
    >
      <el-form :model="sensitiveForm" label-width="90px" class="entity-form">
        <el-form-item label="敏感词"
          ><el-input v-model="sensitiveForm.word"
        /></el-form-item>
        <el-form-item label="等级"
          ><el-input-number
            v-model="sensitiveForm.level"
            :min="1"
            :max="5"
            style="width: 100%"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sensitiveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSensitiveWord">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="couponClaimsDrawerVisible"
      :title="activeCoupon ? `${activeCoupon.name} · 领取记录` : '领取记录'"
      size="620px"
    >
      <el-table :data="couponClaims" v-loading="couponClaimsLoading" stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="scope">{{
            couponClaimStatusLabel(scope.row.status)
          }}</template>
        </el-table-column>
        <el-table-column prop="receivedTime" label="领取时间" width="180" />
        <el-table-column prop="usedTime" label="使用时间" width="180" />
      </el-table>
    </el-drawer>

    <el-drawer
      v-model="replyDrawerVisible"
      :title="
        activeReport ? `评价 ${activeReport.reviewId} · 商家回复` : '商家回复'
      "
      size="620px"
    >
      <el-descriptions v-if="activeReport" :column="1" border>
        <el-descriptions-item label="评价对象">{{
          activeReport.targetName
        }}</el-descriptions-item>
        <el-descriptions-item label="评价内容">{{
          activeReport.reviewContent
        }}</el-descriptions-item>
      </el-descriptions>
      <div class="reply-editor">
        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="3"
          placeholder="输入商家回复内容"
        />
        <el-button type="primary" @click="saveReply">发布回复</el-button>
      </div>
      <el-table :data="reviewReplies" v-loading="replyLoading" stripe>
        <el-table-column prop="content" label="回复内容" min-width="240" />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button
              size="small"
              type="danger"
              @click="removeReply(scope.row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-drawer
      v-model="userDrawerVisible"
      :title="selectedUser ? `${selectedUser.username} · 用户画像` : '用户画像'"
      size="520px"
    >
      <el-descriptions v-if="selectedUser" :column="1" border>
        <el-descriptions-item label="用户名">{{
          selectedUser.username
        }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{
          selectedUser.nickname || "未填写"
        }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{
          selectedUser.email || "未填写"
        }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{
          selectedUser.phone || "未填写"
        }}</el-descriptions-item>
        <el-descriptions-item label="个性签名">{{
          selectedUser.bio || "未填写"
        }}</el-descriptions-item>
        <el-descriptions-item label="用户等级"
          >Lv.{{ selectedUser.level || 1 }}</el-descriptions-item
        >
        <el-descriptions-item label="角色">{{
          selectedUser.role === 1 ? "管理员" : "普通用户"
        }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{
          selectedUser.status === 1 ? "正常" : "禁用"
        }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{
          selectedUser.createTime
        }}</el-descriptions-item>
        <el-descriptions-item label="交通订单">{{
          selectedUser.trafficOrderCount || 0
        }}</el-descriptions-item>
        <el-descriptions-item label="酒店订单">{{
          selectedUser.hotelOrderCount || 0
        }}</el-descriptions-item>
        <el-descriptions-item label="发帖数">{{
          selectedUser.postCount || 0
        }}</el-descriptions-item>
        <el-descriptions-item label="评论数">{{
          selectedUser.commentCount || 0
        }}</el-descriptions-item>
        <el-descriptions-item label="评价数">{{
          selectedUser.reviewCount || 0
        }}</el-descriptions-item>
        <el-descriptions-item label="举报数">{{
          selectedUser.reportCount || 0
        }}</el-descriptions-item>
        <el-descriptions-item label="最近操作">{{
          selectedUser.lastOperation || "暂无"
        }}</el-descriptions-item>
        <el-descriptions-item label="最近操作时间">{{
          selectedUser.lastOperationTime || "暂无"
        }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import * as echarts from "echarts";
import {
  DataLine,
  Promotion,
  House,
  Document,
  User,
  Tickets,
  UploadFilled,
} from "@element-plus/icons-vue";
import request from "@/utils/request";

const activeMenu = ref("stats");
const dashboardLoading = ref(false);
const dashboardData = ref({});

const trendChartRef = ref(null);
const typeChartRef = ref(null);
const destChartRef = ref(null);
const growthChartRef = ref(null);
const qpsChartRef = ref(null);
const latencyChartRef = ref(null);

const flights = ref([]);
const trains = ref([]);
const hotels = ref([]);
const hotelRooms = ref([]);
const attractions = ref([]);
const destinations = ref([]);
const coupons = ref([]);
const couponClaims = ref([]);
const orders = ref([]);
const reviewPosts = ref([]);
const reviewReports = ref([]);
const reviewReplies = ref([]);
const sensitiveWords = ref([]);
const logs = ref([]);
const users = ref([]);
const postMetricsDialogVisible = ref(false);
const postMetricsForm = ref({
  id: null,
  title: "",
  likeCount: 0,
  collectCount: 0,
});

const flightLoading = ref(false);
const trainLoading = ref(false);
const hotelLoading = ref(false);
const roomLoading = ref(false);
const attractionLoading = ref(false);
const destinationLoading = ref(false);
const couponLoading = ref(false);
const couponClaimsLoading = ref(false);
const orderLoading = ref(false);
const postLoading = ref(false);
const reportLoading = ref(false);
const replyLoading = ref(false);
const sensitiveLoading = ref(false);
const logLoading = ref(false);
const userLoading = ref(false);

const orderTypeFilter = ref("all");
const orderStatusFilter = ref(null);
const orderPage = ref(1);
const orderSize = ref(20);
const orderTotal = ref(0);

const postStatusFilter = ref("pending");
const reviewAuditTab = ref("posts");

const logPage = ref(1);
const logSize = ref(200);
const logTotal = ref(0);

const flightDialogVisible = ref(false);
const trainDialogVisible = ref(false);
const hotelDialogVisible = ref(false);
const roomDrawerVisible = ref(false);
const roomDialogVisible = ref(false);
const attractionDialogVisible = ref(false);
const couponDialogVisible = ref(false);
const couponClaimsDrawerVisible = ref(false);
const sensitiveDialogVisible = ref(false);
const replyDrawerVisible = ref(false);
const userDrawerVisible = ref(false);
const importDialogVisible = ref(false);
const importLoading = ref(false);

const activeHotel = ref(null);
const activeCoupon = ref(null);
const activeReport = ref(null);
const selectedUser = ref(null);
const replyContent = ref("");
const importResult = ref(null);

const importTypeOptions = [
  {
    value: "flights",
    label: "航班资源",
    required: [
      "flightNo",
      "airline",
      "departureCity",
      "arrivalCity",
      "departureTime",
      "arrivalTime",
      "economyPrice",
      "businessPrice",
      "totalSeats",
      "availableSeats",
    ],
    optional: ["status"],
  },
  {
    value: "trains",
    label: "火车资源",
    required: [
      "trainNo",
      "trainType",
      "departureStation",
      "arrivalStation",
      "departureTime",
      "arrivalTime",
      "firstClassPrice",
      "secondClassPrice",
      "firstClassSeats",
      "secondClassSeats",
    ],
    optional: ["durationMinutes", "status"],
  },
  {
    value: "hotels",
    label: "酒店资源",
    required: ["name", "city", "address", "starRating", "avgPrice"],
    optional: ["description", "coverImg", "lat", "lng", "score", "status"],
  },
  {
    value: "rooms",
    label: "酒店房型",
    required: [
      "hotelId",
      "roomType",
      "bedType",
      "price",
      "totalRooms",
      "availableRooms",
    ],
    optional: ["area", "images", "facilities", "status"],
  },
  {
    value: "attractions",
    label: "景点资源",
    required: [
      "name",
      "city",
      "address",
      "adultPrice",
      "childPrice",
      "totalTickets",
      "availableTickets",
    ],
    optional: [
      "description",
      "coverImg",
      "openTime",
      "lat",
      "lng",
      "officialUrl",
      "sourceName",
      "dataCheckedDate",
      "status",
    ],
  },
  {
    value: "destinations",
    label: "城市资源",
    required: ["slug", "name", "tag", "img", "desc", "intro"],
    optional: [
      "country",
      "keywords",
      "highlights",
      "culture",
      "bestSeason",
      "transport",
      "sourceName",
      "sourceUrl",
      "sortOrder",
      "status",
    ],
  },
];

const importForm = ref({
  type: "flights",
  mode: "insert",
  dryRun: true,
  file: null,
});

const currentImportType = computed(
  () =>
    importTypeOptions.find((item) => item.value === importForm.value.type) ||
    importTypeOptions[0],
);

const importFailures = computed(() =>
  Array.isArray(importResult.value?.failures) ? importResult.value.failures : [],
);

const createFlightForm = () => ({
  id: null,
  flightNo: "",
  airline: "",
  departureCity: "",
  arrivalCity: "",
  departureTime: "",
  arrivalTime: "",
  economyPrice: 0,
  businessPrice: 0,
  totalSeats: 200,
  availableSeats: 200,
  status: 1,
});

const createTrainForm = () => ({
  id: null,
  trainNo: "",
  trainType: "G",
  departureStation: "",
  arrivalStation: "",
  departureTime: "",
  arrivalTime: "",
  firstClassPrice: 0,
  secondClassPrice: 0,
  firstClassSeats: 100,
  secondClassSeats: 300,
  status: 1,
});

const createHotelForm = () => ({
  id: null,
  name: "",
  city: "",
  address: "",
  starRating: 4,
  description: "",
  coverImg: "",
  avgPrice: 0,
  score: 4.5,
  status: 1,
});

const createRoomForm = () => ({
  id: null,
  roomType: "",
  bedType: "",
  area: 30,
  price: 0,
  totalRooms: 10,
  availableRooms: 10,
  status: 1,
});

const createAttractionForm = () => ({
  id: null,
  name: "",
  city: "",
  address: "",
  description: "",
  coverImg: "",
  adultPrice: 0,
  childPrice: 0,
  totalTickets: 1000,
  availableTickets: 1000,
  openTime: "",
  lat: null,
  lng: null,
  officialUrl: "",
  sourceName: "",
  dataCheckedDate: "",
  status: 1,
});

const createCouponForm = () => ({
  id: null,
  name: "",
  description: "",
  category: "all",
  discountType: 0,
  discountValue: 0,
  minAmount: 0,
  expireDate: "",
  stock: 100,
  status: 0,
});

const createSensitiveForm = () => ({
  id: null,
  word: "",
  level: 1,
});

const flightForm = ref(createFlightForm());
const trainForm = ref(createTrainForm());
const hotelForm = ref(createHotelForm());
const roomForm = ref(createRoomForm());
const attractionForm = ref(createAttractionForm());
const couponForm = ref(createCouponForm());
const sensitiveForm = ref(createSensitiveForm());

const parseNumberish = (value, fallback = 0) => {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }
  const numericValue = Number(value);
  return Number.isNaN(numericValue) ? fallback : numericValue;
};

const normalizeFlight = (row = {}) => ({
  ...createFlightForm(),
  ...row,
  economyPrice: parseNumberish(row.economyPrice, 0),
  businessPrice: parseNumberish(row.businessPrice, 0),
  totalSeats: parseNumberish(row.totalSeats, 200),
  availableSeats: parseNumberish(row.availableSeats, 200),
  status: parseNumberish(row.status, 1),
});

const normalizeTrain = (row = {}) => ({
  ...createTrainForm(),
  ...row,
  firstClassPrice: parseNumberish(row.firstClassPrice, 0),
  secondClassPrice: parseNumberish(row.secondClassPrice, 0),
  firstClassSeats: parseNumberish(row.firstClassSeats, 100),
  secondClassSeats: parseNumberish(row.secondClassSeats, 300),
  status: parseNumberish(row.status, 1),
});

const normalizeHotel = (row = {}) => ({
  ...createHotelForm(),
  ...row,
  starRating: parseNumberish(row.starRating, 4),
  avgPrice: parseNumberish(row.avgPrice, 0),
  score: parseNumberish(row.score, 4.5),
  status: parseNumberish(row.status, 1),
});

const normalizeRoom = (row = {}) => ({
  ...createRoomForm(),
  ...row,
  area: parseNumberish(row.area, 30),
  price: parseNumberish(row.price, 0),
  totalRooms: parseNumberish(row.totalRooms, 10),
  availableRooms: parseNumberish(row.availableRooms, 10),
  status: parseNumberish(row.status, 1),
});

const normalizeAttraction = (row = {}) => ({
  ...createAttractionForm(),
  ...row,
  adultPrice: parseNumberish(row.adultPrice, 0),
  childPrice: parseNumberish(row.childPrice, 0),
  totalTickets: parseNumberish(row.totalTickets, 1000),
  availableTickets: parseNumberish(row.availableTickets, 1000),
  lat:
    row.lat === null || row.lat === undefined
      ? null
      : parseNumberish(row.lat, null),
  lng:
    row.lng === null || row.lng === undefined
      ? null
      : parseNumberish(row.lng, null),
  status: parseNumberish(row.status, 1),
});

const normalizeCoupon = (row = {}) => ({
  ...createCouponForm(),
  ...row,
  category: normalizeCouponCategory(row.category),
  discountType: parseNumberish(row.discountType, 0),
  discountValue: parseNumberish(row.discountValue, 0),
  minAmount: parseNumberish(row.minAmount, 0),
  stock: parseNumberish(row.stock, 100),
  status: parseNumberish(row.status, 0),
});

const normalizeCouponCategory = (category) => {
  const value = String(category || "all").toLowerCase();
  return ["all", "flight", "train", "hotel"].includes(value) ? value : "all";
};

const couponCategoryLabel = (category) =>
  ({
    all: "全部通用",
    flight: "机票",
    train: "火车票",
    hotel: "酒店",
  }[normalizeCouponCategory(category)]);

const latestMetricValue = (key) => {
  const list = dashboardData.value[key];
  if (!Array.isArray(list) || list.length === 0) {
    return 0;
  }
  return list[list.length - 1].value;
};

const latestQps = computed(() => latestMetricValue("qpsTrend"));
const latestLatency = computed(() => latestMetricValue("latencyTrend"));
const alerts = computed(() =>
  Array.isArray(dashboardData.value.alerts) ? dashboardData.value.alerts : [],
);

const setChartOption = (dom, option) => {
  if (!dom) {
    return;
  }
  const chart = echarts.getInstanceByDom(dom) || echarts.init(dom);
  chart.setOption(option);
};

const fetchDashboard = async () => {
  dashboardLoading.value = true;
  try {
    const data = await request.get("/api/admin/dashboard/data");
    dashboardData.value = data || {};
  } catch (error) {
    dashboardData.value = {};
  } finally {
    dashboardLoading.value = false;
    await nextTick();
    renderCharts();
  }
};

const renderCharts = () => {
  const data = dashboardData.value;
  setChartOption(trendChartRef.value, {
    title: {
      text: "近 7 天订单趋势",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: (data.dailyTrend || []).map((item) => item.day),
    },
    yAxis: { type: "value", name: "订单数" },
    grid: { left: 50, right: 24, top: 48, bottom: 36 },
    series: [
      {
        type: "line",
        smooth: true,
        data: (data.dailyTrend || []).map((item) => item.count),
        lineStyle: { color: "#0D9488", width: 3 },
        itemStyle: { color: "#0D9488" },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(13,148,136,0.22)" },
            { offset: 1, color: "rgba(13,148,136,0.02)" },
          ]),
        },
      },
    ],
  });

  setChartOption(typeChartRef.value, {
    title: {
      text: "订单类型分布",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: { trigger: "item", formatter: "{b}: {c} 单 ({d}%)" },
    color: ["#0D9488", "#F59E0B", "#3B82F6", "#EF4444"],
    series: [
      {
        type: "pie",
        radius: ["42%", "70%"],
        center: ["50%", "56%"],
        data: (data.orderTypeDist || []).map((item) => ({
          name: item.name,
          value: item.value,
        })),
        label: { formatter: "{b}\n{d}%" },
      },
    ],
  });

  setChartOption(destChartRef.value, {
    title: {
      text: "热门城市 Top10",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: (data.hotDestinations || []).map((item) => item.name),
      axisLabel: { rotate: 28 },
    },
    yAxis: { type: "value", name: "热度" },
    grid: { left: 50, right: 20, top: 48, bottom: 60 },
    series: [
      {
        type: "bar",
        barWidth: "52%",
        data: (data.hotDestinations || []).map((item) => item.count),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "#0D9488" },
            { offset: 1, color: "#5EEAD4" },
          ]),
        },
      },
    ],
  });

  setChartOption(growthChartRef.value, {
    title: {
      text: "用户增长趋势",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: (data.userGrowth || []).map((item) => item.day),
    },
    yAxis: { type: "value", name: "新增用户" },
    grid: { left: 50, right: 20, top: 48, bottom: 36 },
    series: [
      {
        type: "line",
        smooth: true,
        data: (data.userGrowth || []).map((item) => item.count),
        lineStyle: { color: "#6366F1", width: 3 },
        itemStyle: { color: "#6366F1" },
      },
    ],
  });

  setChartOption(qpsChartRef.value, {
    title: {
      text: "本地请求量监控",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: (data.qpsTrend || []).map((item) => item.time),
    },
    yAxis: { type: "value", name: "QPS" },
    grid: { left: 50, right: 20, top: 48, bottom: 36 },
    series: [
      {
        type: "line",
        smooth: true,
        data: (data.qpsTrend || []).map((item) => item.value),
        lineStyle: { color: "#F59E0B", width: 3 },
        itemStyle: { color: "#F59E0B" },
        areaStyle: { color: "rgba(245,158,11,0.15)" },
      },
    ],
  });

  setChartOption(latencyChartRef.value, {
    title: {
      text: "接口平均延迟监控",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: (data.latencyTrend || []).map((item) => item.time),
    },
    yAxis: { type: "value", name: "ms" },
    grid: { left: 50, right: 20, top: 48, bottom: 36 },
    series: [
      {
        type: "line",
        smooth: true,
        data: (data.latencyTrend || []).map((item) => item.value),
        lineStyle: { color: "#EF4444", width: 3 },
        itemStyle: { color: "#EF4444" },
        areaStyle: { color: "rgba(239,68,68,0.12)" },
      },
    ],
  });
};

const resizeCharts = () => {
  [
    trendChartRef,
    typeChartRef,
    destChartRef,
    growthChartRef,
    qpsChartRef,
    latencyChartRef,
  ].forEach((chartRef) => {
    if (!chartRef.value) {
      return;
    }
    const chart = echarts.getInstanceByDom(chartRef.value);
    if (chart) {
      chart.resize();
    }
  });
};

const fetchFlights = async () => {
  flightLoading.value = true;
  try {
    const data = await request.get("/api/admin/flights");
    flights.value = Array.isArray(data) ? data : [];
  } catch (error) {
    flights.value = [];
  } finally {
    flightLoading.value = false;
  }
};

const fetchTrains = async () => {
  trainLoading.value = true;
  try {
    const data = await request.get("/api/admin/trains");
    trains.value = Array.isArray(data) ? data : [];
  } catch (error) {
    trains.value = [];
  } finally {
    trainLoading.value = false;
  }
};

const fetchHotels = async () => {
  hotelLoading.value = true;
  try {
    const data = await request.get("/api/admin/hotels");
    hotels.value = Array.isArray(data) ? data : [];
  } catch (error) {
    hotels.value = [];
  } finally {
    hotelLoading.value = false;
  }
};

const fetchHotelRooms = async (hotelId) => {
  if (!hotelId) {
    hotelRooms.value = [];
    return;
  }
  roomLoading.value = true;
  try {
    const data = await request.get(`/api/admin/hotels/${hotelId}/rooms`);
    hotelRooms.value = Array.isArray(data) ? data : [];
  } catch (error) {
    hotelRooms.value = [];
  } finally {
    roomLoading.value = false;
  }
};

const fetchAttractions = async () => {
  attractionLoading.value = true;
  try {
    const data = await request.get("/api/admin/attractions");
    attractions.value = Array.isArray(data) ? data : [];
  } catch (error) {
    attractions.value = [];
  } finally {
    attractionLoading.value = false;
  }
};

const fetchDestinations = async () => {
  destinationLoading.value = true;
  try {
    const data = await request.get("/api/admin/destinations");
    destinations.value = Array.isArray(data) ? data : [];
  } catch (error) {
    destinations.value = [];
  } finally {
    destinationLoading.value = false;
  }
};

const fetchCoupons = async () => {
  couponLoading.value = true;
  try {
    const data = await request.get("/api/admin/coupons");
    coupons.value = Array.isArray(data) ? data : [];
  } catch (error) {
    coupons.value = [];
  } finally {
    couponLoading.value = false;
  }
};

const fetchCouponClaims = async (couponId) => {
  couponClaimsLoading.value = true;
  try {
    const data = await request.get(`/api/admin/coupons/${couponId}/claims`);
    couponClaims.value = Array.isArray(data) ? data : [];
  } catch (error) {
    couponClaims.value = [];
  } finally {
    couponClaimsLoading.value = false;
  }
};

const fetchOrders = async () => {
  orderLoading.value = true;
  try {
    const data = await request.get("/api/admin/orders", {
      params: {
        type: orderTypeFilter.value,
        status: orderStatusFilter.value,
        page: orderPage.value,
        size: orderSize.value,
      },
    });
    orders.value = Array.isArray(data?.records) ? data.records : [];
    orderTotal.value = data?.total || 0;
  } catch (error) {
    orders.value = [];
    orderTotal.value = 0;
  } finally {
    orderLoading.value = false;
  }
};

const fetchPosts = async () => {
  postLoading.value = true;
  try {
    const statusMap = { pending: 0, approved: 1, rejected: 2 };
    const params =
      postStatusFilter.value === "all"
        ? {}
        : { status: statusMap[postStatusFilter.value] };
    const data = await request.get("/api/admin/posts", { params });
    reviewPosts.value = Array.isArray(data)
      ? data.map((post) => ({
          ...post,
          likeCount: Number(post.likeCount || 0),
          collectCount: Number(post.collectCount || 0),
        }))
      : [];
  } catch (error) {
    reviewPosts.value = [];
  } finally {
    postLoading.value = false;
  }
};

const fetchReviewReports = async () => {
  reportLoading.value = true;
  try {
    const data = await request.get("/api/admin/review-reports");
    reviewReports.value = Array.isArray(data) ? data : [];
  } catch (error) {
    reviewReports.value = [];
  } finally {
    reportLoading.value = false;
  }
};

const fetchReviewReplies = async (reviewId) => {
  replyLoading.value = true;
  try {
    const data = await request.get(`/api/admin/reviews/${reviewId}/replies`);
    reviewReplies.value = Array.isArray(data) ? data : [];
  } catch (error) {
    reviewReplies.value = [];
  } finally {
    replyLoading.value = false;
  }
};

const fetchSensitiveWords = async () => {
  sensitiveLoading.value = true;
  try {
    const data = await request.get("/api/admin/sensitive-words");
    sensitiveWords.value = Array.isArray(data) ? data : [];
  } catch (error) {
    sensitiveWords.value = [];
  } finally {
    sensitiveLoading.value = false;
  }
};

const fetchLogs = async () => {
  logLoading.value = true;
  try {
    const data = await request.get("/api/admin/logs", {
      params: { page: logPage.value, size: logSize.value },
    });
    logs.value = Array.isArray(data?.records) ? data.records : [];
    logTotal.value = data?.total || 0;
  } catch (error) {
    logs.value = [];
    logTotal.value = 0;
  } finally {
    logLoading.value = false;
  }
};

const fetchUsers = async () => {
  userLoading.value = true;
  try {
    const data = await request.get("/api/admin/users");
    users.value = Array.isArray(data) ? data : [];
  } catch (error) {
    users.value = [];
  } finally {
    userLoading.value = false;
  }
};

const openFlightDialog = (row = null) => {
  flightForm.value = normalizeFlight(row || {});
  flightDialogVisible.value = true;
};

const saveFlight = async () => {
  const payload = { ...flightForm.value };
  try {
    if (payload.id) {
      await request.put(`/api/admin/flights/${payload.id}`, payload);
      ElMessage.success("航班已更新");
    } else {
      await request.post("/api/admin/flights", payload);
      ElMessage.success("航班已新增");
    }
    flightDialogVisible.value = false;
    await fetchFlights();
  } catch (error) {}
};

const removeFlight = async (row) => {
  await ElMessageBox.confirm(`确认删除航班 ${row.flightNo} 吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "暂不删除",
  });
  try {
    await request.delete(`/api/admin/flights/${row.id}`);
    ElMessage.success("航班已删除");
    await fetchFlights();
  } catch (error) {}
};

const openTrainDialog = (row = null) => {
  trainForm.value = normalizeTrain(row || {});
  trainDialogVisible.value = true;
};

const saveTrain = async () => {
  const payload = { ...trainForm.value };
  try {
    if (payload.id) {
      await request.put(`/api/admin/trains/${payload.id}`, payload);
      ElMessage.success("车次已更新");
    } else {
      await request.post("/api/admin/trains", payload);
      ElMessage.success("车次已新增");
    }
    trainDialogVisible.value = false;
    await fetchTrains();
  } catch (error) {}
};

const removeTrain = async (row) => {
  await ElMessageBox.confirm(`确认删除车次 ${row.trainNo} 吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "暂不删除",
  });
  try {
    await request.delete(`/api/admin/trains/${row.id}`);
    ElMessage.success("车次已删除");
    await fetchTrains();
  } catch (error) {}
};

const openHotelDialog = (row = null) => {
  hotelForm.value = normalizeHotel(row || {});
  hotelDialogVisible.value = true;
};

const saveHotel = async () => {
  const payload = { ...hotelForm.value };
  try {
    if (payload.id) {
      await request.put(`/api/admin/hotels/${payload.id}`, payload);
      ElMessage.success("酒店已更新");
    } else {
      await request.post("/api/admin/hotels", payload);
      ElMessage.success("酒店已新增");
    }
    hotelDialogVisible.value = false;
    await fetchHotels();
  } catch (error) {}
};

const removeHotel = async (row) => {
  await ElMessageBox.confirm(`确认删除酒店 ${row.name} 吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "暂不删除",
  });
  try {
    await request.delete(`/api/admin/hotels/${row.id}`);
    ElMessage.success("酒店已删除");
    await fetchHotels();
  } catch (error) {}
};

const openRoomDrawer = async (hotel) => {
  activeHotel.value = hotel;
  roomDrawerVisible.value = true;
  await fetchHotelRooms(hotel.id);
};

const openRoomDialog = (row = null) => {
  roomForm.value = normalizeRoom(row || {});
  roomDialogVisible.value = true;
};

const saveRoom = async () => {
  if (!activeHotel.value) {
    return;
  }
  const payload = { ...roomForm.value, hotelId: activeHotel.value.id };
  try {
    if (payload.id) {
      await request.put(`/api/admin/hotel-rooms/${payload.id}`, payload);
      ElMessage.success("房型已更新");
    } else {
      await request.post(
        `/api/admin/hotels/${activeHotel.value.id}/rooms`,
        payload,
      );
      ElMessage.success("房型已新增");
    }
    roomDialogVisible.value = false;
    await fetchHotelRooms(activeHotel.value.id);
  } catch (error) {}
};

const removeRoom = async (row) => {
  await ElMessageBox.confirm(`确认删除房型 ${row.roomType} 吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "暂不删除",
  });
  try {
    await request.delete(`/api/admin/hotel-rooms/${row.id}`);
    ElMessage.success("房型已删除");
    await fetchHotelRooms(activeHotel.value?.id);
  } catch (error) {}
};

const openAttractionDialog = (row = null) => {
  attractionForm.value = normalizeAttraction(row || {});
  attractionDialogVisible.value = true;
};

const saveAttraction = async () => {
  const payload = { ...attractionForm.value };
  try {
    if (payload.id) {
      await request.put(`/api/admin/attractions/${payload.id}`, payload);
      ElMessage.success("景点已更新");
    } else {
      await request.post("/api/admin/attractions", payload);
      ElMessage.success("景点已新增");
    }
    attractionDialogVisible.value = false;
    await fetchAttractions();
  } catch (error) {}
};

const removeAttraction = async (row) => {
  await ElMessageBox.confirm(
    `确认删除或下线景点 ${row.name} 吗？`,
    "删除确认",
    {
      type: "warning",
      confirmButtonText: "确认",
      cancelButtonText: "取消",
    },
  );
  try {
    await request.delete(`/api/admin/attractions/${row.id}`);
    ElMessage.success("景点已删除或下线");
    await fetchAttractions();
  } catch (error) {}
};

const removeDestination = async (row) => {
  await ElMessageBox.confirm(`确认下线城市 ${row.name} 吗？`, "下线确认", {
    type: "warning",
    confirmButtonText: "确认下线",
    cancelButtonText: "取消",
  });
  try {
    await request.delete(`/api/admin/destinations/${row.id}`);
    ElMessage.success("城市已下线");
    await fetchDestinations();
  } catch (error) {}
};

const triggerImport = (type) => {
  importForm.value = {
    type,
    mode: "insert",
    dryRun: true,
    file: null,
  };
  importResult.value = null;
  importDialogVisible.value = true;
};

const handleImportFileChange = (uploadFile) => {
  importForm.value.file = uploadFile.raw;
  importResult.value = null;
};

const clearImportFile = () => {
  importForm.value.file = null;
};

const submitCsvImport = async () => {
  if (!importForm.value.file) {
    ElMessage.warning("请先选择 CSV 文件");
    return;
  }
  const form = new FormData();
  form.append("file", importForm.value.file);
  importLoading.value = true;
  try {
    const result = await request.post(
      `/api/admin/import/${importForm.value.type}`,
      form,
      {
        headers: { "Content-Type": "multipart/form-data" },
        params: {
          dryRun: importForm.value.dryRun,
          mode: importForm.value.mode,
        },
      },
    );
    importResult.value = result || {};
    if (result?.failed) {
      ElMessage.warning("CSV 已处理，请查看失败行");
    } else {
      ElMessage.success(importForm.value.dryRun ? "预检通过" : "导入完成");
    }
    if (!importForm.value.dryRun && result?.success) {
      await refreshResourceAfterImport(importForm.value.type);
    }
  } catch (error) {
    importResult.value = null;
  } finally {
    importLoading.value = false;
  }
};

const escapeCsvCell = (value) => {
  const text = String(value ?? "");
  return /[",\n\r]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
};

const downloadCsvTemplate = () => {
  const type = currentImportType.value;
  const headers = [...type.required, ...type.optional];
  const examples = {
    flights: [
      "CA1001",
      "中国国际航空",
      "北京",
      "上海",
      "2026-06-01T08:00:00",
      "2026-06-01T10:00:00",
      "680",
      "2180",
      "200",
      "120",
      "1",
    ],
    trains: [
      "G1001",
      "G",
      "北京南",
      "上海虹桥",
      "2026-06-01T08:00:00",
      "2026-06-01T12:30:00",
      "880",
      "553",
      "80",
      "420",
      "270",
      "1",
    ],
    hotels: [
      "城市花园酒店",
      "上海",
      "上海市黄浦区示例路1号",
      "4",
      "520",
      "近地铁商务酒店",
      "https://example.com/hotel.jpg",
      "31.2304",
      "121.4737",
      "4.6",
      "1",
    ],
    rooms: [
      "1",
      "豪华大床房",
      "1张大床",
      "688",
      "20",
      "12",
      "38",
      '["/images/seed/hotel.svg"]',
      '["早餐","洗衣房"]',
      "1",
    ],
    attractions: [
      "示例景区",
      "杭州",
      "杭州市示例路1号",
      "80",
      "40",
      "1000",
      "800",
      "城市观光景区",
      "https://example.com/scenic.jpg",
      "08:00-18:00",
      "30.2741",
      "120.1551",
      "https://example.com",
      "景区官网",
      "2026-05-30",
      "1",
    ],
    destinations: [
      "dali",
      "大理",
      "风花雪月",
      "/images/seed/lake.svg",
      "苍山洱海与古城生活交织",
      "适合慢旅行的城市",
      "中国",
      "洱海|古城|苍山",
      "洱海骑行|古城夜游",
      "白族文化",
      "春秋季",
      "高铁到大理站",
      "公开旅游资料",
      "https://example.com",
      "90",
      "1",
    ],
  };
  const lines = [
    headers.map(escapeCsvCell).join(","),
    (examples[type.value] || []).map(escapeCsvCell).join(","),
  ];
  const blob = new Blob([`\uFEFF${lines.join("\n")}`], {
    type: "text/csv;charset=utf-8",
  });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = `travelmate-${type.value}-template.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
};

const refreshResourceAfterImport = async (type) => {
  if (type === "flights") return fetchFlights();
  if (type === "trains") return fetchTrains();
  if (type === "hotels") return fetchHotels();
  if (type === "rooms") return fetchHotelRooms(activeHotel.value?.id);
  if (type === "attractions") return fetchAttractions();
  if (type === "destinations") return fetchDestinations();
};

const openCouponDialog = (row = null) => {
  couponForm.value = normalizeCoupon(row || {});
  couponDialogVisible.value = true;
};

const openCouponClaims = async (row) => {
  activeCoupon.value = row;
  couponClaimsDrawerVisible.value = true;
  await fetchCouponClaims(row.id);
};

const saveCoupon = async () => {
  const payload = { ...couponForm.value };
  try {
    if (payload.id) {
      await request.put(`/api/admin/coupons/${payload.id}`, payload);
      ElMessage.success("优惠券已更新");
    } else {
      await request.post("/api/admin/coupons", payload);
      ElMessage.success("优惠券已新增");
    }
    couponDialogVisible.value = false;
    await fetchCoupons();
  } catch (error) {}
};

const removeCoupon = async (row) => {
  await ElMessageBox.confirm(`确认删除优惠券 ${row.name} 吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "暂不删除",
  });
  try {
    await request.delete(`/api/admin/coupons/${row.id}`);
    ElMessage.success("优惠券已删除或下架");
    await fetchCoupons();
  } catch (error) {}
};

const couponClaimStatusLabel = (status) =>
  ({ 0: "未使用", 1: "已使用", 2: "已过期" }[status] || `状态${status}`);

const saveSensitiveWord = async () => {
  try {
    if (sensitiveForm.value.id) {
      await request.put(
        `/api/admin/sensitive-words/${sensitiveForm.value.id}`,
        sensitiveForm.value,
      );
      ElMessage.success("敏感词已更新");
    } else {
      await request.post("/api/admin/sensitive-words", sensitiveForm.value);
      ElMessage.success("敏感词已新增");
    }
    sensitiveDialogVisible.value = false;
    sensitiveForm.value = createSensitiveForm();
    await fetchSensitiveWords();
  } catch (error) {}
};

const openSensitiveDialog = (row = null) => {
  sensitiveForm.value = { ...createSensitiveForm(), ...(row || {}) };
  sensitiveDialogVisible.value = true;
};

const removeSensitiveWord = async (row) => {
  await ElMessageBox.confirm(`确认删除敏感词 ${row.word} 吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "暂不删除",
  });
  try {
    await request.delete(`/api/admin/sensitive-words/${row.id}`);
    ElMessage.success("敏感词已删除");
    await fetchSensitiveWords();
  } catch (error) {}
};

const openPostMetricsDialog = (row) => {
  postMetricsForm.value = {
    id: row.id,
    title: row.title || "",
    likeCount: Number(row.likeCount || 0),
    collectCount: Number(row.collectCount || 0),
  };
  postMetricsDialogVisible.value = true;
};

const savePostMetrics = async () => {
  const likeCount = Number(postMetricsForm.value.likeCount);
  const collectCount = Number(postMetricsForm.value.collectCount);
  if (
    !Number.isInteger(likeCount) ||
    likeCount < 0 ||
    !Number.isInteger(collectCount) ||
    collectCount < 0
  ) {
    ElMessage.warning("点赞量和收藏量必须是非负整数");
    return;
  }
  try {
    await request.post(`/api/admin/posts/${postMetricsForm.value.id}/metrics`, {
      likeCount,
      collectCount,
    });
    ElMessage.success("互动数据已保存");
    postMetricsDialogVisible.value = false;
    await fetchPosts();
  } catch (error) {}
};

const approvePost = async (id) => {
  try {
    await request.post(`/api/admin/posts/${id}/approve`);
    ElMessage.success("审核已通过");
    await fetchPosts();
    if (activeMenu.value === "stats") {
      await fetchDashboard();
    }
  } catch (error) {}
};

const rejectPost = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt("请输入拒绝原因", "审核拒绝", {
      inputValue: "内容不符合社区规范",
      inputPattern: /\S+/,
      inputErrorMessage: "拒绝原因不能为空",
      confirmButtonText: "确认拒绝",
      cancelButtonText: "暂不处理",
    });
    await request.post(`/api/admin/posts/${id}/reject`, { reason: value });
    ElMessage.success("审核已拒绝");
    await fetchPosts();
    if (activeMenu.value === "stats") {
      await fetchDashboard();
    }
  } catch (error) {}
};

const disablePostAuthor = async (row) => {
  if (!row.userId) return;
  await ElMessageBox.confirm(
    `确认封禁作者 ${row.authorUsername || row.userId} 吗？`,
    "封禁确认",
    {
      type: "warning",
      confirmButtonText: "确认封禁",
      cancelButtonText: "取消",
    },
  );
  try {
    await request.post(`/api/admin/users/${row.userId}/disable`);
    ElMessage.success("作者已封禁");
    await fetchPosts();
  } catch (error) {}
};

const resolveReviewReport = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt("请输入处理备注", "举报处理", {
      inputValue: "已人工复核",
      inputPattern: /\S+/,
      inputErrorMessage: "处理备注不能为空",
      confirmButtonText: "确认处理",
      cancelButtonText: "暂不处理",
    });
    await request.post(`/api/admin/review-reports/${id}/resolve`, {
      remark: value,
    });
    ElMessage.success("举报工单已处理");
    await fetchReviewReports();
  } catch (error) {}
};

const rejectReviewReport = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt("请输入驳回原因", "驳回举报", {
      inputValue: "举报不成立，已驳回",
      inputPattern: /\S+/,
      inputErrorMessage: "驳回原因不能为空",
      confirmButtonText: "确认驳回",
      cancelButtonText: "取消",
    });
    await request.post(`/api/admin/review-reports/${id}/reject`, {
      remark: value,
    });
    ElMessage.success("举报已驳回");
    await fetchReviewReports();
  } catch (error) {}
};

const deleteReportedReview = async (id) => {
  await ElMessageBox.confirm("确认删除被举报评价并关闭工单吗？", "删除评价", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "取消",
  });
  try {
    await request.post(`/api/admin/review-reports/${id}/delete-review`, {
      remark: "举报成立，评价已删除",
    });
    ElMessage.success("评价已删除");
    await fetchReviewReports();
  } catch (error) {}
};

const openReplyDrawer = async (row) => {
  activeReport.value = row;
  replyContent.value = "";
  replyDrawerVisible.value = true;
  await fetchReviewReplies(row.reviewId);
};

const saveReply = async () => {
  if (!activeReport.value?.reviewId) return;
  try {
    await request.post(
      `/api/admin/reviews/${activeReport.value.reviewId}/replies`,
      {
        content: replyContent.value,
      },
    );
    ElMessage.success("商家回复已发布");
    replyContent.value = "";
    await fetchReviewReplies(activeReport.value.reviewId);
  } catch (error) {}
};

const removeReply = async (row) => {
  await ElMessageBox.confirm("确认删除该回复吗？", "删除回复", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "取消",
  });
  try {
    await request.delete(`/api/admin/replies/${row.id}`);
    ElMessage.success("回复已删除");
    await fetchReviewReplies(activeReport.value?.reviewId);
  } catch (error) {}
};

const openUserDrawer = (user) => {
  selectedUser.value = user;
  userDrawerVisible.value = true;
};

const toggleUserStatus = async (user) => {
  const action = user.status === 1 ? "disable" : "enable";
  const actionText = user.status === 1 ? "禁用" : "启用";
  try {
    await request.post(`/api/admin/users/${user.id}/${action}`);
    ElMessage.success(`用户已${actionText}`);
    await fetchUsers();
    if (selectedUser.value?.id === user.id) {
      selectedUser.value = {
        ...selectedUser.value,
        status: user.status === 1 ? 0 : 1,
      };
    }
  } catch (error) {}
};

const approveRefund = async (orderNo) => {
  try {
    await request.post(`/api/admin/orders/${orderNo}/refund/approve`);
    ElMessage.success("退款审批已通过");
    await fetchOrders();
  } catch (error) {}
};

const rejectRefund = async (orderNo) => {
  try {
    await request.post(`/api/admin/orders/${orderNo}/refund/reject`);
    ElMessage.success("退款申请已驳回");
    await fetchOrders();
  } catch (error) {}
};

const canReviewRefund = (row) => {
  return row.status === 5;
};

const orderStatusLabel = (row) => {
  if (row.type === "酒店") {
    return (
      [
        "待支付",
        "已支付",
        "入住中",
        "已完成",
        "已取消/已退款",
        "退款申请中",
      ][row.status] || `状态${row.status}`
    );
  }
  return (
    [
      "待支付",
      "出票中",
      "已出票",
      "已取消",
      "已退票/已退款",
      "退票申请中",
    ][row.status] || `状态${row.status}`
  );
};

const orderStatusType = (row) => {
  if (row.status === 0) {
    return "warning";
  }
  if (row.status === 1 || row.status === 2) {
    return "success";
  }
  if (row.status === 4) {
    return "info";
  }
  if (row.status === 5) {
    return "danger";
  }
  return "info";
};

const postStatusLabel = (status) =>
  ["待审核", "已通过", "已拒绝"][status] || `状态${status}`;
const postStatusType = (status) =>
  status === 0 ? "warning" : status === 1 ? "success" : "danger";
const mapAlertType = (level) =>
  ({ danger: "danger", warning: "warning", info: "info", success: "success" }[
    level
  ] || "info");

const handleOrderTypeChange = () => {
  orderPage.value = 1;
  fetchOrders();
};

const handleAuditTabChange = () => {
  if (reviewAuditTab.value === "posts") {
    fetchPosts();
  } else {
    fetchReviewReports();
  }
};

const handleMenuSelect = (menu) => {
  activeMenu.value = menu;
  if (menu === "stats") {
    fetchDashboard();
  } else if (menu === "flights") {
    fetchFlights();
  } else if (menu === "trains") {
    fetchTrains();
  } else if (menu === "hotels") {
    fetchHotels();
  } else if (menu === "attractions") {
    fetchAttractions();
  } else if (menu === "destinations") {
    fetchDestinations();
  } else if (menu === "coupons") {
    fetchCoupons();
  } else if (menu === "orders") {
    fetchOrders();
  } else if (menu === "posts") {
    handleAuditTabChange();
  } else if (menu === "sensitive") {
    fetchSensitiveWords();
  } else if (menu === "logs") {
    fetchLogs();
  } else if (menu === "users") {
    fetchUsers();
  }
};

onMounted(() => {
  fetchDashboard();
  window.addEventListener("resize", resizeCharts);
});

onUnmounted(() => {
  window.removeEventListener("resize", resizeCharts);
  [
    trendChartRef,
    typeChartRef,
    destChartRef,
    growthChartRef,
    qpsChartRef,
    latencyChartRef,
  ].forEach((chartRef) => {
    if (!chartRef.value) {
      return;
    }
    const chart = echarts.getInstanceByDom(chartRef.value);
    if (chart) {
      chart.dispose();
    }
  });
});
</script>

<style scoped>
.admin-page {
  margin: -24px -40px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(180deg, #f6fbff 0%, #ffffff 46%, #f8fbff 100%);
}

.reply-editor {
  display: grid;
  gap: 12px;
  margin: 16px 0;
}

.admin-aside {
  background: linear-gradient(180deg, #ffffff 0%, #f3fbff 100%);
  min-height: calc(100vh - 60px);
  border-right: 1px solid #dbeafe;
  box-shadow: 10px 0 30px rgba(59, 130, 246, 0.08);
}

.admin-logo {
  color: #0f766e;
  font-size: 18px;
  font-weight: 700;
  padding: 20px 20px 16px;
  border-bottom: 1px solid #dbeafe;
}

.admin-menu {
  background: transparent;
  border-right: none;
  padding: 8px 10px;
}

.admin-menu :deep(.el-menu-item) {
  height: 44px;
  margin: 4px 0;
  color: #475569;
  border-radius: 8px;
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item:hover),
.admin-menu :deep(.el-menu-item.is-active) {
  background: #e7f8f4;
  color: #0f766e;
}

.admin-main {
  background: radial-gradient(
      circle at 88% 8%,
      rgba(20, 184, 166, 0.12),
      transparent 28%
    ),
    linear-gradient(180deg, #f7fcff 0%, #ffffff 56%, #f8fbff 100%);
  padding: 24px;
}

.section-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-inline {
  margin-bottom: 12px;
}

.stat-row,
.chart-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  border-radius: 8px;
}

.stat-card,
.panel-card,
.alert-card {
  border: 1px solid #dbeafe;
  box-shadow: 0 14px 36px rgba(15, 118, 110, 0.08);
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #0f766e;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.panel-card,
.alert-card {
  border-radius: 8px;
}

:deep(.el-table) {
  --el-table-header-bg-color: #eff8ff;
  --el-table-header-text-color: #0f172a;
  --el-table-row-hover-bg-color: #eefcf8;
  --el-table-border-color: #e5eef8;
  color: #334155;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
}

:deep(.el-table th.el-table__cell) {
  font-weight: 700;
}

:deep(
    .el-table--striped
      .el-table__body
      tr.el-table__row--striped
      td.el-table__cell
  ) {
  background: #fbfdff;
}

:deep(.el-table__fixed-right) {
  box-shadow: -10px 0 18px rgba(15, 23, 42, 0.05);
}

:deep(.el-button--primary) {
  --el-button-bg-color: #14b8a6;
  --el-button-border-color: #14b8a6;
  --el-button-hover-bg-color: #0d9488;
  --el-button-hover-border-color: #0d9488;
}

:deep(.el-button--warning) {
  --el-button-bg-color: #f4b860;
  --el-button-border-color: #f4b860;
  --el-button-hover-bg-color: #e59e35;
  --el-button-hover-border-color: #e59e35;
}

:deep(.el-button--danger) {
  --el-button-bg-color: #e60012;
  --el-button-border-color: #e60012;
  --el-button-hover-bg-color: #c7000b;
  --el-button-hover-border-color: #c7000b;
}

.chart-panel {
  width: 100%;
  height: 300px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.alert-list {
  display: grid;
  gap: 12px;
}

.alert-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 8px;
}

.alert-message {
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

.pager-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.entity-form {
  padding-top: 6px;
}

.muted-text {
  color: var(--el-text-color-secondary);
}

.post-audit-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.post-audit-actions :deep(.el-button) {
  margin-left: 0;
}

.import-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.import-panel {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 14px;
  background: #fbfdff;
}

.csv-upload {
  margin-top: 14px;
}

.import-help-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-weight: 700;
  color: #0f172a;
}

.field-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.import-result,
.import-failure-table {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .import-grid {
    grid-template-columns: 1fr;
  }
}
</style>
