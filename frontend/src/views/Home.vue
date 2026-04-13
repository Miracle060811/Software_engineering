<template>
  <div class="home">
    <h1>欢迎来到 TravelMate (伴游) !</h1>
    <p>您的综合智能出行旅游平台 - 大交通组（成员A）展示主页</p>
    <div class="nav-buttons">
      <el-button
        type="primary"
        size="large"
        @click="$router.push('/flight-search')"
        >✈️ 搜索航班</el-button
      >
      <el-button type="success" size="large" @click="$router.push('/my-orders')"
        >🎫 我的订单</el-button
      >
    </div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.nav-buttons {
  margin-top: 30px;
  display: flex;
  gap: 20px;
}
</style>
