<template>
  <header class="header">
    <div class="header-top">
      <div class="container">
        <div class="header-top-content">
          <div class="top-left">
            <span>欢迎来到青禾商城！</span>
          </div>
          <div class="top-right">
            <template v-if="isLoggedIn">
              <el-dropdown @command="handleUserCommand">
                <span class="user-dropdown-link">
                  <el-avatar :size="24" :src="userInfo?.avatar || defaultAvatar" />
                  <span class="username">{{ userInfo?.nickname || userInfo?.username }}</span>
                  <el-icon class="arrow"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="/user/profile">个人中心</el-dropdown-item>
                    <el-dropdown-item command="/user/address">收货地址</el-dropdown-item>
                    <el-dropdown-item command="/order/list">我的订单</el-dropdown-item>
                    <el-dropdown-item command="/user/favorites">我的收藏</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <router-link to="/login" class="top-link">登录</router-link>
              <router-link to="/register" class="top-link">注册</router-link>
            </template>
            <span class="divider">|</span>
            <router-link to="/order/list" class="top-link">我的订单</router-link>
            <span class="divider">|</span>
            <router-link to="/user/profile" class="top-link">个人中心</router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="header-main">
      <div class="container">
        <div class="header-content">
          <router-link to="/" class="logo">
            <img src="@/assets/images/logo.png" alt="青禾商城" v-if="hasLogo" />
            <span class="logo-icon">🌾</span>
            <span class="logo-text">青禾商城</span>
          </router-link>

          <div class="header-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品、品牌、店铺"
              @keyup.enter="handleSearch"
              class="search-input"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" class="search-btn">搜索</el-button>
              </template>
            </el-input>
            <div class="search-tags">
              <span
                v-for="tag in hotSearchTags"
                :key="tag"
                class="search-tag"
                @click="searchKeyword = tag"
              >
                {{ tag }}
              </span>
            </div>
          </div>

          <div class="header-actions">
            <router-link to="/cart" class="cart-btn">
              <el-badge :value="cartCount" :hidden="cartCount === 0" :max="99">
                <el-icon :size="22"><ShoppingCart /></el-icon>
              </el-badge>
              <span class="cart-text">购物车</span>
              <span class="cart-count" v-if="cartCount > 0">{{ cartCount }}</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="header-nav">
      <div class="container">
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/goods/list" class="nav-item">全部商品</router-link>
          <router-link to="/goods/list?tag=hot" class="nav-item">热销</router-link>
          <router-link to="/goods/list?tag=new" class="nav-item">新品</router-link>
          <router-link to="/goods/list?tag=promotion" class="nav-item">促销</router-link>
        </nav>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ShoppingCart, ArrowDown } from '@element-plus/icons-vue'
import { useUser } from '@/composables/useUser'
import { useCartStore } from '@/stores/cart'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'

const router = useRouter()
const { isLoggedIn, userInfo, logout } = useUser()
const cartStore = useCartStore()
const { totalCount: cartCount } = storeToRefs(cartStore)

const searchKeyword = ref('')
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1png.png'
const hasLogo = false
const hotSearchTags = ['手机', '电脑', '服装', '美妆', '食品']

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/goods/list', query: { keyword: searchKeyword.value } })
  } else {
    router.push('/goods/list')
  }
}

const handleUserCommand = async (command: string) => {
  if (command === 'logout') {
    await logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else {
    router.push(command)
  }
}
</script>

<style lang="scss" scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

// 顶部信息栏
.header-top {
  background: #f5f5f5;
  font-size: 12px;

  .container {
    width: 1200px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .header-top-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 32px;
  }

  .top-left {
    color: #909399;
  }

  .top-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .top-link {
    color: #606266;
    text-decoration: none;

    &:hover {
      color: #409eff;
    }
  }

  .divider {
    color: #dcdfe6;
  }

  .user-dropdown-link {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: #606266;

    &:hover {
      color: #409eff;
    }

    .username {
      max-width: 80px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .arrow {
      font-size: 12px;
    }
  }
}

// 主内容区
.header-main {
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f5;

  .container {
    width: 1200px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .header-content {
    display: flex;
    align-items: center;
    gap: 40px;
  }
}

.logo {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 8px;

  .logo-icon {
    font-size: 28px;
  }

  .logo-text {
    font-size: 22px;
    font-weight: bold;
    color: #409eff;
  }

  img {
    height: 40px;
    margin-right: 8px;
  }
}

.header-search {
  flex: 1;
  max-width: 500px;

  .search-input {
    :deep(.el-input__wrapper) {
      border-radius: 20px 0 0 20px;
    }

    :deep(.el-input-group__append) {
      border-radius: 0 20px 20px 0;
      background: #409eff;
      border-color: #409eff;

      .search-btn {
        color: #fff;
        font-size: 15px;
      }
    }
  }

  .search-tags {
    display: flex;
    gap: 12px;
    margin-top: 8px;
    padding-left: 16px;

    .search-tag {
      font-size: 12px;
      color: #909399;
      cursor: pointer;

      &:hover {
        color: #409eff;
      }
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
}

.cart-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  text-decoration: none;
  color: #303133;
  transition: all 0.3s;
  position: relative;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }

  .cart-text {
    font-size: 14px;
  }

  .cart-count {
    position: absolute;
    top: -6px;
    right: -6px;
    min-width: 18px;
    height: 18px;
    line-height: 18px;
    text-align: center;
    background: #f56c6c;
    color: #fff;
    font-size: 11px;
    border-radius: 9px;
    padding: 0 5px;
  }
}

// 导航菜单
.header-nav {
  background: #fff;

  .container {
    width: 1200px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .nav-menu {
    display: flex;
    gap: 40px;

    .nav-item {
      padding: 14px 0;
      font-size: 15px;
      color: #303133;
      text-decoration: none;
      transition: color 0.3s;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        width: 0;
        height: 2px;
        background: #409eff;
        transition: width 0.3s;
      }

      &:hover,
      &.router-link-active {
        color: #409eff;

        &::after {
          width: 100%;
        }
      }
    }
  }
}
</style>
