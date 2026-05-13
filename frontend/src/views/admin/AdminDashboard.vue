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
          <el-menu-item index="hotels"
            ><el-icon><House /></el-icon><span>酒店与房态</span></el-menu-item
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
            <el-col :span="4"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.totalUsers || 0 }}
                </div>
                <div class="stat-label">总用户数</div></el-card
              ></el-col
            >
            <el-col :span="4"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.totalOrders || 0 }}
                </div>
                <div class="stat-label">总订单数</div></el-card
              ></el-col
            >
            <el-col :span="4"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.todayOrders || 0 }}
                </div>
                <div class="stat-label">今日订单</div></el-card
              ></el-col
            >
            <el-col :span="4"
              ><el-card class="stat-card"
                ><div class="stat-value">
                  {{ dashboardData.pendingPosts || 0 }}
                </div>
                <div class="stat-label">待审核内容</div></el-card
              ></el-col
            >
            <el-col :span="4"
              ><el-card class="stat-card"
                ><div class="stat-value">{{ latestQps }}</div>
                <div class="stat-label">当前 QPS（模拟）</div></el-card
              ></el-col
            >
            <el-col :span="4"
              ><el-card class="stat-card"
                ><div class="stat-value">{{ latestLatency }}ms</div>
                <div class="stat-label">当前延迟（模拟）</div></el-card
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
            <el-button type="primary" @click="openFlightDialog()"
              >新增航班</el-button
            >
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
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="openFlightDialog(scope.row)"
                  >编辑</el-button
                >
                <el-button
                  size="small"
                  type="warning"
                  @click="
                    openFlightDialog({
                      ...scope.row,
                      availableSeats: scope.row.availableSeats,
                    })
                  "
                  >改库存</el-button
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

        <section v-else-if="activeMenu === 'hotels'">
          <div class="toolbar">
            <h2 class="section-title">酒店与房态管理</h2>
            <el-button type="primary" @click="openHotelDialog()"
              >新增酒店</el-button
            >
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

        <section v-else-if="activeMenu === 'coupons'">
          <div class="toolbar">
            <h2 class="section-title">促销券配置</h2>
            <el-button type="primary" @click="openCouponDialog()"
              >新增优惠券</el-button
            >
          </div>
          <el-table :data="coupons" v-loading="couponLoading" stripe>
            <el-table-column prop="name" label="名称" min-width="160" />
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
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
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
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="canReviewRefund(scope.row)"
                  size="small"
                  type="success"
                  @click="approveRefund(scope.row.orderNo)"
                  >通过退款</el-button
                >
                <el-button
                  v-if="canReviewRefund(scope.row)"
                  size="small"
                  type="danger"
                  @click="rejectRefund(scope.row.orderNo)"
                  >拒绝</el-button
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
                  prop="authorUsername"
                  label="作者"
                  width="120"
                />
                <el-table-column
                  prop="destination"
                  label="目的地"
                  width="120"
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
                <el-table-column label="操作" width="180" fixed="right">
                  <template #default="scope">
                    <el-button
                      v-if="scope.row.status === 0"
                      size="small"
                      type="success"
                      @click="approvePost(scope.row.id)"
                      >通过</el-button
                    >
                    <el-button
                      v-if="scope.row.status === 0"
                      size="small"
                      type="danger"
                      @click="rejectPost(scope.row.id)"
                      >拒绝</el-button
                    >
                    <span v-else class="muted-text">已处理</span>
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
                  prop="reporterId"
                  label="举报人ID"
                  width="110"
                />
                <el-table-column
                  prop="reason"
                  label="举报原因"
                  min-width="220"
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
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="scope">
                    <el-button
                      v-if="scope.row.status === 0"
                      size="small"
                      type="primary"
                      @click="resolveReviewReport(scope.row.id)"
                      >标记已处理</el-button
                    >
                    <span v-else class="muted-text">已完成</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </section>

        <section v-else-if="activeMenu === 'sensitive'">
          <div class="toolbar">
            <h2 class="section-title">敏感词过滤配置</h2>
            <el-button type="primary" @click="sensitiveDialogVisible = true"
              >新增敏感词</el-button
            >
          </div>
          <el-table :data="sensitiveWords" v-loading="sensitiveLoading" stripe>
            <el-table-column prop="word" label="敏感词" min-width="180" />
            <el-table-column prop="level" label="等级" width="100" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope"
                ><el-button
                  size="small"
                  type="danger"
                  @click="removeSensitiveWord(scope.row)"
                  >删除</el-button
                ></template
              >
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
              layout="prev, pager, next"
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

    <el-drawer
      v-model="roomDrawerVisible"
      :title="
        activeHotel ? `${activeHotel.name} · 房态库存管理` : '房态库存管理'
      "
      size="760px"
    >
      <div class="toolbar toolbar-inline">
        <div class="muted-text">成员 E 可在此干预房态、价格和上下架。</div>
        <el-button type="primary" @click="openRoomDialog()">新增房型</el-button>
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
                value-format="YYYY-MM-DD HH:mm:ss"
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
      title="新增敏感词"
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
const hotels = ref([]);
const hotelRooms = ref([]);
const coupons = ref([]);
const orders = ref([]);
const reviewPosts = ref([]);
const reviewReports = ref([]);
const sensitiveWords = ref([]);
const logs = ref([]);
const users = ref([]);

