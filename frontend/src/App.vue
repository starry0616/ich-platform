<template>
  <router-view />
</template>

<script setup>
// 根组件：全局样式已由 index.css / element-plus 提供
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  background-color: #f7f5f0;
  color: #333;
}
a {
  text-decoration: none;
  color: inherit;
}
.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px;
}
.el-card {
  border-radius: 10px;
}
</style>
