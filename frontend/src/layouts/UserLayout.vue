<template>
  <div class="user-layout">
    <div class="user-container">
      <div class="user-sidebar">
        <div class="user-info">
          <el-avatar :size="60" :src="userInfo?.avatar || defaultAvatar" />
          <p class="username">{{ userInfo?.nickname || userInfo?.username || '用户' }}</p>
        </div>
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/user/profile">
            <span>个人中心</span>
          </el-menu-item>
          <el-menu-item index="/order/list">
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/user/address">
            <span>收货地址</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="user-main">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUser } from '@/composables/useUser'

const route = useRoute()
const { userInfo } = useUser()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1png.png'

const activeMenu = computed(() => route.path)
</script>

<style lang="scss" scoped>
.user-layout {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding: 20px 0;
}

.user-container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  gap: 20px;
}

.user-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.user-info {
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;

  .username {
    margin-top: 10px;
    font-size: 14px;
    color: #303133;
  }
}

.user-main {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: 500px;
}
</style>