const flightLoading = ref(false);
const hotelLoading = ref(false);
const roomLoading = ref(false);
const couponLoading = ref(false);
const orderLoading = ref(false);
const postLoading = ref(false);
const reportLoading = ref(false);
const sensitiveLoading = ref(false);
const logLoading = ref(false);
const userLoading = ref(false);

const orderTypeFilter = ref("all");
const orderPage = ref(1);
const orderSize = ref(20);
const orderTotal = ref(0);

const postStatusFilter = ref("pending");
const reviewAuditTab = ref("posts");

const logPage = ref(1);
const logSize = ref(10);
const logTotal = ref(0);

const flightDialogVisible = ref(false);
const hotelDialogVisible = ref(false);
const roomDrawerVisible = ref(false);
const roomDialogVisible = ref(false);
const couponDialogVisible = ref(false);
const sensitiveDialogVisible = ref(false);
const userDrawerVisible = ref(false);

const activeHotel = ref(null);
const selectedUser = ref(null);

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

const createHotelForm = () => ({
  id: null,
  name: "",
  city: "",
  address: "",
  starRating: 4,
  description: "",
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

const createCouponForm = () => ({
  id: null,
  name: "",
  description: "",
  discountType: 0,
  discountValue: 0,
  minAmount: 0,
  expireDate: "",
  stock: 100,
  status: 0,
});

const createSensitiveForm = () => ({
  word: "",
  level: 1,
});

const flightForm = ref(createFlightForm());
const hotelForm = ref(createHotelForm());
const roomForm = ref(createRoomForm());
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

const normalizeCoupon = (row = {}) => ({
  ...createCouponForm(),
  ...row,
  discountType: parseNumberish(row.discountType, 0),
  discountValue: parseNumberish(row.discountValue, 0),
  minAmount: parseNumberish(row.minAmount, 0),
  stock: parseNumberish(row.stock, 100),
  status: parseNumberish(row.status, 0),
});

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
      text: "热门目的地 Top10",
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
      text: "系统 QPS 监控（模拟）",
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
      text: "接口延迟监控（模拟）",
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

const fetchOrders = async () => {
  orderLoading.value = true;
  try {
    const data = await request.get("/api/admin/orders", {
      params: {
        type: orderTypeFilter.value,
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
    reviewPosts.value = Array.isArray(data) ? data : [];
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
  });
  try {
    await request.delete(`/api/admin/flights/${row.id}`);
    ElMessage.success("航班已删除");
    await fetchFlights();
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
  });
  try {
    await request.delete(`/api/admin/hotel-rooms/${row.id}`);
    ElMessage.success("房型已删除");
    await fetchHotelRooms(activeHotel.value?.id);
  } catch (error) {}
};

const openCouponDialog = (row = null) => {
  couponForm.value = normalizeCoupon(row || {});
  couponDialogVisible.value = true;
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
  });
  try {
    await request.delete(`/api/admin/coupons/${row.id}`);
    ElMessage.success("优惠券已删除");
    await fetchCoupons();
  } catch (error) {}
};

const saveSensitiveWord = async () => {
  try {
    await request.post("/api/admin/sensitive-words", sensitiveForm.value);
    ElMessage.success("敏感词已新增");
    sensitiveDialogVisible.value = false;
    sensitiveForm.value = createSensitiveForm();
    await fetchSensitiveWords();
  } catch (error) {}
};

const removeSensitiveWord = async (row) => {
  await ElMessageBox.confirm(`确认删除敏感词 ${row.word} 吗？`, "删除确认", {
    type: "warning",
  });
  try {
    await request.delete(`/api/admin/sensitive-words/${row.id}`);
    ElMessage.success("敏感词已删除");
    await fetchSensitiveWords();
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
    await request.post(`/api/admin/posts/${id}/reject`);
    ElMessage.success("审核已拒绝");
    await fetchPosts();
    if (activeMenu.value === "stats") {
      await fetchDashboard();
    }
  } catch (error) {}
};

const resolveReviewReport = async (id) => {
  try {
    await request.post(`/api/admin/review-reports/${id}/resolve`);
    ElMessage.success("举报工单已处理");
    await fetchReviewReports();
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
    ElMessage.success("退款申请已拒绝");
    await fetchOrders();
  } catch (error) {}
};

const canReviewRefund = (row) => row.type !== "酒店" && row.status === 3;

const orderStatusLabel = (row) => {
  if (row.type === "酒店") {
    return (
      ["待支付", "已支付", "入住中", "已完成", "已取消"][row.status] ||
      `状态${row.status}`
    );
  }
  return (
    ["待支付", "出票中", "已出票", "已取消", "已退票"][row.status] ||
    `状态${row.status}`
  );
};

const orderStatusType = (row) => {
  if (row.status === 0) {
    return "warning";
  }
  if (row.status === 1 || row.status === 2 || row.status === 4) {
    return "success";
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
  } else if (menu === "hotels") {
    fetchHotels();
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
}

.admin-aside {
  background: #0f172a;
  min-height: calc(100vh - 60px);
}

.admin-logo {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  padding: 20px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-menu {
  background: #0f172a;
  border-right: none;
}

.admin-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.6);
}

.admin-menu :deep(.el-menu-item:hover),
.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #0d9488, #10b981);
  color: #fff;
}

.admin-main {
  background: #f8fafc;
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
  border-radius: 12px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  background: linear-gradient(135deg, #0d9488, #10b981);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.panel-card,
.alert-card {
  border-radius: 16px;
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
  border-radius: 12px;
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
</style>
