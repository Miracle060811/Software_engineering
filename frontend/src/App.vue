<template>
  <el-config-provider>
    <div id="app-container">
      <router-view />
    </div>
  </el-config-provider>
</template>

<script setup></script>

<style>
#app-container {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  margin: 0;
  padding: 0;
  height: 100vh;
}
</style>
